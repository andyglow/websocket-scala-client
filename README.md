# Scala Websocket Client

[![build](https://github.com/andyglow/websocket-scala-client/actions/workflows/ci.yml/badge.svg)](https://github.com/andyglow/websocket-scala-client/actions/workflows/ci.yml)
[![coverage](https://codecov.io/gh/andyglow/websocket-scala-client/graph/badge.svg?token=XJUuxQhbSH)](https://codecov.io/gh/andyglow/websocket-scala-client)
[![mvn](https://img.shields.io/badge/dynamic/json.svg?label=mvn&query=%24.response.docs%5B0%5D.latestVersion&url=https%3A%2F%2Fsearch.maven.org%2Fsolrsearch%2Fselect%3Fq%3Dwebsocket-scala-client_2.13%26start%3D0%26rows%3D1)](https://search.maven.org/artifact/com.github.andyglow/websocket-scala-client_2.13/)

Scala Websocket Client is an open-source library which provides a clean, 
programmer-friendly API to describe Websocket request/response machinery. 
Communication with server handled using one of the backends, which wrap other 
Scala or Java HTTP client implementations. 

Supported backends:
- [x] Netty
- [x] JDK Http Client
- [x] Akka
- [x] Pekko
- [ ] Zio-Http
- [ ] Async-http-client
- [ ] OkHttp
- [ ] Armeria

Supported Serialization/Deserialization formats:
- [x] Avro using Avro4s
- [ ] Protobuf
- [ ] Json using Circe
- [ ] Json using Play-Json
- [ ] Json using Spray-Json
- [ ] Json using Jsoniter-scala
- [ ] Json using Borer
- [ ] Json using uJson
- [ ] CBOR using Borer