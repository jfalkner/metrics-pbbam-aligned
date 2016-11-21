package com.pacb.itg.metrics.pbbam.aligned

//import java.nio.file.{Files, Paths}
//
//import com.pacb.itg.metrics.pbbam.aligned.AlignedPacBioBam_v1_5.AlignedPbRead
//import falkner.jayson.metrics.Distribution.{Categorical, Discrete}
//import falkner.jayson.metrics._
//import falkner.jayson.metrics.bam.aligned.AlignedBam_v1_4.{Cigar, Read}
//import falkner.jayson.metrics.bam.aligned.AlignedBam
//import falkner.jayson.metrics.io.{CSV, JSON}


/**
  * Writes out the PB BAM Aligned metrics to JSON and CSV
  */
object Main extends App {
//  println("\nPBBAM Alignment Export\n")
//  val pbaMetrics = AlignedPacBioBam(Paths.get(args.head))
//  val baMetrics = AlignedBam(Paths.get(args.head))
//  // CSV is flattened and won't have bins from distributions
//  println("Writing CSV")
//  CSV(Paths.get(args.last + ".csv"), Seq(baMetrics, pbaMetrics))
//  // JSON will have all data and is a superset of the CSV
//  println("Writing JSON")
//  JSON.write(Paths.get(args.last + ".json"), Seq(baMetrics, pbaMetrics))
//
//  class ReadMetric(name: String,
//                    zmw: Int,
//                   accuracy: Float,
//                   avgSnrA: Float, avgSnrC: Float, avgSnrG: Float, avgSnrT: Float,
//                   // base calls
//                   pulseCall: Categorical,
//                   // frame counts and related measurements
//                   ipdFrames: Discrete,
//                   pulseWidthFrames: Discrete,
//                   pkMid: Discrete,
//                   pkMean: Discrete,
//                   prePulseFrames: Discrete,
//                   pulseCallWidthFrames: Discrete,
//                   readLen: Int,
//                   refLen: Int,
//                   mappingQuality: Int,
//                   refAccClip: Float,
//                   refAccNoClip: Float,
//                   readAccClip: Float,
//                   readAccNoClip: Float,
//                   cigar: Cigar) extends Metrics {
//    override val namespace: String = "Read"
//    override val version: String = "_"
//    override val values: List[Metric] = List(
//      Str("Name", name),
//      Num("ZMW", zmw),
//      Num("Accuracy", accuracy),
//      Num("Avg SNR A", avgSnrA),
//      Num("Avg SNR C", avgSnrC),
//      Num("Avg SNR G", avgSnrG),
//      Num("Avg SNR T", avgSnrT),
//      CatDist("Pulse Call", pulseCall, pulseCall.bins.keySet.toList),
//      Dist("IPD Frames", ipdFrames),
//      Dist("pkMid", pkMid),
//      Dist("Pre-Pulse Frames", prePulseFrames),
//      Dist("Pulse Call Width Frames", pulseCallWidthFrames),
//      // aligned BAM read metrics
//      Num("Read Length", readLen),
//      Num("Ref Length",  refLen),
//      Num("Mapping Quality", mappingQuality),
//      Num("Ref Accuracy (Clip)", refAccClip),
//      Num("Ref Accuracy (No Clip)", refAccNoClip),
//      Num("Read Accuracy (Clip)", readAccClip),
//      Num("Read Accuracy (No Clip)", readAccNoClip),
//      Num("M", cigar.M),
//      Num("D", cigar.D),
//      Num("I", cigar.I),
//      Num("N", cigar.N),
//      Num("S", cigar.S),
//      Num("H", cigar.H),
//      Num("P", cigar.P),
//      Num("=", cigar.EQ),
//      Num("X", cigar.X)
//    )
//  }
//
//  // map all the reads
//  val apbr = pbaMetrics.reads.map(r => (r.name, r)).toMap
//  val abr = baMetrics.reads.map(r => (r.name, r)).toMap
//
//  // all keys, since the maps share the same keys already
//  val keys = apbr.keys.toList
//
//
//  // write out all the reads too
//  val bw = Files.newBufferedWriter(Paths.get(args.last +".reads.csv"))
//
//  def write(r: Read, pr: AlignedPbRead): ReadMetric = new ReadMetric(
//    pr.name,
//    pr.zmw,
//    pr.accuracy,
//    pr.avgSnrA,
//    pr.avgSnrC,
//    pr.avgSnrG,
//    pr.avgSnrT,
//    pr.pulseCall,
//    pr.ipdFrames,
//    pr.pulseWidthFrames,
//    pr.pkMid,
//    pr.pkMean,
//    pr.prePulseFrames,
//    pr.pulseCallWidthFrames,
//    // aligned BAM metrics
//    r.len,
//    r.refLen,
//    r.mappingQuality,
//    // accuracies
//    r.refAccClip,
//    r.refAccNoClip,
//    r.readAccClip,
//    r.readAccNoClip,
//    r.cigar
//  )
//
//  // dump ZMWs
//  println("Writing ZMWs")
//  Seq(keys.head).map(k => bw.write(CSV(write(abr(k), apbr(k))).all + "\n"))
//  keys.tail.foreach(k => bw.write(CSV(write(abr(k), apbr(k))).values + "\n"))
//  bw.flush()
//  bw.close()
}
