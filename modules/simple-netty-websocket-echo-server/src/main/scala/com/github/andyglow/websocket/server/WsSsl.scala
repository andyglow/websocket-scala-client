package com.github.andyglow.websocket.server

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

object WsSsl {

  lazy val selfSignedContext: SslContext = {
    val ssc = new SelfSignedCertificate
    SslContextBuilder.forServer(ssc.certificate, ssc.privateKey).build
  }
}
