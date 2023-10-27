package com.github.andyglow.websocket

import org.scalatest.DoNotDiscover

@DoNotDiscover
final class PekkoPlatformSslSpec extends IntegrationSpecBase {
  override val platform: PekkoPlatform = PekkoPlatform
  override val ssl                     = false
  override val isPingSupported         = false
}
