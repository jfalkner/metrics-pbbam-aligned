name := "PacBio Aligned Bam Metrics"

version in ThisBuild := "0.0.1"

organization in ThisBuild := "com.pacb"

scalaVersion in ThisBuild := "2.11.8"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:postfixOps")

parallelExecution in ThisBuild := false

fork in ThisBuild := true

// passed to JVM
javaOptions in ThisBuild += "-Xms256m"
javaOptions in ThisBuild += "-Xmx2g"

// `sbt run` will run this class
mainClass in (Compile, run) := Some("com.pacb.itg.metrics.pbbam.aligned.Main")

// `sbt pack` to make all JARs for a deploy. see https://github.com/xerial/sbt-pack
packSettings

libraryDependencies ++= {
  Seq(
    "org.specs2" %% "specs2-core" % "3.8.5" % "test",
    // Java API for working with BAM files
    "com.github.samtools" % "htsjdk" % "2.6.1"
  )
}

// allow code coverage via - https://github.com/scoverage/sbt-scoverage
//coverageExcludedPackages := "<empty>;.*Export.*AsCSV.*"

lazy val metrics = RootProject(uri("https://github.com/jfalkner/metrics.git#0.0.6"))

val main = Project(id = "application", base = file(".")).dependsOn(metrics)