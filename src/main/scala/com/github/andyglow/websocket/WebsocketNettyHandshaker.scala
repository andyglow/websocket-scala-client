package com.github.andyglow.websocket

import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshaker13, WebSocketVersion}
import io.netty.handler.codec.http.{FullHttpRequest, HttpHeaders}

class WebsocketNettyHandshaker(uri: Uri, customHeaders: HttpHeaders) extends WebSocketClientHandshaker13(
  uri.toURI, WebSocketVersion.V13, null, false, customHeaders, 65536) {

  override def newHandshakeRequest(): FullHttpRequest = {
    val req = super.newHandshakeRequest()
    req.headers().add(customHeaders)
    req
  }

}