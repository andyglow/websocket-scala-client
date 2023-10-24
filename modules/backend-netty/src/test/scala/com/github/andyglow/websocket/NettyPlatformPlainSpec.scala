package com.github.andyglow.websocket

final class NettyPlatformPlainSpec extends IntegrationSpecBase {
  import NettyPlatform._
  override val platform = NettyPlatform
  override val ssl = false
  override val isPingSupported = true
  override val options = NettyOptions(
    logLevel = Some(io.netty.handler.logging.LogLevel.INFO)
  )
}
