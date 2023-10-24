package com.github.andyglow.websocket.util

import java.net.URI
import java.net.URLDecoder

/** Basic struct that represents URI. Unfortunately standard UR* classes from java doesn't provide methods that could be
  * used for mutation. On the other hand standard Scala's `product.copy` works pretty well here.
  *
  * @param secure
  * @param host
  * @param port
  * @param path
  * @param query
  */
case class ServerAddressBuilder(
  secure: Boolean,
  host: String,
  port: Int,
  path: Option[String] = None,
  query: Map[String, String] = Map.empty
) {

  val scheme: String = if (secure) "wss" else "ws"

  def build(): URI = {
    def queryStr = if (query.isEmpty) null
    else {
      query map { case (k, v) => s"$k=$v" } mkString "&"
    }

    new URI(scheme, null, host, port, path.orNull, queryStr, null)
  }
}

object ServerAddressBuilder {

  val defaultPorts: Map[String, Int] = Map("ws" -> 80, "wss" -> 443)

  def apply(uri: String): ServerAddressBuilder = apply(new URI(uri))

  def apply(uri: URI): ServerAddressBuilder = {
    require(uri.getScheme == "ws" || uri.getScheme == "wss")
    val secure = uri.getScheme == "wss"
    val host   = uri.getHost
    val port   = uri.getPort
    val path   = uri.getPath
    val queryList =
      if (uri.getRawQuery == null) List.empty
      else
        (uri.getRawQuery split "&").toList map { token =>
          token split "=" match {
            case Array(k: String, v: String) => (k, URLDecoder.decode(v, "UTF-8"))
            case arr =>
              throw new Exception(
                s"Query Parameter Parse Exception. Invalid token [$token] lead to [${arr mkString ","}]. Expected 'k=v' form"
              )
          }
        }

    ServerAddressBuilder(
      secure = secure,
      host = if (host == null) "127.0.0.1" else host,
      port = if (port == -1) defaultPorts(uri.getScheme) else port,
      path = Option(path),
      query = queryList.toMap
    )
  }
}
