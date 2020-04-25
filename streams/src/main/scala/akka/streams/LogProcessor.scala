package akka.streams

import java.nio.file.{Path, Paths}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink}
import akka.stream.OverflowStrategy
import akka.util.ByteString
import akka.NotUsed

import scala.concurrent.{ExecutionContext, Future}

final class LogProcessor(path: String)(implicit system: ActorSystem, ec: ExecutionContext) {

  def result: Future[Seq[(String, Long)]] =
    FileIO
      .fromPath(createPath)
      .via(lineDelimiter)
      .mapAsyncUnordered(2)(data => Future(data.utf8String))
      .async
      .filter(validateIp)
      .mapAsyncUnordered(2)(line => extractStatusCode(line))
      .buffer(1000, OverflowStrategy.backpressure)
      .groupBy(5, identity)
      .mapAsyncUnordered(2)(status => Future(status -> 1L))
      .reduce((left, right) => (left._1, left._2 + right._2))
      .mergeSubstreams
      .runWith(Sink.seq[(String, Long)])

  private val createPath: Path =
    Paths.get(getClass.getClassLoader.getResource(path).getPath)

  private val lineDelimiter: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 256,
      allowTruncation = true
    )

  private def validateIp(line: String): Boolean = {
    val ipRegex = """.*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""".r
    ipRegex.matches(line.split(",")(0))
  }

  private def extractStatusCode(line: String): Future[String] = Future {
    line.split(",").toList match {
      case _ :: _ :: _ :: status :: _ => status.trim
    }
  }
}
