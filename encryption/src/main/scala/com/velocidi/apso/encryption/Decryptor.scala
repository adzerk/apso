package com.velocidi.apso.encryption

import java.io.InputStream
import java.security.{Key, MessageDigest}
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base64

import com.velocidi.apso.Logging

/** Utility class to handle decrypting data to string format and, optionally, handle base64 encoded data.
  *
  * @param decryptor the underlying Cipher object that allows to decrypt the data.
  */
class Decryptor(decryptor: Cipher) extends EncryptionErrorHandling {
  def apply(s: String): Option[String] = decrypt(s)

  def decrypt(s: String, base64: Boolean = true): Option[String] = handle {
    if (base64) EncryptionUtils.paddedUrlSafebase64(decryptor.doFinal(Base64.decodeBase64(s)))
    else new String(decryptor.doFinal(Base64.decodeBase64(s)))
  }

  def decryptToString(s: String): Option[String] = handle {
    new String(decryptor.doFinal(Base64.decodeBase64(s)))
  }
}

/** Provides the `apply` methods that allow to more easily create a [[Decryptor]] object by directly specifying the
  * transformation and key, or a keystore holding the key parameters.
  */
object Decryptor extends EncryptionUtils with Logging {

  private def loadDecryptionCipher(transformation: String, key: Key): Option[Cipher] = handle(
    {
      log.debug(s"Building Decryptor using Transformation '$transformation' and Key Algorithm '${key.getAlgorithm}'")
      val cipher = Cipher.getInstance(transformation, provider)
      cipher.init(Cipher.DECRYPT_MODE, key)
      cipher
    },
    { ex: Throwable =>
      log.warn(s"Cipher Transformation: $transformation")
      log.warn("Cipher Key: " + key)
      log.warn(s"Impossible to create Decryption Cipher!", ex)
    }
  )

  def apply(transformation: String, key: Array[Byte]): Option[Decryptor] =
    loadDecryptionCipher(transformation, new SecretKeySpec(key, transformation)).map(new Decryptor(_))

  def apply(transformation: String, key: String): Option[Decryptor] = {
    val md = MessageDigest.getInstance("SHA-256")

    loadDecryptionCipher(transformation, new SecretKeySpec(md.digest(key.getBytes("UTF-8")), transformation))
      .map(new Decryptor(_))
  }

  def apply(
      transformation: String,
      key: InputStream,
      keyStorePassword: String,
      keyAlias: String,
      keyPassword: String
  ): Option[Decryptor] =
    for {
      keyStore <- loadKeyStore(key, keyStorePassword)
      key <- getKey(keyStore, keyAlias, keyPassword)
      cipher <- loadDecryptionCipher(transformation, key)
    } yield new Decryptor(cipher)
}
