package com.github.andyglow.websocket

import org.scalatest.DoNotDiscover

@DoNotDiscover
final class AkkaPlatformSslSpec extends IntegrationSpecBase {
  import AkkaPlatform._
  override val platform = AkkaPlatform
  override val ssl = false
  override val isPingSupported = false
}
