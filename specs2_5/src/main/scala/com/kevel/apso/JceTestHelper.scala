package com.kevel.apso

import javax.crypto.Cipher

import org.specs2.execute.{AsResult, Result, Skipped}

object JceTestHelper {

  /** Ensures the Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files are installed.
    */
  def unlimitedJCE[T](r: => T)(using evidence: AsResult[T]): Result = {
    if (Cipher.getMaxAllowedKeyLength("sha256") == Int.MaxValue) evidence.asResult(r)
    else Skipped("Java Cryptography Extension Unlimited Strength Jurisdiction Policy Files not installed.")
  }
}
