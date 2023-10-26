package com.github.andyglow.websocket

import java.nio.ByteBuffer
import java.nio.CharBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.util.Try

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

  /** This is needed for some platforms to perform effect resolutions. In Akka/Pekko it contains a materializer which is
    * used to `msg.toStrict`
    */
  type InternalContext

  /** A Message Adapter aimed to be used to convert types to messages and vice versa.
    *
    * TODO: We should think about pluggable system that'd allow usage of serdes like Circe, SprayJson, PlayJson, etc
    *
    * @tparam T
    *   use type
    */
  trait MessageAdapter[T] { self =>
    type F <: MessageType
    def toMessage(x: T)(implicit ic: InternalContext): F
    // NOTE: some frameworks use effectful api instead of sync.
    //       in `akka/pekko` we need to resolve Future. Might be
    //       a good idea to allow pluggable effect systems at this level
    def fromMessage(x: F)(implicit ic: InternalContext): T

    def mapRight[TT](to: TT => T, from: T => TT): MessageAdapter[TT] = new MessageAdapter[TT] {
      override type F = self.F
      override def toMessage(x: TT)(implicit ic: InternalContext): F   = self.toMessage(to(x))
      override def fromMessage(x: F)(implicit ic: InternalContext): TT = from(self.fromMessage(x))
    }
  }

  object MessageAdapter {
    type Aux[T, FF <: MessageType] = MessageAdapter[T] { type F = FF }

    def apply[T, FF <: MessageType](to: T => FF, from: FF => T): MessageAdapter.Aux[T, FF] = new MessageAdapter[T] {
      override type F = FF
      override def toMessage(x: T)(implicit ic: InternalContext): FF   = to(x)
      override def fromMessage(x: FF)(implicit ic: InternalContext): T = from(x)
    }

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

  val implicits: MessageAdapter.Implicits with Implicits

  type Tracer = PartialFunction[Websocket.TracingEvent, Unit]
  protected type Trace = Websocket.TracingEvent => Unit

  /** Most common options shared by all the platforms
    */
  trait CommonOptions {
    def headers: Map[String, String] = Map.empty
    def subProtocol: Option[String]  = None
    def tracer: Tracer               = acceptingAnyPF(())
    def withTracer(tracer: Tracer): Options
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

    def shutdown(): Unit

    implicit val ic: InternalContext

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
    final def send[T](x: T)(implicit adapter: MessageAdapter[T], ic: InternalContext): Unit = send(adapter.toMessage(x))
    def ping(): Unit
    def close(): Unit
  }

  object Websocket {

    /** Internal trait supposed to be used in implementations which provide internally async api. Like these one: Jdk
      * Http Client, Netty, Akka, Pekko
      *
      * It converts async calls into sync
      */
    trait AsyncImpl extends Websocket {
      protected implicit val executionContext: ExecutionContext
      protected def timeout: FiniteDuration
      override def send(x: MessageType): Unit = Await.result(sendAsync(x), timeout)
      override def ping(): Unit               = Await.result(pingAsync(), timeout)
      override def close(): Unit              = Await.result(closeAsync(), timeout)

      protected def sendAsync(x: MessageType): Future[Unit]
      protected def pingAsync(): Future[Unit]
      protected def closeAsync(): Future[Unit]
    }

    trait TracingBase extends Websocket {
      private val rnd = new Random
      protected val trace: Trace
      protected def withTraceId[T](block: String => T): T = {
        val traceId = rnd.alphanumeric.take(8).mkString("")
        block(traceId)
      }
    }

    trait TracingSync extends Websocket with TracingBase {
      abstract override def send(x: MessageType): Unit = withTraceId { traceId =>
        try {
          trace(TracingEvent.Sending(traceId, x))
          super.send(x)
        } catch {
          case ex: Throwable => trace(TracingEvent.Sent(traceId, Failure(ex)))
        } finally {
          trace(TracingEvent.Sent(traceId, Success(x)))
        }
      }
      abstract override def ping(): Unit = withTraceId { traceId =>
        try {
          trace(TracingEvent.Pinging(traceId))
          super.ping()
        } catch {
          case ex: Throwable => trace(TracingEvent.Pinged(traceId, Failure(ex)))
        } finally {
          trace(TracingEvent.Pinged(traceId, Success(())))
        }
      }
      abstract override def close(): Unit = withTraceId { traceId =>
        try {
          trace(TracingEvent.Closing(traceId))
          super.close()
        } catch {
          case ex: Throwable => trace(TracingEvent.Closed(traceId, Failure(ex)))
        } finally {
          trace(TracingEvent.Closed(traceId, Success(())))
        }
      }
    }

    trait TracingAsync extends Websocket with TracingBase with AsyncImpl {
      abstract override protected def sendAsync(x: MessageType): Future[Unit] = withTraceId { traceId =>
        val f = for {
          _ <- Future.successful(trace(TracingEvent.Sending(traceId, x)))
          _ <- super.sendAsync(x)
        } yield x
        f.onComplete { result =>
          trace(TracingEvent.Sent(traceId, result))
        }
        f.map(_ => ())
      }
      abstract override protected def pingAsync(): Future[Unit] = withTraceId { traceId =>
        val f = for {
          _ <- Future.successful(trace(TracingEvent.Pinging(traceId)))
          x <- super.pingAsync()
        } yield x
        f.onComplete { result =>
          trace(TracingEvent.Pinged(traceId, result))
        }
        f.map(_ => ())
      }
      abstract override protected def closeAsync(): Future[Unit] = withTraceId { traceId =>
        val f = for {
          _ <- Future { trace(TracingEvent.Closing(traceId)) }
          x <- super.closeAsync()
        } yield x
        f.onComplete { result =>
          trace(TracingEvent.Closed(traceId, result))
        }
        f.map(_ => ())
      }
    }

    trait TracingEvent
    object TracingEvent {
      case class Received(value: MessageType) extends TracingEvent {
        override def toString: String = s"Received(${stringify(value)})"
      }
      case class Sending(traceId: String, value: MessageType) extends TracingEvent {
        override def toString: String = s"Sending($traceId, ${stringify(value)})"
      }
      case class Sent(traceId: String, result: Try[MessageType]) extends TracingEvent {
        override def toString: String = {
          val res = result match {
            case Success(r)  => stringify(r)
            case Failure(ex) => "failed(" + ex.getMessage + ")"
          }
          s"Sent($traceId, $res)"
        }
      }
      case class Pinging(traceId: String)                   extends TracingEvent
      case class Pinged(traceId: String, result: Try[Unit]) extends TracingEvent
      case class Closing(traceId: String)                   extends TracingEvent
      case class Closed(traceId: String, result: Try[Unit]) extends TracingEvent
    }
  }

  object NoopWebsocket extends Websocket {
    override protected def send(x: MessageType): Unit = ()
    override def close(): Unit                        = ()
    override def ping(): Unit                         = ()
    override def toString: String                     = "noop-sender"
  }

  implicit class ByteBufferSyntaxExtension(private val bb: ByteBuffer) {
    import java.nio.charset.StandardCharsets.UTF_8

    def asByteArray: Array[Byte] = {
      if (bb.hasArray) bb.array()
      else {
        bb.rewind()
        val arr = Array.ofDim[Byte](bb.remaining())
        bb.get(arr)
        arr
      }
    }

    def asString: String = {
      new String(asByteArray, UTF_8)
    }
  }

  implicit class CharBufferSyntaxExtension(private val bb: CharBuffer) {

    def asCharArray: Array[Char] = {
      if (bb.hasArray) bb.array()
      else {
        bb.rewind()
        val arr = Array.ofDim[Char](bb.remaining())
        bb.get(arr)
        arr
      }
    }

    def asString: String = {
      new String(asCharArray)
    }
  }

  protected def acceptingAnyPF[F, T](value: => T): PartialFunction[F, T] = new PartialFunction[F, T] {
    override def isDefinedAt(x: F): Boolean = true
    override def apply(v1: F): T            = value
    override def toString(): String         = "acceptingAnyPF"
  }

  protected def stringify(x: MessageType): String
}
