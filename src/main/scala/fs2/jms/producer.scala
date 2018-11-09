package fs2
package jms

import javax.jms._
import fs2.concurrent.Queue
import cats.implicits._
import cats.effect.{Effect, ConcurrentEffect, IO}
import cats.effect.concurrent.Ref

package object producer {
  def textPipe[F[_]](producerSettings: JmsProducerSettings)(implicit F: ConcurrentEffect[F]): fs2.Pipe[F, String, Either[Throwable, TextMessage]] = {
    in =>
      /* Callback method to invoke when a session is instantiated.
       * We construct a new ProducerSession, comprising of the session and the corresponding
       * MessageProducer for the session and append this to the producer queue.
       */
    def sessionCreatedCallBack(producers: Queue[F, ProducerSession]): QueueSession => Unit = { session =>
      F.runAsync(producers.enqueue1(
        ProducerSession(session, session.createProducer(session.createQueue(producerSettings.queueName)))
      ))(_ => IO.unit).unsafeRunSync
    }

      Stream.eval(Queue.bounded[F, ProducerSession](producerSettings.sessionCount)).flatMap { queue =>
        val jmsProducer = new JmsProducer[F](producerSettings, sessionCreatedCallBack(queue))
        Stream.eval_(jmsProducer.openSessions.unsafeRunSync)

        in.evalMap { elem =>
          for {
            sessionProducer <- queue.dequeue1
            msg = sessionProducer.session.createTextMessage(elem)
            callback <- Effect[F].async[TextMessage] { cb =>
              val callback = MessageSentCallback(cb)
              val s = sessionProducer.producer.send(msg, callback)
              s
            }
            _ <- queue.enqueue1(sessionProducer)
          } yield callback
        }.attempt
      }
  }
}
