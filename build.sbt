name := "itg_metrics_pbbam_aligned"

version in ThisBuild := "0.0.4"

organization in ThisBuild := "com.pacb"

scalaVersion in ThisBuild := "2.11.8"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:postfixOps")

// `sbt pack` to make all JARs for a deploy. see https://github.com/xerial/sbt-pack
packSettings

libraryDependencies ++= {
  Seq(
    "org.specs2" %% "specs2-core" % "3.8.5" % "test",
    // Java API for working with BAM files
    "com.github.samtools" % "htsjdk" % "2.6.1"
  )
}

lazy val metrics = RootProject(uri("https://github.com/jfalkner/metrics.git#0.2.2"))
//lazy val metrics = RootProject(file("/Users/jfalkner/tokeep/git/jfalkner/metrics"))

val main = Project(id = "itg_metrics_pbbam_aligned", base = file(".")).dependsOn(metrics)
