package com.github.andyglow.websocket.util

import java.net.URI

case class Uri(
  secure: Boolean,
  host: String,
  port: Int,
  path: Option[String] = None,
  query: Map[String, String] = Map.empty) {

  val scheme: String = if (secure) "wss" else "ws"

  def toURI: URI = {
    def queryStr = if (query.isEmpty) null else {
      query map { case (k, v) => s"$k=$v" } mkString "&"
    }

    new URI(scheme, null, host, port, path.orNull, queryStr, null)
  }
}

object Uri {
  val defaultPorts: Map[String, Int] = Map("ws" -> 80, "wss" -> 443)

  def apply(uri: String): Uri = apply(new URI(uri))

  def apply(uri: URI): Uri = {
    require(uri.getScheme == "ws" || uri.getScheme == "wss")
    val secure = uri.getScheme == "wss"
    val host = uri.getHost
    val port = uri.getPort
    val path = uri.getPath
    val queryList = if (uri.getRawQuery == null) List.empty else
      (uri.getRawQuery split "&").toList map { token =>
        val Array(k, v) = token split "="
        k -> v
      }

    Uri (
      secure = secure,
      host = if (host == null) "127.0.0.1" else host,
      port = if (port == -1) defaultPorts(uri.getScheme) else port,
      path = Option(path),
      query = queryList.toMap)
  }
}
