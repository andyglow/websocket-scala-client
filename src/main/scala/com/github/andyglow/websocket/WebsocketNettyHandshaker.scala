package com.github.andyglow.websocket

import com.github.andyglow.websocket.util.Uri
import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshaker13, WebSocketVersion}
import io.netty.handler.codec.http.{FullHttpRequest, HttpHeaders}

class WebsocketNettyHandshaker(
  uri: Uri,
  customHeaders: HttpHeaders,
  subprotocol: Option[String] = None,
  maxFramePayloadLength: Int = 65536) extends WebSocketClientHandshaker13(
  uri.toURI,
  WebSocketVersion.V13,
  subprotocol.orNull,
  false,
  customHeaders,
  maxFramePayloadLength) {

  override def newHandshakeRequest(): FullHttpRequest = {
    val req = super.newHandshakeRequest()
    req.headers().add(customHeaders)
    req
  }
}
