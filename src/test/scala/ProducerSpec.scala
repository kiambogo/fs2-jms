package fs2
package jms

import fs2._
import fs2.jms.producer._
import cats.effect.{IO, ContextShift}
import org.scalatest.{FlatSpec, Matchers, BeforeAndAfterEach}

import javax.jms._

import scala.concurrent.ExecutionContext

class ProducerSpec extends FlatSpec with Matchers with JmsMock {
  trait TestContext {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

    val producerSettings = {
      JmsProducerSettings(
        connectionFactory,
        sessionCount = 1,
        queueName = "testQueue"
      )
    }

  }

  "JMS producer" should "send multiple messages to a queue successfully" in new TestContext {
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .rethrow
      .compile
      .toVector
      .unsafeRunSync
      .map(_.getText)
      .toList shouldBe (List(1 to 9).flatten.map(_.toString))
  }

  it should "return a Left with an exception for a message that failed to send" in new TestContext {

    doAnswer { (msg: TextMessage, callback: CompletionListener) =>
      if (msg.getText == "5")
        cbCaptor.value.onException(msg, new Exception("HI"))
      else
        cbCaptor.value.onCompletion(messageCaptor.value)
    }.when(messageProducer).send(messageCaptor, cbCaptor)

    val output = fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync
      .partition(_.isRight)

    output._1.map(_.right.get.getText).toList shouldBe List(1, 2, 3, 4, 6, 7, 8, 9).map(_.toString)
    output._2.map(_.left.get.getMessage).toList shouldBe List("HI")
  }

  it should "throw an error if there is a problem creating a connection" in new TestContext {

    doThrow(new JMSException("Error creating connection")).when(connectionFactory).createQueueConnection()

    assertThrows[JMSException] {
      val output = fs2.Stream
        .range(1, 10)
        .map(_.toString)
        .through(textPipe[IO](producerSettings))
        .compile
        .toVector
        .unsafeRunSync
    }
  }

  it should "throw an error if there is a problem creating a session" in new TestContext {

    doThrow(new JMSException("Error creating session")).when(connection).createQueueSession(any, any)

    assertThrows[JMSException] {
      val output = fs2.Stream
        .range(1, 10)
        .map(_.toString)
        .through(textPipe[IO](producerSettings))
        .compile
        .toVector
        .unsafeRunSync
    }

  }
}
