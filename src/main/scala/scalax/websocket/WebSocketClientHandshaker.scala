package scalax.websocket

import java.net.{URL, URI}

import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.handler.codec.http.websocketx.{WebSocketClientHandshaker13, WebSocketHandshakeException, WebSocketVersion}
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse, HttpHeaders, HttpResponseStatus}

class WebSocketClientHandshaker(url: URL, customHeaders: HttpHeaders) extends WebSocketClientHandshaker13(
  url.toURI, WebSocketVersion.V13, null, false, customHeaders, 65536) {

  override def newHandshakeRequest(): FullHttpRequest = {
    val req = super.newHandshakeRequest()
    req.headers().set(customHeaders)
    req
  }

}