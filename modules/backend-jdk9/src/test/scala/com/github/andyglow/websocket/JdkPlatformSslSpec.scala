package com.github.andyglow.websocket

import org.scalatest.DoNotDiscover

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}

@DoNotDiscover
final class JdkPlatformSslSpec extends IntegrationSpecBase {
  import JdkPlatform._
  override val platform = JdkPlatform
  override val ssl = true
  override val isPingSupported = true
  override val options = {

    val trustAny = Array[TrustManager](new X509TrustManager() {
      def getAcceptedIssuers: Array[X509Certificate] = Array.ofDim[X509Certificate](0)
      def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
      def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    })

    // Ignore differences between given hostname and certificate hostname
    val sslCtx = SSLContext.getInstance("SSL")
    sslCtx.init(null, trustAny, new SecureRandom())

    JdkOptions(
      sslCtx = Some(sslCtx)
    )
  }

}
