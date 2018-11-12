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
      def sessionCreatedCallBack(producers: Queue[F, Option[ProducerSession]]): QueueSession => Unit = { session =>
        F.runAsync(
            producers.enqueue1(
              ProducerSession(session, session.createProducer(session.createQueue(producerSettings.queueName))).some
            ))(_ => IO.unit)
          .unsafeRunSync
      }

      Stream.eval(Queue.bounded[F, Option[ProducerSession]](producerSettings.sessionCount + 1)).flatMap { queue =>
        val jmsProducer = new JmsProducer[F](producerSettings, sessionCreatedCallBack(queue))
        Stream.eval_(jmsProducer.openSessions) ++ in
          .evalMap { elem =>
            for {
              sessionProducer <- queue.dequeue1
              msg = sessionProducer.get.session.createTextMessage(elem)
              callback <- Effect[F]
                .async[TextMessage] { cb =>
                  val callback = MessageSentCallback(cb)
                  sessionProducer.get.producer.send(msg, callback)
                }
                .attempt
              _ <- queue.enqueue1(sessionProducer)
            } yield callback
          }
          .onFinalize {
            (Stream.eval_(queue.enqueue1(None)) ++
              queue.dequeue.unNoneTerminate.evalMap { producerSession =>
                F.delay(producerSession.session.close())
              } ++
              Stream.eval_(jmsProducer.shutdown)).compile.drain
          }
      }
  }
}
