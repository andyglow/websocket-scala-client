package com.github.andyglow.websocket

import org.scalatest.DoNotDiscover

@DoNotDiscover
final class AkkaPlatformSslSpec extends IntegrationSpecBase {
  override val platform: AkkaPlatform = AkkaPlatform
  override val ssl                    = false
  override val isPingSupported        = false
}
