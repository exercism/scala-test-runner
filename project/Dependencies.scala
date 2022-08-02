import sbt._

object Dependencies {
  val monocleVersion = "2.0.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10"

  lazy val monocle = Seq(
    "com.github.julien-truffaut" %%  "monocle-core"  % monocleVersion,
    "com.github.julien-truffaut" %%  "monocle-macro" % monocleVersion
  )
}
