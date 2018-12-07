package fs2
package jms

import fs2._
import fs2.jms.producer._
import cats.effect.{IO, ContextShift}
import org.scalatest.{FlatSpec, Matchers, BeforeAndAfterEach}
import org.mockito.captor.ArgCaptor
import jmstestkit.JmsQueue
import javax.jms._

import scala.concurrent.ExecutionContext

class ProducerSpec extends FlatSpec with Matchers {
  class TestContext(sessionCount: Int = 1) extends JmsMock {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)

    val producerSettings = {
      JmsProducerSettings(
        connectionFactory,
        sessionCount = sessionCount,
        queueName = "testQueue"
      )
    }

  }

  "JMS textPipe" should "send multiple messages to a queue successfully" in {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
    val jmsQueue = JmsQueue()
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings(jmsQueue)))
      .rethrow
      .compile
      .toVector
      .unsafeRunSync

    jmsQueue.toSeq shouldBe Seq("1", "2", "3", "4", "5", "6", "7", "8", "9")
  }

  it should "return a Left with an exception for a message that failed to send" in new TestContext {

    override def mockMessageProducer: MessageProducer = {
      val messageCaptor   = ArgCaptor[TextMessage]
      val messageProducer = mock[MessageProducer]
      doAnswer { msg: TextMessage =>
        if (msg.getText == "5")
          throw new Exception("Exception on send")
        else
          receivedMessages = receivedMessages :+ messageCaptor.value.getText()
      }.when(messageProducer).send(messageCaptor)
      messageProducer
    }

    val output = fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    receivedMessages shouldBe List(1, 2, 3, 4, 6, 7, 8, 9).map(_.toString)
    output.filter(_.isLeft).map(_.left.get.getMessage) shouldBe List("Exception on send")
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

  it should "close all sessions on stream termination" in new TestContext(sessionCount = 2) {
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    verify(session, times(2)).close()
  }

  it should "close the connection on stream termination" in new TestContext(sessionCount = 2) {
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textPipe[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    verify(connection, times(1)).close()
  }

  "JMS textSink" should "send multiple messages to a queue successfully" in {
    implicit val ec: ExecutionContext             = ExecutionContext.global
    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ec)
    val jmsQueue = JmsQueue()
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textSink[IO](producerSettings(jmsQueue)))
      .compile
      .toVector
      .unsafeRunSync

    jmsQueue.toSeq shouldBe Seq("1", "2", "3", "4", "5", "6", "7", "8", "9")
  }

  it should "return a Left with an exception for a message that failed to send" in new TestContext {

    override def mockMessageProducer: MessageProducer = {
      val messageCaptor   = ArgCaptor[TextMessage]
      val messageProducer = mock[MessageProducer]
      doAnswer { msg: TextMessage =>
        if (msg.getText == "5")
          throw new Exception("Exception on send")
        else
          receivedMessages = receivedMessages :+ messageCaptor.value.getText()
      }.when(messageProducer).send(messageCaptor)
      messageProducer
    }

    val output = fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textSink[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    receivedMessages shouldBe List(1, 2, 3, 4, 6, 7, 8, 9).map(_.toString)
  }

  it should "throw an error if there is a problem creating a connection" in new TestContext {

    doThrow(new JMSException("Error creating connection")).when(connectionFactory).createQueueConnection()

    assertThrows[JMSException] {
      val output = fs2.Stream
        .range(1, 10)
        .map(_.toString)
        .through(textSink[IO](producerSettings))
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
        .through(textSink[IO](producerSettings))
        .compile
        .toVector
        .unsafeRunSync
    }
  }

  it should "close all sessions on stream termination" in new TestContext(sessionCount = 2) {
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textSink[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    verify(session, times(2)).close()
  }

  it should "close the connection on stream termination" in new TestContext(sessionCount = 2) {
    fs2.Stream
      .range(1, 10)
      .map(_.toString)
      .through(textSink[IO](producerSettings))
      .compile
      .toVector
      .unsafeRunSync

    verify(connection, times(1)).close()
  }

  private def producerSettings(jmsQueue: JmsQueue, sessionCount: Int = 1): JmsProducerSettings = {
    JmsProducerSettings(
      jmsQueue.createQueueConnectionFactory,
      sessionCount = sessionCount,
      queueName = jmsQueue.queueName
    )
  }

}
