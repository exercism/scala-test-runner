scalaVersion := "2.13.6"

val monocleVersion = "2.0.0"

libraryDependencies ++= Seq(
  "com.github.julien-truffaut" %%  "monocle-core"  % monocleVersion,
  "com.github.julien-truffaut" %%  "monocle-macro" % monocleVersion,
  "org.scalatest" %% "scalatest" % "3.2.10" % "test"
)
