name := "akka-data-processing"
organization in ThisBuild := "com.aleksandarskrbic"
scalaVersion in ThisBuild := "2.13.1"

lazy val global = project
  .in(file("."))
  .aggregate(actors, streams)

lazy val actors = (project in file("actors"))
  .settings(
    name := "actors",
    libraryDependencies ++= Seq(dependencies.akkaActors, dependencies.logback)
  )

lazy val streams = (project in file("streams"))
  .settings(
    name := "streams",
    libraryDependencies ++= Seq(
      dependencies.akkaActors,
      dependencies.akkaStreams
    )
  )

lazy val dependencies = new {
  val akkaVersion = "2.6.0"
  val logbackVersion = "1.2.2"

  val akkaActors = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
}
