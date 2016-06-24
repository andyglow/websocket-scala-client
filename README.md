# Websocket Client for Scala
[![Build Status](https://travis-ci.org/andyglow/websocket-scala-client.svg)](https://travis-ci.org/andyglow/websocket-scala-client)
[![Download](https://api.bintray.com/packages/andyglow/scala-tools/websocket-scala-client/images/download.svg) ](https://bintray.com/andyglow/scala-tools/websocket-scala-client/_latestVersion)

WebSocket client based on Netty

## Usage

### build.sbt
```
libraryDependencies += "com.github.andyglow" %% "websocket-scala-client" % ${LATEST_VERSION} % Compile
```

### Code

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

For more examples please see [Examples Section](https://github.com/andyglow/websocket-scala-client/tree/develop/src/example/scala)