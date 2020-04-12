package akka.classic

import akka.actor.ActorSystem

object Application extends App {

  implicit val system = ActorSystem("actor-system")

  val supervisor =
    system.actorOf(Supervisor.props("data/weblog.csv", "data/results/weblog.csv", 3), "supervisor")
  supervisor ! Supervisor.Start
}
