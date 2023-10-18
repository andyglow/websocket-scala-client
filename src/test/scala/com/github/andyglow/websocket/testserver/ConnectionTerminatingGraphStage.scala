package com.github.andyglow.websocket.testserver

import akka.http.scaladsl.model.ws.Message
import akka.stream.Attributes
import akka.stream.FlowShape
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler

class ConnectionTerminatingGraphStage(pf: PartialFunction[Message, Unit])
    extends GraphStage[FlowShape[Message, Message]] {
  private val in  = Inlet[Message]("Close.in")
  private val out = Outlet[Message]("Close.out")

  override def shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    setHandlers(
      in,
      out,
      new InHandler with OutHandler {
        override def onPush(): Unit = {
          val m = grab(in)
          m match {
            case x if pf isDefinedAt x => completeStage()
            case x                     => push(out, x)
          }
        }

        override def onPull(): Unit = pull(in)
      }
    )
  }
}
