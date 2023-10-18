package com.github.andyglow.websocket.util

import com.github.andyglow.websocket.testserver.PlatformDependent._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.EventExecutorGroup
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues._
import org.scalatest.TryValues._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.Await
import scala.concurrent.duration._

class NettyFutureSpec extends AnyWordSpec with BeforeAndAfterAll {

  private var executors: EventExecutorGroup = _

  override def beforeAll(): Unit = {
    executors = new NioEventLoopGroup()
  }

  override def afterAll(): Unit = {
    executors.shutdownGracefully().syncUninterruptibly()
    ()
  }

  "NettyFuture" should {

    "handled resolved successful futures" in {
      val fut = NettyFuture(executors.submit(callable(() => "foo")).await())
      fut.value.isDefined shouldBe true
      fut.value.value.isSuccess shouldBe true
      fut.value.value.get shouldBe "foo"
    }

    "handled un-resolved successful futures" in {
      val fut = NettyFuture(executors.submit(callable(() => "foo")))
      val v   = Await.result(fut, 1.second)
      v shouldBe "foo"
    }

    "handled resolved failed futures" in {
      val fut = NettyFuture(executors.submit(callable[String](() => throw new IllegalStateException("err"))).await())
      fut.value.isDefined shouldBe true
      fut.value.value.isSuccess shouldBe false
      fut.value.value.failure.exception shouldBe a[IllegalStateException]
      fut.value.value.failure.exception.getMessage shouldBe "err"
    }

    "handled un-resolved failed futures" in {
      val fut = NettyFuture(executors.submit(callable[String](() => throw new IllegalStateException("err"))))
      val v   = the[IllegalStateException] thrownBy { Await.result(fut, 1.second) }
      v.getMessage shouldBe "err"
    }
  }
}
