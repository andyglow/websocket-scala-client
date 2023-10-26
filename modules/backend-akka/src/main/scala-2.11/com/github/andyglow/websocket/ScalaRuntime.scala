package com.github.andyglow.websocket

import akka.Done
import akka.actor.ActorRef
import akka.actor.Status
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.HttpsConnectionContext
import akka.stream.CompletionStrategy
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import javax.net.ssl.SSLContext

object ScalaRuntime {

  def akka_http_ConnectionContext_httpsClient(sslCtx: SSLContext): HttpsConnectionContext = {
    ConnectionContext.https(sslCtx, None, None, None, None, None)
  }

  def akka_stream_completion_message: Status.Status = Status.Success(CompletionStrategy.immediately)

  def akka_stream_Source_actorRef[T](): Source[T, ActorRef] = {
    Source.actorRef(
      bufferSize = 10,
      OverflowStrategy.fail
    )
  }
}
