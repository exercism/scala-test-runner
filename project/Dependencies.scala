import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val scalaCheck = "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0"

  val monocleVersion = "3.2.0"

  lazy val monocle = Seq(
    "dev.optics" %%  "monocle-core"  % monocleVersion,
    "dev.optics" %%  "monocle-macro" % monocleVersion
  )

  lazy val json = "org.json" % "json" % "20220320"
}
