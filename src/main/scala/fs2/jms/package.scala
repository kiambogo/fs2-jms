package fs2

import javax.jms._
import cats.effect.{Effect, IO, SyncIO}
import cats.implicits._

import scala.util.{Try, Success, Failure}

package object jms {

  sealed trait JmsSettings {
    def connectionFactory: ConnectionFactory
    def sessionCount: Int
  }

  final case class JmsProducerSettings(connectionFactory: QueueConnectionFactory, sessionCount: Int = 1, queueName: String) extends JmsSettings {}

  final case class ProducerSession(session: Session, producer: MessageProducer)

  class JmsProducer[F[_]](settings: JmsProducerSettings, sessionCallback: QueueSession => Unit)(implicit F: Effect[F]) {
    val factory = settings.connectionFactory
    lazy val connection: F[QueueConnection] = F.delay {
      factory.createQueueConnection()
    }

    def initSession: F[QueueSession] =
      connection.flatMap { con =>
        F.delay {
          con.createQueueSession(false, AcknowledgeMode.AutoAcknowledge.code)
        }
      }

    def openSessions(): F[List[Unit]] = {
      List(1 to settings.sessionCount).flatten
        .traverse { _ =>
          F.runAsync(initSession) {
            case Right(session) => IO(sessionCallback(session))
            case Left(e)        => throw e
          }
        }
        .to[F]
    }

    def shutdown(): F[Unit] = {
      connection.flatMap(con => F.delay(con.close))
    }
  }

  final class AcknowledgeMode(val code: Int)

  object AcknowledgeMode {
    val AutoAcknowledge: AcknowledgeMode   = new AcknowledgeMode(Session.AUTO_ACKNOWLEDGE)
    val ClientAcknowledge: AcknowledgeMode = new AcknowledgeMode(Session.CLIENT_ACKNOWLEDGE)
    val DupsOkAcknowledge: AcknowledgeMode = new AcknowledgeMode(Session.DUPS_OK_ACKNOWLEDGE)
    val SessionTransacted: AcknowledgeMode = new AcknowledgeMode(Session.SESSION_TRANSACTED)
  }

}
