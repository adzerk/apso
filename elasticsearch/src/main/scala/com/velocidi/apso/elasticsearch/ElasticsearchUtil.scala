package com.kevel.apso.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.http.JavaClient
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient

trait ElasticsearchUtil {
  import ElasticsearchUtil._

  def esConfig: config.Elasticsearch

  lazy val getEsClient: ElasticClient = buildEsClient(esConfig)
}

object ElasticsearchUtil {
  def buildEsClient(esConfig: config.Elasticsearch): ElasticClient = {
    val protocol = if (esConfig.useHttps) "https" else "http"

    val credsProvider = (esConfig.username, esConfig.password) match {
      case (Some(user), Some(pass)) =>
        val cred = new BasicCredentialsProvider()
        cred.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass))
        cred

      case _ => null
    }

    val restClient =
      RestClient
        .builder(new HttpHost(esConfig.host, esConfig.port, protocol))
        .setHttpClientConfigCallback(_.setDefaultCredentialsProvider(credsProvider))
        .build()

    ElasticClient(JavaClient.fromRestClient(restClient))
  }
}
