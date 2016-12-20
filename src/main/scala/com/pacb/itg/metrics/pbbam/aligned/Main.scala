package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Paths

import falkner.jayson.metrics.io.JSON


object Main extends App {
    if (args.size != 2)
        println("Usage: java com.pacb.itg.metrics.pbbam.aligned.Main <blasr_aligned.bam> <output.json>")
    else
        JSON(Paths.get(args(1)), AlignedPacBioBam(Paths.get(args(0))))
}
