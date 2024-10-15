package com.kevel.apso.encryption

import java.io.InputStream
import java.security._

import org.apache.commons.codec.binary.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider

object BouncyCastleInitializer {
  private val provider = new BouncyCastleProvider()
  Security.addProvider(provider)

  def apply() = provider
}

/** Loads and provide a `BouncyCastleProvider` and provides utility methods to encode in base64, load keystores and
  * create keys from raw password input.
  */
trait EncryptionUtils extends EncryptionErrorHandling {
  val provider = BouncyCastleInitializer()

  def pad(b64: String): String =
    if (b64.trim.isEmpty) b64
    else {
      val padding = b64.length % 4 match {
        case 2 => 2
        case 3 => 1
        case _ => 0
      }

      b64.padTo(b64.length + padding, '=')
    }

  def paddedUrlSafebase64(bytes: Array[Byte], pad: Boolean = true): String = {
    val b64 = Base64.encodeBase64URLSafeString(bytes) // the url-safe alphabet does not pad data
    if (pad) EncryptionUtils.pad(b64) else b64
  }

  def loadKeyStore(key: InputStream, password: String): Option[KeyStore] = handle {
    val ks = KeyStore.getInstance("JCEKS")
    ks.load(key, password.toCharArray)
    ks
  }

  def getKey(keystore: KeyStore, alias: String, password: String): Option[Key] = handle {
    keystore.getKey(alias, password.toCharArray)
  }

  def keyBytesFromPassword(password: String): Array[Byte] = {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(password.getBytes("UTF-8"))
    digest.take(32)
  }
}

object EncryptionUtils extends EncryptionUtils

trait EncryptionErrorHandling {

  protected def handler[T]: PartialFunction[Throwable, Option[T]] = { case _: Exception =>
    None
  }

  protected def handler[T](onException: Throwable => Unit): PartialFunction[Throwable, Option[T]] = {
    case ex: Exception =>
      onException(ex)
      None
  }

  protected def handle[T](f: => T): Option[T] = try { Some(f) }
  catch handler[T]
  protected def handle[T](f: => T, onException: Throwable => Unit): Option[T] = try { Some(f) }
  catch handler[T](onException)
  protected def handle[T](f: => T, g: => Unit): Option[T] = try { Some(f) }
  catch handler[T]
  finally { g }
}
