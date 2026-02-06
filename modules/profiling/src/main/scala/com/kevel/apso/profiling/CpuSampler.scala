package com.kevel.apso.profiling

import java.lang.management.ManagementFactory

import scala.annotation.nowarn
import scala.collection.mutable

import com.typesafe.scalalogging.Logger

import com.kevel.apso.profiling.CpuSampler.*

/** A lightweight CPU profiler based on call stack sampling.
  *
  * When run as a thread, it periodically captures the call stacks of all live threads and maintains counters for each
  * leaf method. The counters are then dumped to a logger with a given periodicity (most probably greater than the
  * sampling period). Each data row written to the logger contains a timestamp, the method profiled, its location in the
  * source code and the associated absolute counters and relative weight.
  *
  * @param samplePeriod
  *   the period between each sample taken
  * @param flushPeriod
  *   the period between each flush to log
  * @param logger
  *   the logger to which results are written
  */
class CpuSampler(samplePeriod: Long = 100, flushPeriod: Long = 10000, logger: Logger = Logger(getClass))
    extends Runnable {

  private[this] val threadBean = ManagementFactory.getThreadMXBean

  private[this] var active = true
  private[this] var lastFlush = 0L
  private[this] val entries = mutable.Queue[StackTraceElement]()

  /** Returns a boolean value indicating if the stack trace element should be considered when profiling or not.
    * @param elem
    *   the stack trace element
    * @return
    *   `true` if the stack trace element should be considered in profiling, `false` otherwise.
    */
  def shouldProfile(elem: StackTraceElement) = {
    !excludeClassRegex.matcher(elem.getClassName).matches &&
    !excludedMethods.get(elem.getClassName).exists(_ == elem.getMethodName)
  }

  /** Captures the current call stacks of all live threads and stores relevant profiling data about them.
    */
  @nowarn("cat=deprecation")
  def sample() = for {
    // TODO Thread#getId is deprecated as of Java 19 and should be replaced with Thread#threadId.
    // Unfortunately, this is only available since Java 19.
    info <- threadBean.dumpAllThreads(false, false) if info.getThreadId != Thread.currentThread.getId
    elem <- info.getStackTrace.headOption if shouldProfile(elem)
  } entries.enqueue(elem)

  /** Flushes the stored profiling data to the logger.
    * @param timestamp
    *   the timestamp to use when writing the entries to the logger
    */
  def flush(timestamp: Long = System.currentTimeMillis()) = {
    aggregateAll(timestamp).foreach(t => logger.debug(s"$t"))
    lastFlush = System.currentTimeMillis()
  }

  private[this] def aggregateAll(timestamp: Long): Iterator[Entry] = {
    val acc = mutable.Map[StackTraceElement, Int]()

    val total = entries.length
    while (entries.nonEmpty) {
      val entry = entries.dequeue()
      acc.update(entry, acc.getOrElseUpdate(entry, 0) + 1)
    }

    acc.iterator.map { p =>
      Entry(timestamp, p._1, p._2, if (total == 0) 0.0 else p._2.toDouble / total)
    }
  }

  private[this] def sampleLoop() = {
    while (active) {
      val time = System.currentTimeMillis()

      sample()
      if (time > lastFlush + flushPeriod) {
        flush(time)
      }

      Thread.sleep(math.max(0, samplePeriod - (System.currentTimeMillis() - time)))
    }
  }

  def run() = {
    logger.debug(headerEntry)
    lastFlush = System.currentTimeMillis()
    sampleLoop()
  }

  /** Stops the data collecting and flushes the remaining data to the logger, causing the thread to stop eventually.
    */
  def stop() = {
    active = false
    flush()
  }
}

/** Object containing constants and helper methods and classes for `CpuSampler`.
  */
object CpuSampler {

  /** An entry to be logged.
    * @param timestamp
    *   the timestamp of the data
    * @param elem
    *   the stack trace element
    * @param count
    *   the number of times the element was seen since the last flush
    * @param perc
    *   the percentage of times the element was seen, in the range 0.0 to 100.0
    */
  case class Entry(timestamp: Long, elem: StackTraceElement, count: Int, perc: Double) {
    override def toString =
      "%d,%s,%s,%s,%d,%d,%.2f".format(
        timestamp,
        elem.getClassName,
        elem.getMethodName,
        elem.getFileName,
        elem.getLineNumber,
        count,
        perc * 100
      )
  }

  /** A list of package names that should not be profiled. All child packages of the packages in this list are also
    * excluded.
    */
  val excludedPackages =
    Seq("sun", "sunw", "com.sun", "com.apple", "apple.awt", "apple.laf", "org.jboss.netty", "scala.concurrent.forkjoin")

  /** The compiled regex for the `excludedPackages` list.
    */
  val excludeClassRegex =
    excludedPackages.map(_.replace(".", "\\.") + """\..*""").reduce(_ + "|" + _).r.pattern

  /** A map containing the methods in each class that should not be profiled.
    */
  val excludedMethods = Map(
    "java.net.PlainSocketImpl" -> "socketAccept",
    "sun.awt.windows.WToolkit" -> "eventLoop",
    "java.lang.UNIXProcess" -> "waitForProcessExit",
    "sun.awt.X11.XToolkit" -> "waitForEvents",
    "apple.awt.CToolkit" -> "doAWTRunLoop",
    "java.lang.Object" -> "wait",
    "java.lang.Thread" -> "sleep",
    "sun.net.dns.ResolverConfigurationImpl" -> "notifyAddrChange0",
    "java.net.SocketInputStream" -> "socketRead0"
  )

  /** The entry written to a `CpuSampler` logger at the beginning of the data collection.
    */
  val headerEntry = "Timestamp,Class,Method,File,Line,Count"
}
