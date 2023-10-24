package com.github.andyglow.websocket.testserver

import akka.http.scaladsl.model.ws.Message
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

class ConnectionTerminatingGraphStage(pf: PartialFunction[Message, Unit]) extends GraphStage[FlowShape[Message, Message]] {
  private val in = Inlet[Message]("Close.in")
  private val out = Outlet[Message]("Close.out")

  override def shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    setHandlers(in, out, new InHandler with OutHandler {
      override def onPush(): Unit = {
        val m = grab(in)
        m match {
          case x if pf isDefinedAt x => completeStage()
          case x => push(out, x)
        }
      }

      override def onPull(): Unit = pull(in)
    })
  }
}