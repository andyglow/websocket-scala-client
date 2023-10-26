package com.github.andyglow.websocket

import org.scalatest.DoNotDiscover

@DoNotDiscover
final class PekkoPlatformSslSpec extends IntegrationSpecBase {
  import PekkoPlatform._
  override val platform        = PekkoPlatform
  override val ssl             = false
  override val isPingSupported = false
}
