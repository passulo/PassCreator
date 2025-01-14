// $ sbt reStart
// $ sbt reStop
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

// $ sbt scalafmtAll
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.3")

// $ sbt stage
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.0")
// $ sbt assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")

// assembly name uses git-tag as version number
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.2")
