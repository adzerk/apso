package com.velocidi.apso.currencies

import squants.market._

trait SquantsImplicitConversions {
  @inline private[this] def findCurrency(ctx: MoneyContext, curr: String): Option[Currency] = {
    ctx.currencies.find(_.code == curr)
  }

  implicit class ExtraCurrencyRateOps(val exch: CurrencyRate) {
    def squants(implicit ctx: MoneyContext): Option[CurrencyExchangeRate] = {
      for {
        from <- findCurrency(ctx, exch.from)
        to <- findCurrency(ctx, exch.to)
      } yield from / to(exch.rate)
    }
  }

  implicit class ExtraStringOps(val str: String) {
    def asCurrency(implicit ctx: MoneyContext): Option[Currency] = {
      findCurrency(ctx, str)
    }
  }
}

object SquantsImplicitConversions extends SquantsImplicitConversions
