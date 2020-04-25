package akka.streams

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Application extends App with LazyLogging {

  implicit val system = ActorSystem("actor-system")
  implicit val dispatcher = system.dispatcher

  val processor = new LogProcessor("data/weblog.csv")
  val processorResult: Future[Seq[(String, Long)]] = processor.result

  processorResult.onComplete {
    case Success(result) =>
      logger.info("Stream completed successfully.")
      result.sortBy(_._2).foreach(println)
      system.terminate()
    case Failure(ex) =>
      logger.error("Stream completion failed.", ex)
      system.terminate()
      System.exit(1)
  }
}
