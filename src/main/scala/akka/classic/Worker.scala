package akka.classic

import akka.actor.{Actor, ActorLogging, Props}

object Worker {
  final object ResultRequest
  final case class ResultResponse(id: Int, state: Map[String, Long])
  final case class Date(month: String, year: Integer, hour: Integer)
  final case class Log(ip: String, date: Date, url: String, status: String)

  def props(id: Int) = Props(new Worker(id))
}

trait WorkerHandler {
  import Worker._

  def toLog(line: String): Log = line.split(",").toList match {
    case ip :: time :: url :: status :: _ =>
      val date = time.substring(1, time.length).split("/").toList match {
        case _ :: month :: timeParts :: _ =>
          val year = timeParts.split(":")(0).toInt
          val hour = timeParts.split(":")(1).toInt
          Date(month, year, hour)
      }
      Log(ip, date, url, status)
  }
}

class Worker(id: Int) extends Actor with ActorLogging with WorkerHandler {
  import Worker._

  private type StatusCode = String
  private type Count = Long

  override def receive: Receive = process(Map.empty)

  def process(state: Map[StatusCode, Count]): Receive = {
    case Ingestion.Line(text) =>
      val status = toLog(text).status
      state.get(status) match {
        case Some(count) =>
          val newState: Map[StatusCode, Count] = state + (status -> (count + 1))
          context.become(process(newState))
        case None =>
          val newState: Map[StatusCode, Count] = state + (status -> 1)
          context.become(process(newState))
      }

    case ResultRequest =>
      sender() ! ResultResponse(id, state)
  }
}
