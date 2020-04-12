package akka.classic

import java.io.File

import scala.io.Source
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.util.matching.Regex

object Ingestion {
  final object StartIngestion
  final object StopIngestion
  final case class Line(text: String)

  def props(input: String, output: String, nWorkers: Int) =
    Props(new Ingestion(input, output, nWorkers))
}

trait IngestionHandler {
  val ip: Regex = """.*?(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3}).*""".r
  val validIp: String => Boolean = line => ip.matches(line.split(",")(0))
}

class Ingestion(input: String, output: String, nWorkers: Int)
    extends Actor
    with ActorLogging
    with IngestionHandler {
  import Ingestion._

  val master: ActorRef = createMasterActor()
  lazy val source = Source.fromFile(createFile())

  override def receive: Receive = {
    case StartIngestion =>
      log.info("Initializing Master Actor...")
      master ! Master.Initialize
    case Master.MasterInitialized =>
      log.info("Starting ingestion...")
      source.getLines().filter(validIp).map(Line).foreach(master ! _)
      log.info("Collecting results...")
      master ! Master.CollectResults
    case aggregate @ Master.Aggregate(_) =>
      context.parent.forward(aggregate)
      self ! StopIngestion
    case StopIngestion =>
      source.close()
      context.parent ! Supervisor.Stop
  }

  def createMasterActor(): ActorRef = context.actorOf(Master.props(nWorkers), "master")

  def createFile(): File = new File(getClass.getClassLoader.getResource(input).getPath)
}
