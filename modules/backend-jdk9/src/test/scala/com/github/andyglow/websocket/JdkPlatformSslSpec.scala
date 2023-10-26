package com.github.andyglow.websocket

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.scalatest.DoNotDiscover

@DoNotDiscover
final class JdkPlatformSslSpec extends IntegrationSpecBase {
  override val platform: JdkPlatform = JdkPlatform
  override val ssl                   = true
  override val isPingSupported       = true
  override val options: platform.JdkOptions = {

    val trustAny = Array[TrustManager](new X509TrustManager() {
      def getAcceptedIssuers: Array[X509Certificate]                                = Array.ofDim[X509Certificate](0)
      def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
      def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    })

    // Ignore differences between given hostname and certificate hostname
    val sslCtx = SSLContext.getInstance("SSL")
    sslCtx.init(null, trustAny, new SecureRandom())

    platform.JdkOptions(
      sslCtx = Some(sslCtx)
    )
  }

}
