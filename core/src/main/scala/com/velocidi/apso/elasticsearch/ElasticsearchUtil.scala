package com.velocidi.apso.elasticsearch

import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ ElasticClient, ElasticProperties }

trait ElasticsearchUtil {
  def esConfig: config.Elasticsearch

  lazy val getEsClient: ElasticClient = {
    val esHost = esConfig.host
    val esPort = esConfig.port
    getEsClient(esHost, esPort)
  }

  def getEsClient(esHost: String, esPort: Int): ElasticClient = {
    val protocol = if (esConfig.useHttps) "https" else "http"
    ElasticClient(JavaClient(ElasticProperties(s"$protocol://$esHost:$esPort")))
  }
}
