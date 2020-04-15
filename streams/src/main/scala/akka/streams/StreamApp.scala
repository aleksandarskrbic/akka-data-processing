package akka.streams

import akka.actor.ActorSystem

import scala.concurrent.Future
import scala.util.{Failure, Success}

object StreamApp extends App {

  implicit val system = ActorSystem("actor-system")
  implicit val dispatcher = system.dispatcher

  val processor = new FileProcessor("data/weblog.csv")
  val result: Future[Seq[(String, Long)]] = processor.result()

  result.onComplete {
    case Success(result) =>
      result.sortBy(_._2).foreach(println)
      system.terminate()
    case Failure(_) => System.exit(1)
  }
}
