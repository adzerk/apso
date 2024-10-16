package com.kevel.apso.encryption

import java.io.InputStream
import java.security.{Key, MessageDigest}
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base64

/** Utility class to handle encrypting data to string format and, optionally, handle base64 encoded data.
  *
  * @param encryptor
  *   the underlying Cipher object that allows to encrypt the data.
  */
class Encryptor(encryptor: Cipher) extends EncryptionErrorHandling with LazyLogging {
  def apply(data: String) = encrypt(data)

  def encrypt(data: String, pad: Boolean = true): Option[String] = handle(
    EncryptionUtils.paddedUrlSafebase64(encryptor.doFinal(data.getBytes("UTF-8")), pad),
    { ex: Throwable => logger.warn(s"Error while trying to encrypt data with padding '$pad': $data", ex) }
  )

  def encryptToSafeString(data: Array[Byte]): Option[String] = handle(
    Base64.encodeBase64URLSafeString(encryptor.doFinal(data)),
    { ex: Throwable => logger.warn(s"Error while trying to encrypt data: $data", ex) }
  )

  def encryptToSafeString(data: String): Option[String] = handle(
    Base64.encodeBase64URLSafeString(encryptor.doFinal(data.getBytes("UTF-8"))),
    { ex: Throwable => logger.warn(s"Error while trying to encrypt data: $data", ex) }
  )
}

/** Provides the `apply` methods that allow to more easily create a [[Encryptor]] object by directly specifying the
  * transformation and key, or a keystore holding the key parameters.
  */
object Encryptor extends EncryptionUtils with LazyLogging {

  private def loadEncryptionCipher(transformation: String, key: Key): Option[Cipher] = handle(
    {
      logger.debug(s"Building Encryptor using Transformation '$transformation' and Key Algorithm '${key.getAlgorithm}'")
      val cipher = Cipher.getInstance(transformation, provider)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      cipher
    },
    { ex: Throwable =>
      logger.warn(s"Cipher Transformation: $transformation")
      logger.warn("Cipher Key: " + key)
      logger.warn(s"Impossible to create Encryption Cipher!", ex)
    }
  )

  def apply(transformation: String, secretKeySpec: SecretKeySpec): Option[Encryptor] =
    loadEncryptionCipher(transformation, secretKeySpec).map(new Encryptor(_))

  def apply(transformation: String, key: Array[Byte]): Option[Encryptor] =
    apply(transformation, new SecretKeySpec(key, transformation))

  def apply(transformation: String, key: String): Option[Encryptor] = {
    val md = MessageDigest.getInstance("SHA-256")

    apply(transformation, new SecretKeySpec(md.digest(key.getBytes("UTF-8")), transformation))
  }

  def apply(
      transformation: String,
      key: InputStream,
      keyStorePassword: String,
      keyAlias: String,
      keyPassword: String
  ): Option[Encryptor] =
    for {
      keyStore <- loadKeyStore(key, keyStorePassword)
      key <- getKey(keyStore, keyAlias, keyPassword)
      cipher <- loadEncryptionCipher(transformation, key)
    } yield new Encryptor(cipher)
}
