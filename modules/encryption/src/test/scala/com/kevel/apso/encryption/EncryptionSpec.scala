package com.kevel.apso.encryption

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec

import org.specs2.mutable.Specification

class EncryptionSpec extends Specification {
  val data = "apso secret data"
  val password = "secret-password"

  "An Encryptor and a Decryptor" should {
    "round-trip data using a password-derived key" in {
      foreach(Seq("AES", "AES/ECB/PKCS5Padding")) { transformation =>
        val encryptor = Encryptor(transformation, password)
        val decryptor = Decryptor(transformation, password)

        encryptor must beSome
        decryptor must beSome

        val encrypted = encryptor.get.encrypt(data)
        encrypted must beSome
        decryptor.get.decryptToString(encrypted.get) must beSome(data)
      }
    }

    "round-trip data using raw key bytes" in {
      val key = EncryptionUtils.keyBytesFromPassword(password)
      val encrypted = Encryptor("AES", key).flatMap(_.encryptToSafeString(data))

      encrypted must beSome
      Decryptor("AES", key).flatMap(_.decryptToString(encrypted.get)) must beSome(data)
    }

    "round-trip data using a key loaded from a JCEKS keystore" in {
      val keyStorePassword = "keystorePass"
      val keyAlias = "keyAlias"
      val keyPassword = "keyPass"

      val ks = KeyStore.getInstance("JCEKS")
      ks.load(null, keyStorePassword.toCharArray)
      ks.setEntry(
        keyAlias,
        new KeyStore.SecretKeyEntry(new SecretKeySpec(EncryptionUtils.keyBytesFromPassword(password), "AES")),
        new KeyStore.PasswordProtection(keyPassword.toCharArray)
      )
      val out = new ByteArrayOutputStream()
      ks.store(out, keyStorePassword.toCharArray)
      def keyStoreStream = new ByteArrayInputStream(out.toByteArray)

      val encryptor = Encryptor("AES", keyStoreStream, keyStorePassword, keyAlias, keyPassword)
      val decryptor = Decryptor("AES", keyStoreStream, keyStorePassword, keyAlias, keyPassword)

      encryptor must beSome
      decryptor must beSome
      decryptor.get.decryptToString(encryptor.get.encrypt(data).get) must beSome(data)
    }

    "produce output compatible with standard AES-256-ECB implementations" in {
      // Generated with `openssl enc -aes-256-ecb -K <sha256 of password> -nosalt`, url-safe base64 encoded
      val expected = "MHaEpfdDxnpqrtqdRHoXWOFYNb4Oopx_V4A6GZDMMv8="

      Encryptor("AES", password).flatMap(_.encrypt(data)) must beSome(expected)
      Decryptor("AES", password).flatMap(_.decryptToString(expected)) must beSome(data)
    }

    "fail gracefully on unsupported transformations" in {
      Encryptor("NotAnAlgorithm", password) must beNone
      Decryptor("NotAnAlgorithm", password) must beNone
    }
  }
}
