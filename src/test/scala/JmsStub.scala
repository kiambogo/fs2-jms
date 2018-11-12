package fs2
package jms

import org.mockito.{MockitoSugar, ArgumentMatchersSugar}
import org.mockito.captor.ArgCaptor
import javax.jms._

trait JmsMock extends MockitoSugar with ArgumentMatchersSugar {
  val connectionFactory = mock[QueueConnectionFactory]
  val connection        = mock[QueueConnection]

  def mockTextMessage: String => TextMessage = { body =>
    val msg = mock[TextMessage]
    when(msg.getText) thenReturn body
    msg
  }

  def mockMessageProducer: MessageProducer = {
    val messageCaptor   = ArgCaptor[TextMessage]
    val cbCaptor        = ArgCaptor[CompletionListener]
    val messageProducer = mock[MessageProducer]
    doAnswer(cbCaptor.value.onCompletion(messageCaptor.value))
      .when(messageProducer)
      .send(messageCaptor, cbCaptor)
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
