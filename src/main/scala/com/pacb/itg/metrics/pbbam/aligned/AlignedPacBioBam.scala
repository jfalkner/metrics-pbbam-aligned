package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Path


object AlignedPacBioBam {

  // version of the overall com.pacb.itg.metrics.pbbam.aligned package. should match build.sbt
  val version = "0.0.1"

  lazy val blank = AlignedPacBioBam(null)

  lazy val currentVersion = blank.version

  // placeholder to support other versions down the road
  def apply(p: Path): AlignedPacBioBam_1_5 = new AlignedPacBioBam_1_5(p)
}

class AlignedPacBioBam(p: Path) extends AlignedPacBioBam_1_5(p)