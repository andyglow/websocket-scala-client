package com.github.andyglow.websocket

final class AkkaPlatformPlainSpec extends IntegrationSpecBase {
  override val platform: AkkaPlatform = AkkaPlatform
  override val ssl = false
  override val isPingSupported = false
}
