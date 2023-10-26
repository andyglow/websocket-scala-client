package com.github.andyglow.websocket

final class NettyPlatformPlainSpec extends IntegrationSpecBase {
  override val platform: NettyPlatform = NettyPlatform
  override val ssl                     = false
  override val isPingSupported         = true
  override val options: platform.NettyOptions = platform.NettyOptions(
    logLevel = Some(io.netty.handler.logging.LogLevel.INFO)
//    tracingEventHandler = {
//      case event => println(s"TRACE: $event")
//    }
  )
}
