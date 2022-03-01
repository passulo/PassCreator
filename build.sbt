lazy val root = (project in file("."))
  .settings(
    name                 := "PassCreator",
    normalizedName       := "pass-creator",
    organization         := "com.passulo",
    organizationName     := "Passulo",
    organizationHomepage := Some(url("https://www.passulo.com")),
    description          := "Program to create Passulo passes for a list of members",
    scmInfo              := Some(ScmInfo(url("https://github.com/passulo/PassCreator"), "git@github.com:passulo/PassCreator.git")),
    developers           := List(Developer("jannikarndt", "Jannik Arndt", "@jannikarndt", url("https://github.com/JannikArndt"))),
    maintainer           := "mail@passulo.com",
    version              := "1.0.0",
    scalaVersion         := "2.13.8",
    scalacOptions        := scalaCompilerOptions,
    libraryDependencies ++= dependencies ++ testDependencies ++ loggingDependencies
  )
  .enablePlugins(JavaAppPackaging)

lazy val dependencies = Seq(
  "de.brendamour"          % "jpasskit"           % "0.2.0",
  "com.github.pureconfig" %% "pureconfig"         % "0.17.1",
  "com.nrinaudo"          %% "kantan.csv"         % "0.6.2",
  "com.nrinaudo"          %% "kantan.csv-java8"   % "0.6.2",
  "com.nrinaudo"          %% "kantan.csv-generic" % "0.6.2",
  "com.thesamet.scalapb"  %% "scalapb-runtime"    % "0.11.8",
  "info.picocli"           % "picocli"            % "4.6.3"
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest"     % "3.2.11" % Test,
  "org.mockito"   %% "mockito-scala" % "1.17.5" % Test
)

lazy val log4JVersion = "2.17.2"

lazy val loggingDependencies = Seq(
  // scala-logging wraps SLF4J, which can use log4j2
  "com.typesafe.scala-logging" %% "scala-logging"    % "3.9.4",
  "org.apache.logging.log4j"    % "log4j-api"        % log4JVersion,
  "org.apache.logging.log4j"    % "log4j-core"       % log4JVersion,
  "org.apache.logging.log4j"    % "log4j-slf4j-impl" % log4JVersion % "runtime",
  "com.lmax"                    % "disruptor"        % "3.4.4"      % "runtime"
)

lazy val scalaCompilerOptions = Seq(
  "-target:17",
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8",         // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",   // Wrap field accessors to throw an exception on uninitialized access.
//  "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args",       // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",           // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached",       // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",       // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",          // Warn when a type argument is inferred to be `Any`.
//  "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",        // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code",              // Warn when dead code is identified.
  "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen",          // Warn when numerics are widened.
  "-Ywarn-unused:implicits",       // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",         // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",          // Warn if a local definition is unused.
  "-Ywarn-unused:params",          // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",        // Warn if a private member is unused.
  "-Ywarn-value-discard",          // Warn when non-Unit expression results are unused.
  "-Xsource:3"                     // use Scala 3 syntax
)
