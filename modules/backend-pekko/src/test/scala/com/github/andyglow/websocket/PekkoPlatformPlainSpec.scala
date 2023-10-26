package com.github.andyglow.websocket

final class PekkoPlatformPlainSpec extends IntegrationSpecBase {
  import PekkoPlatform._
  override val platform        = PekkoPlatform
  override val ssl             = false
  override val isPingSupported = false
}
