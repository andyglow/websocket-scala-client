package com.github.andyglow.websocket

final class PekkoPlatformPlainSpec extends IntegrationSpecBase {
  override val platform: PekkoPlatform = PekkoPlatform
  override val ssl                     = false
  override val isPingSupported         = false
}
