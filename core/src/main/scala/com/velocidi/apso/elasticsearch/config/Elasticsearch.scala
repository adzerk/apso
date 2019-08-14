package com.velocidi.apso.elasticsearch.config

import scala.concurrent.duration._

import com.velocidi.apso.elasticsearch.config.Elasticsearch.BulkInserter

case class Elasticsearch(
    host: String,
    port: Int,
    useHttps: Boolean,
    username: Option[String] = None,
    password: Option[String] = None,
    bulkInserter: Option[BulkInserter] = None)

object Elasticsearch {
  case class BulkInserter(
      flushFrequency: FiniteDuration,
      esDownCheckFrequency: FiniteDuration,
      maxBufferSize: Int,
      maxTryCount: Int)
}
