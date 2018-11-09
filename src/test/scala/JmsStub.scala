package fs2
package jms

import org.mockito.{MockitoSugar, ArgumentMatchersSugar}
import org.mockito.captor.ArgCaptor

import javax.jms._

trait JmsMock extends MockitoSugar with ArgumentMatchersSugar {
  val connectionFactory = mock[QueueConnectionFactory]
  val connection = mock[QueueConnection]
  val session = mock[QueueSession]
  val queue = mock[Queue]
  val messageProducer = mock[MessageProducer]

  val callback = mock[CompletionListener]
  val cbCaptor = ArgCaptor[CompletionListener]
  val messageCaptor = ArgCaptor[TextMessage]

  val bodyCaptor = ArgCaptor[String]

  var messagesMap: Map[Message, String] = Map()


  when(connectionFactory.createQueueConnection()) thenReturn connection
  when(connection.createQueueSession(any, any)) thenReturn session
  when(session.createQueue(any)) thenReturn queue
  when(session.createProducer(queue)) thenReturn messageProducer
  doAnswer { (body: String) =>
    val msg = mock[TextMessage]
    when(msg.getText) thenReturn body
    messagesMap += (msg -> body)
    msg}.when(session).createTextMessage(any[String])
  doAnswer(cbCaptor.value.onCompletion(messageCaptor.value)).when(messageProducer).send(messageCaptor, cbCaptor)
}
