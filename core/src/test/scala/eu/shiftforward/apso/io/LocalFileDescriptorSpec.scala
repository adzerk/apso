package eu.shiftforward.apso.io

import java.io.{ File, InputStream }
import java.nio.file.Files
import java.util.UUID

import org.specs2.mutable.Specification
import scala.io.Source
import scala.util.Try

class LocalFileDescriptorSpec extends Specification {

  "A LocalFileDescriptor" should {

    val randomFolder = UUID.randomUUID().toString

    def randomString = UUID.randomUUID().toString.take(10)

    "Have a correct absolute path after right after initialization" in {
      val file = new File("/tmp/one/two/three")
      val fd = LocalFileDescriptor("/tmp/one/two/three")
      file.getAbsolutePath == fd.path
    }

    "Retrieve the size of a file" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      fd1.write("hello world")
      fd1.size === "hello world".getBytes.length
    }

    "Move up the hierarchy correctly" in {
      LocalFileDescriptor("/tmp/one/two/three").parent() == LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp/one/two/three").parent(3) == LocalFileDescriptor("/tmp")
    }

    "Move down the hierarchy correctly" in {
      LocalFileDescriptor("/tmp") / "one" / "two" === LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp").child("one") === LocalFileDescriptor("/tmp/one")
      LocalFileDescriptor("/tmp").children("one", "two") === LocalFileDescriptor("/tmp/one/two")
      LocalFileDescriptor("/tmp") / "wrong" !== LocalFileDescriptor("/tmp/right")
    }

    "Move horizontaly in the hierarchy correctly" in {
      LocalFileDescriptor("/tmp/one/two").sibling("foo") === LocalFileDescriptor("/tmp/one/foo")
      LocalFileDescriptor("/tmp/one/two").sibling(_ + ".tmp") === LocalFileDescriptor("/tmp/one/two.tmp")
    }

    "Write and read to the file associated with the file descriptor" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      fd.exists must beFalse
      fd.write("test")
      fd.exists must beTrue
      fd.readString must beEqualTo("test")
      fd.delete()
    }

    "Delete a existing file associated with the file descriptor" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      fd.exists must beFalse
      fd.write("test")
      fd.exists must beTrue
      fd.delete()
      fd.exists must beFalse
    }

    "Delete a directory associated with the file descriptor" in {
      val symlinkFd = LocalFileDescriptor("/tmp") / randomString / randomString
      symlinkFd.write("test")

      val dirFd = LocalFileDescriptor("/tmp") / randomString

      (dirFd / randomString).write("test")
      (dirFd / randomString / randomString).write("test")

      // we create a symbolic link to an "external" folder which should not be followed while
      // deleting the directory
      Files.createSymbolicLink((dirFd / "symlink").file.toPath, symlinkFd.parent().file.toPath)

      dirFd.deleteDir() must beTrue
      dirFd.exists must beFalse

      symlinkFd.exists must beTrue
      symlinkFd.delete()
    }

    "Have a correctly working 'cd' interface" in {
      LocalFileDescriptor("/tmp/one/two/three").cd("../four/./five") ===
        LocalFileDescriptor("/tmp/one/two/four/five")
    }

    "Create intermediary folders when required" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val f = fd.children("one", "two")
      f.exists must beFalse
      f.isDirectory must beFalse
      f.mkdirs()
      f.isDirectory must beTrue
      f.exists must beTrue
    }

    "Download a file correctly to a file" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / randomString

      val file1 = fd1 / "one"
      file1.write("hello world")

      val file2 = fd2 / "two"
      file2.exists must beFalse

      file1.download(file2)
      file2.readString must beEqualTo("hello world")
    }

    "Do not download file to a directory" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / randomString

      val file1 = fd1 / "one"
      file1.write("hello world")

      val dir2 = fd2 / "two"
      dir2.mkdirs()

      Try(file1.download(dir2)) must beAFailedTry
    }

    "Download a file correctly to a file in a safe manner" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / randomString

      val file1 = fd1 / "one"
      file1.write("hello world")

      val file2 = fd2 / "two"
      file2.exists must beFalse

      file1.download(file2, safeDownloading = true)
      file1.sibling(_ + ".tmp").exists must beFalse
      file2.readString must beEqualTo("hello world")
    }

    "Upload file correctly to a file" in {
      "From another file" in {
        val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
        val fd2 = LocalFileDescriptor("/tmp") / randomFolder / randomString

        val file1 = fd1 / "one"
        file1.write("hello world")

        val file2 = fd2 / "two"
        file2.exists must beFalse

        file2.upload(file1)
        file2.readString must beEqualTo("hello world")
      }

      "From an InputStream" in {
        val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString

        val inputStream = new InputStream {
          val buffer = "hello world".iterator.map(_.toInt)
          override def read(): Int = if (buffer.isEmpty) -1 else buffer.next()
        }

        val file = fd / "two"
        file.exists must beFalse

        file.upload(inputStream, None)
        file.readString must beEqualTo("hello world")
      }
    }

    "Do not upload file to a directory" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val fd2 = LocalFileDescriptor("/tmp") / randomFolder / randomString

      val file1 = fd1 / "one"
      file1.write("hello world")

      val dir2 = fd2 / "two"
      dir2.mkdirs()

      Try(dir2.upload(file1)) must beAFailedTry
    }

    "Stream a file as bytes or lines" in {
      val fd1 = LocalFileDescriptor("/tmp") / randomFolder / randomString
      fd1.write("1\n2\n3\n4")

      Source.fromInputStream(fd1.stream()).mkString === "1\n2\n3\n4"
      Source.fromInputStream(fd1.stream(2)).mkString === "2\n3\n4"

      fd1.lines().toList === List("1", "2", "3", "4")
    }

    "List files correctly" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val files = (1 to 5).map(n => fd / n.toString)

      files.foreach(_.write("test"))
      fd.list.toSet === files.toSet
    }

    "List files by prefix correctly" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      val files = (1 to 5).map(n => fd / ("a" * n))

      files.foreach(f => f.write(f.name))
      fd.list.filter(_.name.startsWith("aaa")).toSet ===
        files.filter(_.name.length > 2).toSet
    }

    "List all files by prefix" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString

      val dir1 = fd / "aaa"
      val dir2 = fd / "bbb"
      val dir3 = fd / "ccc"

      dir1.mkdirs()
      dir2.mkdirs()
      dir3.mkdirs()

      (1 to 5).map(n => dir1 / ("g" * n)).foreach(f => f.write(f.name))
      (1 to 3).map(n => dir2 / ("j" * n)).foreach(f => f.write(f.name))

      fd.listAllFilesWithPrefix("aaa/ggg").toList must haveSize(3)
      fd.listAllFilesWithPrefix("") must haveSize(8)
      dir3.listAllFilesWithPrefix("") must beEmpty
    }

    "Know whether the FD points to a directory" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString
      fd.mkdirs()
      fd.isDirectory must beTrue
    }

    "Know whether the FD points to a file that exists" in {
      val fd = LocalFileDescriptor("/tmp") / randomFolder / randomString / "one"
      fd.write("one")
      fd.exists must beTrue
      fd.isDirectory must beFalse
    }
  }
}
