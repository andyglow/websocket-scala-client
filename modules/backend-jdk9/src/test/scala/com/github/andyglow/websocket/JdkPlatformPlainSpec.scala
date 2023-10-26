package com.github.andyglow.websocket

final class JdkPlatformPlainSpec extends IntegrationSpecBase {
  override val platform: JdkPlatform = JdkPlatform
  override val ssl = false
  override val isPingSupported = true
}
