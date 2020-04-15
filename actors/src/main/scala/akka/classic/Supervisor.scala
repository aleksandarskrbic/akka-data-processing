package akka.classic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Supervisor {
  final object Start
  final object Stop

  def props(input: String, output: String, parallelism: Int) =
    Props(new Supervisor(input, output, parallelism))
}

class Supervisor(input: String, output: String, parallelism: Int)
    extends Actor
    with ActorLogging {
  import Supervisor._

  val ingestion: ActorRef = createIngestionActor()

  override def receive: Receive = {
    case Start =>
      ingestion ! Ingestion.StartIngestion
    case aggregate @ Master.Aggregate(_) =>
      aggregate.result.foreach(println)
    case Stop =>
      context.system.terminate()
  }

  def createIngestionActor(): ActorRef =
    context.actorOf(
      Ingestion.props(input, output, parallelism),
      "ingestion-actor"
    )
}
