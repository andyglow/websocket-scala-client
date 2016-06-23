package scalax.websocket

import java.net.URL

import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshaker13, WebSocketVersion}
import io.netty.handler.codec.http.{FullHttpRequest, HttpHeaders}

class WebSocketClientHandshaker(url: URL, customHeaders: HttpHeaders) extends WebSocketClientHandshaker13(
  url.toURI, WebSocketVersion.V13, null, false, customHeaders, 65536) {

  override def newHandshakeRequest(): FullHttpRequest = {
    val req = super.newHandshakeRequest()
    req.headers().set(customHeaders)
    req
  }

}