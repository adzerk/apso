package com.kevel.apso.gcp

import java.io.{ByteArrayInputStream, File, InputStream}
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.collection.mutable.ListBuffer
import scala.util.Try

import com.google.cloud.storage.{Storage, StorageException}
import org.specs2.mutable.Specification

class GCSBucketSpec extends Specification {

  /** A `Storage` whose uploads always time out, recording the stream given to each attempt and what was read from it.
    */
  private class TimingOutStorage {
    val attempts = ListBuffer.empty[(InputStream, String)]

    private val storage = Proxy
      .newProxyInstance(
        classOf[Storage].getClassLoader,
        Array(classOf[Storage]),
        (_, method, args) =>
          method.getName match {
            case "createFrom" =>
              val in = args(1).asInstanceOf[InputStream]
              attempts += in -> new String(in.readAllBytes(), StandardCharsets.UTF_8)
              throw new StorageException(new SocketTimeoutException("Read timed out"))
            case "toString" => "TimingOutStorage"
            case name       => throw new UnsupportedOperationException(name)
          }
      )
      .asInstanceOf[Storage]

    val bucket = new GCSBucket("bucket", () => storage)
  }

  private def tmpFile(content: String): File = {
    val path = Files.createTempFile("gcs-bucket-spec", ".txt")
    path.toFile.deleteOnExit()
    Files.writeString(path, content).toFile
  }

  "A GCSBucket" should {

    "retry pushing a file whose upload failed" in {

      "reading the file from its start on every attempt" in {
        val storage = new TimingOutStorage
        storage.bucket.push("key", tmpFile("content")) must beFalse
        // The first attempt plus the 2 retries set in the test configuration.
        storage.attempts.map(_._2) ==== List.fill(3)("content")
      }

      "closing the file after every attempt" in {
        val storage = new TimingOutStorage
        storage.bucket.push("key", tmpFile("content")) must beFalse
        storage.attempts.map { case (in, _) => Try(in.read()).isFailure } ==== List.fill(3)(true)
      }
    }

    "not retry pushing a stream whose upload failed" in {
      val storage = new TimingOutStorage
      val in = new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8))
      storage.bucket.push("key", in, None) must beFalse
      storage.attempts.map(_._2) ==== List("content")
    }
  }
}
