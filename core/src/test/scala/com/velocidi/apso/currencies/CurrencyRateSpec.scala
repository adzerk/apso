package com.velocidi.apso.currencies

import java.io.{ File, PrintWriter }

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import com.velocidi.apso.io.LocalFileDescriptor

class CurrencyRateSpec extends Specification {

  trait MockData extends Scope {
    def payload: String

    lazy val tempFi = File.createTempFile("currency-list", ".json")
    tempFi.deleteOnExit()
    lazy val writer = new PrintWriter(tempFi)
    writer.write(payload)
    writer.close()

    lazy val fileDescriptor = LocalFileDescriptor(tempFi.getAbsolutePath)
  }

  "CurrencyRate" should {
    "provide a method to correctly deserialize a list of CurrencyRate from a FileDescriptor" in {
      "succeed with a non-empty payload" in new MockData {
        lazy val payload =
          """
            |[
            |{"from":"ALL","to":"EUR","rate":"0.008063"},
            |{"from":"XCD","to":"EUR","rate":"0.324383"}
            |]
          """.stripMargin
        val expected = Set(
          CurrencyRate(from = "ALL", to = "EUR", rate = BigDecimal("0.008063")),
          CurrencyRate(from = "XCD", to = "EUR", rate = BigDecimal("0.324383")))

        CurrencyRate.fromFileDescriptor(fileDescriptor) must beSuccessfulTry[Set[CurrencyRate]](expected)
      }

      "succeed with empty payload" in new MockData {
        lazy val payload = "[]"
        val expected = Set.empty[CurrencyRate]
        CurrencyRate.fromFileDescriptor(fileDescriptor) must beSuccessfulTry[Set[CurrencyRate]](expected)
      }
    }
  }

}
