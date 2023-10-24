package com.github.andyglow.websocket

final class JdkPlatformPlainSpec extends IntegrationSpecBase {
  import JdkPlatform._
  override val platform = JdkPlatform
  override val ssl = false
  override val isPingSupported = true
  override val options = JdkOptions()
}
