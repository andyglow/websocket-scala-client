# Websocket Client for Scala
[![Build Status](https://travis-ci.org/andyglow/websocket-scala-client.svg)](https://travis-ci.org/andyglow/websocket-scala-client)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.andyglow/websocket-scala-client_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.andyglow/websocket-scala-client_2.11)
[![Download](https://api.bintray.com/packages/andyglow/scala-tools/websocket-scala-client/images/download.svg) ](https://bintray.com/andyglow/scala-tools/websocket-scala-client/_latestVersion)

WebSocket client based on Netty

## Usage

### build.sbt
```
libraryDependencies += "com.github.andyglow" %% "websocket-scala-client" % ${LATEST_VERSION} % Compile
```

### Code

#### Import
```scala
import com.github.andyglow.websocket._
```

#### Simple example
```scala
  // 1. prepare ws-client
  // 2. define message handler
  val cli = WebsocketClient[String]("ws://echo.websocket.org") {
    case str =>
      logger.info(s"<<| $str")
  }

  // 4. open websocket
  val ws = cli.open()

  // 5. send messages
  ws ! "hello"
  ws ! "world"
```

#### A bit more advantaged example
```scala
  // define some mutex to be able to shutdown app when message passing ends 
  val semaphore = new Semaphore(0)
  
  // define our protocol handler
  val protocolHandler = new WebsocketHandler[String]() {
    def receive = {
      case str if str startsWith "repeat " =>
        sender() ! "repeating " + str.substring(7)
        logger.info(s"<<| $str")

      case str if str endsWith "close" =>
        logger.info(s"<<! $str")
        sender().close()
        semaphore.release()

      case str =>
        logger.info(s"<<| $str")
    }
  }

  // create websocket client and open connection
  val cli = WebsocketClient(Uri("ws://echo.websocket.org"), protocolHandler)
  val ws = cli.open()

  // send some messages
  ws ! "hello"
  ws ! "world"
  ws ! "repeat and close"
  
  // wait for echoed 'repeating close'
  semaphore.acquire(1)
  
  // shutdown whole the netty stack
  cli.shutdown()
```
Defining websocket handler this way we are able to communicate back straight from handler. Use `sender()` for that. 

For more examples please see [Examples Section](https://github.com/andyglow/websocket-scala-client/tree/develop/src/example/scala)

To run examples one can use `sbt`
```
sbt> example:run
```
or
```
sbt> example:runMain [BinaryEchoWebSocketOrg|SimplifiedTextEchoWebSocketOrg|TextEchoWebSocketOrg]
```