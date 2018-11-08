package fs2
package jms

import fs2._
import fs2.jms.producer._
import cats.effect.{IO, ContextShift}
import org.scalatest.{FlatSpec, Matchers, BeforeAndAfterEach}

import java.nio.file.FileSystems
import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.common.CommonConstants

import scala.concurrent.ExecutionContext

class ProducerSpec extends FlatSpec with Matchers {
  trait TestContext {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

    val producerSettings = {
      val connectionFactory = new MQQueueConnectionFactory()
      connectionFactory.setHostName("localhost")
      connectionFactory.setPort(1414)
      connectionFactory.setQueueManager("QM1")
      connectionFactory.setTransportType(CommonConstants.WMQ_CM_CLIENT)
      connectionFactory.setChannel("DEV.APP.SVRCONN")

      JmsProducerSettings(
        connectionFactory,
        sessionCount = 2,
        queueName = "DEV.QUEUE.1"
      )
    }

  }

  "JMS producer" should "send multiple messages to a queue successfully" in new TestContext {
    fs2.Stream.range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .rethrow
      .compile
      .toVector
      .unsafeRunSync
      .map(_.getBody(classOf[String])).toList shouldBe (List(1 to 9).flatten.map(_.toString))
  }

  it should "return a Left with an exception for a message that failed to send" in new TestContext {
    fs2.Stream.range(1, 10)
      .map(_.toString)
      .map {i => if (i == "5") throw new Exception("Failed to write to MQ"); else i }
      .through(textPipe[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync shouldBe (List(1 to 9).flatten.map(_.toString))
  }
}
