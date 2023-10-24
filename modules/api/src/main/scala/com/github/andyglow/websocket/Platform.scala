package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.implicitConversions

trait Platform {
  // We call it a message type, but effectively on some Platforms like Netty and JDK we do not
  // control message framing completeness. So for some platforms it still should be treated
  // rather as a FrameType
  type MessageType
  type Binary <: MessageType
  type Text <: MessageType
  type Pong <: MessageType

  final type OnMessage          = PartialFunction[MessageType, Unit]
  final type OnUnhandledMessage = Function[MessageType, Unit]

  /** A Message Adapter aimed to be used to convert types to messages and vice versa.
    *
    * TODO: We should think about pluggable system that'd allow usage of serdes like Circe, SprayJson, PlayJson, etc
    *
    * @tparam T
    *   use type
    */
  trait MessageAdapter[T] {
    type F <: MessageType
    def toMessage(x: T): F
    // NOTE: some frameworks use effectful api instead of sync.
    //       in `akka/pekko` we need to resolve Future. Might be
    //       a good idea to allow pluggable effect systems at this level
    def fromMessage(x: F): T
  }

  object MessageAdapter {
    type Aux[T, FF <: MessageType] = MessageAdapter[T] { type F = FF }

    /** Message Adapters for the most common types used to pass messages
      */
    trait Implicits {
      // TODO: think about combining String + CharArray into a single CharSequence based adapter
      implicit val StringMessageAdapter: Aux[String, Text]
      implicit val CharArrayMessageAdapter: Aux[Array[Char], Text]
      implicit val CharBufferMessageAdapter: Aux[CharBuffer, Text]
      implicit val ByteArrayMessageAdapter: Aux[Array[Byte], Binary]
      implicit val ByteBufferMessageAdapter: Aux[ByteBuffer, Binary]
    }
  }

  protected def cast[T](
    x: MessageType,
    ifBinary: Binary => T,
    ifText: Text => T,
    ifPong: => T
  ): T

  private implicit class FrameOps(val f: MessageType) {
    @inline def asText(): Option[Text]     = cast(f, _ => None, Some.apply, None)
    @inline def asBinary(): Option[Binary] = cast(f, Some.apply, _ => None, None)
    @inline def asPong(): Option[Unit]     = cast(f, _ => None, _ => None, Some(()))
  }

  /** Collections of implicit converters that might seem useful when constructing WebsocketHandler
    */
  trait Implicits {

    implicit def websocketHandlerFromPartialFunction(pf: PartialFunction[MessageType, Unit]): WebsocketHandler =
      WebsocketHandler.Builder(pf).build()

    implicit def websocketHandlerFromWebsocketHandlerBuilder(builder: WebsocketHandler.Builder): WebsocketHandler =
      builder.build()
  }

  /** Most common options shared by all the platforms
    */
  trait CommonOptions {
    def headers: Map[String, String] = Map.empty
    def subProtocol: Option[String]  = None
  }

  /** Platform specific options
    */
  type Options <: CommonOptions

  /** Platform specific options used by default
    * @return
    */
  def defaultOptions: Options

  /** Create a new websocket client using specified [[address]] and [[options]].
    * @param address
    *   Websocket Server Address
    * @param options
    *   Platform Specific options. Default options will be used if parameter is not specified.
    * @return
    *   newly created Websocket Client
    */
  def newClient(address: ServerAddress, options: Options = defaultOptions): WebsocketClient

  /** Websocket Handler used to specify message exchange protocol
    */
  trait WebsocketHandler {
    @volatile private[websocket] var _sender: Websocket = NoopWebsocket

    /** Sender, a websocket object that can be used to interact with the sending party (a server)
      * @return
      *   Websocket
      */
    protected def sender(): Websocket = _sender

    /** A partial function used to specify an interaction with sending party. Including
      *   - reactions to incoming message
      *   - and/or sending reply messages
      *
      * @return
      */
    def onMessage: OnMessage

    /** This function, if specified, will be called for each unhandled (by [[onMessage]]) message.
      * @return
      */
    def onUnhandledMessage: OnUnhandledMessage = _ => ()

