package eu.shiftforward.apso.io

import java.io.File
import java.util.UUID
import org.specs2.mutable.Specification

import scala.util.Try

class LocalFileDescriptorSpec extends Specification {

  "A LocalFileDescriptor" should {

    val randomFolder = UUID.randomUUID().toString
    val testingFolder = new File(s"/tmp/$randomFolder")

    "Have a correct absolute path after right after initialization" in {
      val file = new File("/tmp/one/two/three")
      val fd = LocalFileDescriptor("/tmp/one/two/three")
      file.getAbsolutePath == fd.path
    }

    "Move up the hierarchy correctly" in {
      LocalFileDescriptor("/tmp/one/two/three").parent() == LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp/one/two/three").parent(3) == LocalFileDescriptor("/tmp")
    }

    "Move down the hiearchy correctly" in {
      LocalFileDescriptor("/tmp") / "one" / "two" === LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp").addChild("one") === LocalFileDescriptor("/tmp/one")
      LocalFileDescriptor("/tmp").addChildren("one", "two") === LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp") / "wrong" !== LocalFileDescriptor("/tmp/right")
    }

    "Move horizontaly in the hierarchy correctly" in {
      LocalFileDescriptor("/tmp/one/two").sibling("foo") === LocalFileDescriptor("/tmp/one/foo")
      LocalFileDescriptor("/tmp/one/two").sibling(_ + ".tmp") === LocalFileDescriptor("/tmp/one/two.tmp")
    }

    "Write and read to the file associated to the file descriptor" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testfile1"
      fd.exists must beFalse
      fd.write("test")
      fd.exists must beTrue
      fd.readString must beEqualTo("test")
      fd.delete()
    }

    "Delete a existing file associated to the file descriptor" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testfile2"
      fd.exists must beFalse
      fd.write("test")
      fd.exists must beTrue
      fd.delete()
      fd.exists must beFalse
    }

    "Have a correctly working 'cd' interface" in {
      LocalFileDescriptor("/tmp/one/two/three").cd("../four/./five") ===
        LocalFileDescriptor("/tmp/one/two/four/five")
    }

    "Create intermediary folders when required" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testdir1"
      val f = fd.addChildren("one", "two")
      f.exists must beFalse
      f.isDirectory must beFalse
      f.mkdirs()
      f.isDirectory must beTrue
      f.exists must beTrue
    }

    "Download a file correctly to a file" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / "testdir2"
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / "testdir2b"

      val file1 = fd1 / "one"
      file1.write("hello world")

      val file2 = fd2 / "two"
      file2.exists must beFalse

      file1.download(file2)
      file2.readString must beEqualTo("hello world")
    }

    "Do not download file to a directory" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / "testdir02"
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / "testdir02b"

      val file1 = fd1 / "one"
      file1.write("hello world")

      val dir2 = fd2 / "two"
      dir2.mkdirs()

      Try(file1.download(dir2)) must beAFailedTry
    }

    "Upload file correctly to a file" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / "testdir3"
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / "testdir3b"

      val file1 = fd1 / "one"
      file1.write("hello world")

      val file2 = fd2 / "two"
      file2.exists must beFalse

      file2.upload(file1)
      file2.readString must beEqualTo("hello world")
    }

    "Do not upload file to a directory" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / "testdir03"
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / "testdir03b"

      val file1 = fd1 / "one"
      file1.write("hello world")

      val dir2 = fd2 / "two"
      dir2.mkdirs()

      Try(dir2.upload(file1)) must beAFailedTry
    }

    "List files correctly" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testdir4"
      val files = (1 to 5).map(n => fd / n.toString)

      files.foreach(_.write("test"))
      fd.list().toSet === files.toSet
    }

    "List files by prefix correctly" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testdir5"
      val files = (1 to 5).map(n => fd / ("a" * n))

      files.foreach(f => f.write(f.name))
      fd.listByPrefix("aaa").toSet === files.filter(_.name.length > 2).toSet
    }

    "Know whether the FD points to a directory" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testdir6"
      fd.mkdirs()
      fd.isDirectory must beTrue
    }

    "Know whether the FD points to a file that exists" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / "testdir7" / "one"
      fd.write("one")
      fd.exists must beTrue
      fd.isDirectory must beFalse
    }

  }
}
