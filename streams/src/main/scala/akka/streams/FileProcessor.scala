package akka.streams

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

final class FileProcessor(path: String)(implicit system: ActorSystem, ec: ExecutionContext) {

  def result(): Future[Seq[(String, Long)]] =
    FileIO
      .fromPath(createPath())
      .via(
        Framing.delimiter(
          ByteString("\n"),
          maximumFrameLength = 256,
          allowTruncation = true
        )
      )
      .mapAsyncUnordered(4)(data => Future { data.utf8String })
      .filter(line => validateIp(line))
      .mapAsyncUnordered(4)(line => Future { extractStatusCode(line) })
      .groupBy(5, identity)
      .mapAsyncUnordered(4)(status => Future { status -> 1L })
      .reduce((left, right) => (left._1, left._2 + right._2))
      .mergeSubstreams
      .runWith(Sink.seq[(String, Long)])

  private def createPath(): Path =
    Paths.get(getClass.getClassLoader.getResource(path).getPath)

  private def validateIp(line: String): Boolean = {
    val ipRegex = """.*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""".r
    ipRegex.matches(line.split(",")(0))
  }

  private def extractStatusCode(line: String): String =
    line.split(",").toList match {
      case _ :: _ :: _ :: status :: _ => status.trim
    }
}