    /** We trigger this partial function in case of failures
      * @return
      */
    def onFailure: PartialFunction[Throwable, Unit] = { case _: Throwable => /* ignore errors */ }

    /** This function is going to be called when the socket is closed.
      * @return
      */
    def onClose: Unit => Unit = identity

    private[websocket] def reportFailure(th: Throwable): Unit = if (onFailure isDefinedAt th) onFailure(th)
  }

  object WebsocketHandler {

    /** Builder. Helps to create a [[WebsocketHandler]] instance based on severa; building blocks:
      *   - [[_onMessage]] handler
      *   - optional [[_onUnhandledMessage]] handler
      *   - optional [[_onFailure]] handler
      *   - optional [[_onClose]] handler
      *
      * Also useful in DSL form.
      */
    case class Builder(_onMessage: OnMessage) {
      private var _onUnhandledMessage: OnUnhandledMessage      = (_: MessageType) => ()
      private var _onFailure: PartialFunction[Throwable, Unit] = PartialFunction.empty
      private var _onClose: Unit => Unit                       = identity

      def onUnhandled(pf: PartialFunction[MessageType, Unit]): Builder = {
        _onUnhandledMessage = f => if (pf.isDefinedAt(f)) pf(f) else ()
        this
      }

      def onFailure(pf: PartialFunction[Throwable, Unit]): Builder = {
        _onFailure = pf
        this
      }

      def onClose(f: Unit => Unit): Builder = {
        _onClose = f
        this
      }

      def build(): WebsocketHandler = new WebsocketHandler {
        override def onMessage: PartialFunction[MessageType, Unit]   = _onMessage
        override def onUnhandledMessage: Function[MessageType, Unit] = _onUnhandledMessage
        override def onFailure: PartialFunction[Throwable, Unit]     = _onFailure
        override def onClose: Unit => Unit                           = _onClose
      }
    }
  }

  // Creates a builder basically. Dsl part
  def onMessage(onMessage: OnMessage): WebsocketHandler.Builder = WebsocketHandler.Builder(onMessage)

  trait WebsocketClient {
    def open(handler: WebsocketHandler): Websocket
    def shutdownSync(): Unit
    def shutdownAsync(implicit ec: ExecutionContext): Future[Unit]
    val implicits: MessageAdapter.Implicits with Implicits

    object M {
      object String {
        def unapply(x: MessageType): Option[String] = x.asText().map(implicits.StringMessageAdapter.fromMessage)
      }
      object `Array[Char]` {
        def unapply(x: MessageType): Option[Array[Char]] = x.asText().map(implicits.CharArrayMessageAdapter.fromMessage)
      }
      object CharBuffer {
        def unapply(x: MessageType): Option[CharBuffer] = x.asText().map(implicits.CharBufferMessageAdapter.fromMessage)
      }
      object `Array[Byte]` {
        def unapply(x: MessageType): Option[Array[Byte]] =
          x.asBinary().map(implicits.ByteArrayMessageAdapter.fromMessage)
      }
      object ByteBuffer {
        def unapply(x: MessageType): Option[ByteBuffer] =
          x.asBinary().map(implicits.ByteBufferMessageAdapter.fromMessage)
      }
      object Pong {
        def unapply(x: MessageType): Boolean = x.asPong().isDefined
      }
    }
  }

  trait Websocket {
    protected def send(x: MessageType): Unit
    final def send[T](x: T)(implicit bridge: MessageAdapter[T]): Unit = send(bridge.toMessage(x))
    def ping(): Unit
    def close()(implicit ec: ExecutionContext): Future[Unit]
  }

  object NoopWebsocket extends Websocket {
    override protected def send(x: MessageType): Unit                 = ()
    override def close()(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())
    override def ping(): Unit                                         = ()
    override def toString: String                                     = "noop-sender"
  }

  implicit class ByteBufferSyntaxExtension(private val bb: ByteBuffer) {

    def toByteArray: Array[Byte] = {
      if (bb.hasArray) bb.array()
      else {
        bb.rewind()
        val arr = Array.ofDim[Byte](bb.remaining())
        bb.get(arr)
        arr
      }
    }
  }
}
