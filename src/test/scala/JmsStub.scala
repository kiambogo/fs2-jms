package fs2
package jms

import javax.jms._
import org.mockito.{MockitoSugar, ArgumentMatchersSugar}
import org.mockito.captor.ArgCaptor

trait JmsMock extends MockitoSugar with ArgumentMatchersSugar {
  val connectionFactory              = mock[QueueConnectionFactory]
  val connection                     = mock[QueueConnection]
  var receivedMessages: List[String] = List()

  def mockTextMessage: String => TextMessage = { body =>
    val msg = mock[TextMessage]
    when(msg.getText) thenReturn body
    msg
  }

  def mockMessageProducer: MessageProducer = {
    val messageCaptor   = ArgCaptor[TextMessage]
    val messageProducer = mock[MessageProducer]
    doAnswer { receivedMessages = receivedMessages :+ messageCaptor.value.getText(); () }
      .when(messageProducer)
      .send(messageCaptor)
    messageProducer
  }

  def mockSession: QueueSession = {
    val session = mock[QueueSession]
    val queue   = mock[Queue]
    val mmp     = mockMessageProducer
    when(session.createQueue(any)) thenReturn queue
    when(session.createProducer(queue)) thenReturn mmp
    doAnswer(()).when(session).close()
    doAnswer(b => mockTextMessage(b)).when(session).createTextMessage(any[String])
    session
  }

  val session = mockSession
  when(connection.createQueueSession(any, any)) thenReturn session
  doAnswer(()).when(connection).close()
  when(connectionFactory.createQueueConnection()) thenReturn connection
}
