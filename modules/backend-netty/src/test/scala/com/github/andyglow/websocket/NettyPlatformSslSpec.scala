package com.github.andyglow.websocket

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.scalatest.DoNotDiscover

@DoNotDiscover
final class NettyPlatformSslSpec extends IntegrationSpecBase {
  import NettyPlatform._
  override val platform = NettyPlatform
  override val ssl = true
  override val isPingSupported = true
  override val options = NettyOptions(
    logLevel = Some(io.netty.handler.logging.LogLevel.INFO),
    sslCtx = Some(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build())
  )
}
