package akka.classic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.classic.Master.Aggregate

import scala.collection.mutable.ListBuffer

object Master {
  final object Initialize
  final object MasterInitialized
  final object CollectResults
  final case class Aggregate(result: Seq[(String, Long)])

  def props(nWorkers: Int) = Props(new Master(nWorkers))
}

trait MasterHandler {
  def toAggregate(results: ListBuffer[Worker.ResultResponse]): Aggregate = {
    val aggregate = results
      .map(_.state)
      .flatMap(_.toList)
      .groupBy(_._1)
      .map { case (k, v) => k -> v.map(_._2).sum }
      .toList
      .sortBy(_._2)
    Aggregate(aggregate)
  }
}

class Master(nWorkers: Int) extends Actor with ActorLogging with MasterHandler {
  import Master._

  val results = new ListBuffer[Worker.ResultResponse]()

  override def receive: Receive = {
    case Initialize =>
      log.info(s"Spawning $nWorkers workers...")
      val workers: Seq[ActorRef] = (1 to nWorkers).map(i => context.actorOf(Worker.props(i), s"worker-$i"))
      context.become(forwardTask(workers, 0))
      sender() ! MasterInitialized
  }

  def forwardTask(workers: Seq[ActorRef], currentWorker: Int): Receive = {
    case line @ Ingestion.Line(_) =>
      val worker = workers(currentWorker)
      worker ! line
      val nextWorker = (currentWorker + 1) % workers.length
      context.become(forwardTask(workers, nextWorker))
    case CollectResults =>
      workers.foreach(_ ! Worker.ResultRequest)
      context.become(collectResults())
  }

  def collectResults(): Receive = {
    case response @ Worker.ResultResponse(_, _) =>
      results += response
      if (results.length == nWorkers) {
        context.parent ! toAggregate(results)
      }
  }
}
