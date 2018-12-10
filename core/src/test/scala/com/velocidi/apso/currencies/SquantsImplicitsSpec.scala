package com.velocidi.apso.currencies

import net.ruippeixotog.akka.testkit.specs2.mutable.AkkaSpecification
import org.specs2.specification.Scope
import squants.market._

// TODO: this won't be necessary when https://github.com/typelevel/squants/pull/330 is accepted and released
class SquantsImplicitsSpec extends AkkaSpecification {
  val defaultCtx = MoneyContext(EUR, defaultCurrencySet, Nil)

  trait MockData extends Scope with SquantsImplicitConversions

  trait MockDataWithContext extends Scope with SquantsImplicitConversions {
    implicit val ctx: MoneyContext = defaultCtx
  }

  object DUM extends Currency("DUM", "Dummy", "§", 1)

  "SquantsImplicits" should {
    "provide extension methods to convert string to Currency" in {
      "working on default context" in new MockDataWithContext {
        "EUR".asCurrency must beSome(squants.market.EUR)
        "DUM".asCurrency must beNone
      }

      "working on specific contexts" in new MockData {
        implicit val ctx: MoneyContext = defaultCtx.withAdditionalCurrencies(Set(DUM))

        "EUR".asCurrency must beSome(squants.market.EUR)
        "DUM".asCurrency must beSome(DUM)
      }
    }

    "provide extension methods to convert CurrencyRate to CurrencyExchangeRate" in {
      "working on default context" in new MockDataWithContext {
        CurrencyRate(from = "USD", to = "CAD", rate = BigDecimal("1.05")).squants must beSome(USD / CAD(1.05))
        CurrencyRate(from = "USD", to = "DUM", rate = BigDecimal("1.05")).squants must beNone
      }

      "working on specific contexts" in new MockData {
        implicit val ctx: MoneyContext = defaultCtx.withAdditionalCurrencies(Set(DUM))

        CurrencyRate(from = "USD", to = "CAD", rate = BigDecimal("1.05")).squants must beSome(USD / CAD(1.05))
        CurrencyRate(from = "USD", to = "DUM", rate = BigDecimal("1.05")).squants must beSome(USD / DUM(1.05))
      }
    }
  }
}
