package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Paths

import falkner.jayson.metrics.io.{CSV, JSON}


/**
  * Writes out the PB BAM Aligned metrics to JSON and CSV
  */
object Main extends App {
  val metrics = AlignedPacBioBam(Paths.get(args.head))
  // CSV is flattened and won't have bins from distributions
  CSV(Paths.get(args.last), metrics)
  // JSON will have all data and is a superset of the CSV
  JSON.write(Paths.get(args.last), metrics)
}
