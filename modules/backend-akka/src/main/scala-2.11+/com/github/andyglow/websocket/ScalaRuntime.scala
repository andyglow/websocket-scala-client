package com.github.andyglow.websocket

import akka.Done
import akka.actor.ActorRef
import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.Source

import javax.net.ssl.SSLContext


object ScalaRuntime {

  def akka_http_ConnectionContext_httpsClient(sslCtx: SSLContext): HttpsConnectionContext = {
    ConnectionContext.httpsClient(sslCtx)
  }

  def akka_stream_completion_message: Done = Done

  def akka_stream_Source_actorRef[T](): Source[T, ActorRef] = {
    Source.actorRef(
      completionMatcher = { case Done =>
        CompletionStrategy.immediately
      },
      failureMatcher = PartialFunction.empty,
      bufferSize = 10,
      OverflowStrategy.fail
    )
  }
}
