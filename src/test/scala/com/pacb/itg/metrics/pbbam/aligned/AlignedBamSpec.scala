package com.pacb.itg.metrics.pbbam.aligned

import falkner.jayson.metrics.io.CSV
import org.specs2.mutable.Specification


class AlignedBamSpec extends Specification {

  "Aligned PB BAM Metrics" should {
    "Current version calculates without error" in {
      println(s"Current PB BAM Version: ${AlignedPacBioBam.currentVersion}")
      AlignedPacBioBam.currentVersion != null mustEqual true
    }
    "Support blank CSV generation" in {
      CSV(AlignedPacBioBam.blank).all != null mustEqual true
    }

  }
}
