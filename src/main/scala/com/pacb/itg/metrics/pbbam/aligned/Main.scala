package com.pacb.itg.metrics.pbbam.aligned

import falkner.jayson.metrics.Distribution._
import java.nio.file.Paths

import falkner.jayson.metrics.io.{CSV, JSON}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Reads an aligned BAM and produces metrics in either CSV or JSON format
  */
object Main extends App {

//  val ab = AlignedBam.calc(Paths.get("/Users/jfalkner/tokeep/bitbucket/itg/metrics-bam-aligned/example.bam")).toList.map(r => Await.result(r, 3 seconds))
  val ab = AlignedPacBioBam(Paths.get("/Users/jfalkner/tokeep/bitbucket/itg/metrics-bam-aligned/example.bam"))
  JSON.write(Paths.get("example.json"), ab)
  CSV(Paths.get("example.csv"), ab)

//  // write them all to disk
//
//  // dump 10 reads
//  reads.take(10).foreach(println)
//
//  // calc accuracy distributions
//  val nBins = 30
//
//  // read length
//  val readLength = calcDiscreteDist(reads.map(_.len).sorted)
//  print("Read Length", readLength)
//
//  // quality
//  val mappingQuality = calcContinuousDist(reads.map(_.mappingQuality).sorted)
//  print("Mapping Quality", mappingQuality)
//
//  // accuracies
//  val refAccG = calcContinuousDist(reads.map(_.refAccG).sorted)
//  print("Reference Accuracy (Genomic)", refAccG)
//  val refAccT = calcContinuousDist(reads.map(_.refAccT).sorted)
//  print("Reference Accuracy (Transcript)", refAccT)
//  val readAccG = calcContinuousDist(reads.map(_.readAccG).sorted)
//  print("Read Accuracy (Genomic)", readAccG)
//  val readAccT = calcContinuousDist(reads.map(_.readAccT).sorted)
//  print("Read Accuracy (Transcript)", readAccT)
//
//  // mutation frequencies
//  val matchOrMismatchFrequency = calcContinuousDist(reads.map(v => v.cigar.M.toFloat / v.len).sorted)
//  printPercent("Match or Mis-Match Frequency (Read)", matchOrMismatchFrequency)
//  val delFrequency = calcContinuousDist(reads.map(v => v.cigar.D.toFloat / v.len).sorted)
//  printPercent("Del Frequency (Read)", delFrequency)
//  val insFrequency = calcContinuousDist(reads.map(v => v.cigar.I.toFloat / v.len).sorted)
//  printPercent("Insert Frequency (Read)", insFrequency)
//  val skipFrequency = calcContinuousDist(reads.map(v => v.cigar.N.toFloat / v.len).sorted)
//  printPercent("Skip Frequency (Read)", skipFrequency)
//  val softClipFrequency = calcContinuousDist(reads.map(v => v.cigar.S.toFloat / v.len).sorted)
//  printPercent("Soft Clip Frequency (Read)", softClipFrequency)
//  val hardClipFrequency = calcContinuousDist(reads.map(v => v.cigar.H.toFloat / v.len).sorted)
//  printPercent("Hard Clip Frequency (Read)", hardClipFrequency)
//  val mismatchFrequency = calcContinuousDist(reads.map(v => v.cigar.X.toFloat / v.len).sorted)
//  printPercent("Mismatch Frequency (Read)", mismatchFrequency)
}
