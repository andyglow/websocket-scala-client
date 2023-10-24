package com.github.andyglow.websocket

final class AkkaPlatformPlainSpec extends IntegrationSpecBase {
  import AkkaPlatform._
  override val platform = AkkaPlatform
  override val ssl = false
  override val isPingSupported = false
}
