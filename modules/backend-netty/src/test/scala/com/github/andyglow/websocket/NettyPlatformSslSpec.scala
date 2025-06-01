package com.github.andyglow.websocket

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.scalatest.DoNotDiscover

@DoNotDiscover
final class NettyPlatformSslSpec extends IntegrationSpecBase {
  override val platform: NettyPlatform        = NettyPlatform
  override val ssl                            = true
  override val isPingSupported                = true
  override val options: platform.NettyOptions = platform.NettyOptions(
    logLevel = Some(io.netty.handler.logging.LogLevel.INFO),
    sslCtx = Some(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build())
  )
}
