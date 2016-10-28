package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Path

import falkner.jayson.metrics.Distribution._
import falkner.jayson.metrics._
import htsjdk.samtools._

import scala.collection.immutable.ListMap
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


object AlignedPacBioBam_1_5 {
  val version = "1.5"
}

/**
  * Exports PacBio specific info and metrics via single-pass through a BAM file
  *
  * See README.md for details about each metric. If you haven't read the docs, the majority of this information comes from
  * PacBioFileFormats 3.0 documentation: "BAM format specification for PacBio".
  *
  * http://pacbiofileformats.readthedocs.io/en/3.0/BAM.html
  */
class AlignedPacBioBam_1_5(p: Path, nBins: Int = 30) extends Metrics {
  override val namespace = "PBBAM"
  override val version = s"${AlignedPacBioBam.version}~${AlignedPacBioBam_1_5.version}"
  override lazy val values: List[Metric] = List(
    // PacBio-specific header BAM info
    Str("BLASR Version", pr("BLASR").getProgramVersion),
    Str("BLASR Command Line", pr("BLASR").getCommandLine),
    Str("baz2bam Version", pr("baz2bam").getProgramVersion),
    Str("baz2bam Command Line", pr("baz2bam").getCommandLine),
    Str("bazformat Version", pr("bazformat").getProgramVersion),
    Str("bazwriter", pr("bazwriter").getProgramVersion),
    // PacBio-specific instrument and movie context
    Str("Instrument Model", rg.getPlatformModel),
    Str("Movie", rg.getPlatformUnit),
    // PacBio-specific meta-info stashed in the Read Group's Description field
    Str("Binding Kit", rgm("BINDINGKIT")),
    Str("Sequencing Kit", rgm("SEQUENCINGKIT")),
    Str("Base Caller Version", rgm("BASECALLERVERSION")),
    Num("Frame Rate", rgm("FRAMERATEHZ")),
    // all of the read-based distributions. many mean of means here
    DistCon("Accuracy", calcContinuous(reads.map(_.accuracy))),
    DistCon("Mean of SnR A Mean", calcContinuous(reads.map(_.avgSnrA))),
    DistCon("Mean of SnR C Mean", calcContinuous(reads.map(_.avgSnrC))),
    DistCon("Mean of SnR G Mean", calcContinuous(reads.map(_.avgSnrG))),
    DistCon("Mean of SnR T Mean", calcContinuous(reads.map(_.avgSnrT))),
    // TODO: most of these quality dist are in a non-linear scale, not plain categories
    CatDist("Del Quality", makeCategorical(sumQual((r) => r.delQV))),
    CatDist("Del Tag", makeCategorical(sumQual((r) => r.delTag))),
    CatDist("Ins Quality", makeCategorical(sumQual((r) => r.insQV))),
    CatDist("Merge Quality", makeCategorical(sumQual((r) => r.mergeQV))),
    CatDist("Substitution Quality", makeCategorical(sumQual((r) => r.subQV))),
    CatDist("Substitution Tag", makeCategorical(sumQual((r) => r.subTag))),
    CatDist("Label Quality", makeCategorical(sumQual((r) => r.labelQV))),
    CatDist("Alt Label", makeCategorical(sumQual((r) => r.altLabel))),
    CatDist("Alt Label Qual", makeCategorical(sumQual((r) => r.labelQV))),
    CatDist("Pulse Merge Qual", makeCategorical(sumQual((r) => r.pulseMergeQV))),
    CatDist("Pulse Call", makeCategorical(sumQual((r) => r.pulseCall))), // true categorical
    // summaries of per-read distributions. TOOD: aggregate across all distributions?
    DistCon("Mean of IPD Frames Mean", calcContinuous(reads.map(_.ipdFrames.mean))),
    Dist("Mean of IPD Frames Median", calcDiscrete(reads.map(_.ipdFrames.median))),
    DistCon("Mean of Pulse Width Frames Mean", calcContinuous(reads.map(_.pulseWidthFrames.mean))),
    Dist("Median of Pulse Width Frames Median", calcDiscrete(reads.map(_.pulseWidthFrames.median))),
    DistCon("Mean of pkMid", calcContinuous(reads.map(_.pkMid.mean))),
    Dist("Median of pkMid", calcDiscrete(reads.map(_.pkMid.median))),
    DistCon("Mean of pkMean", calcContinuous(reads.map(_.pkMean.mean))),
    Dist("Median of pkMean", calcDiscrete(reads.map(_.pkMean.median))),
    DistCon("Mean of Pre-Pulse Frames", calcContinuous(reads.map(_.prePulseFrames.mean))),
    Dist("Median of Pre-Pulse Frames", calcDiscrete(reads.map(_.prePulseFrames.median))),
    DistCon("Mean of Pulse Call Width Frames Mean", calcContinuous(reads.map(_.pulseCallWidthFrames.mean))),
    Dist("Median of Pulse Call Width Frames Median", calcDiscrete(reads.map(_.pulseCallWidthFrames.median))),
    // merged distributions from above
    Dist("IPD Frames", mergeIntDist((r) => r.pulseWidthFrames)),
    Dist("Pulse Width Frames", mergeIntDist((r) => r.pulseWidthFrames)),
    Dist("PkMean Width Frames", mergeIntDist((r) => r.pkMean)),
    Dist("PkMid Width Frames", mergeIntDist((r) => r.pkMid)),
    Dist("Pre-Pulse Frames", mergeIntDist((r) => r.prePulseFrames))
  )

  // stashes the per-read metrics (probably want to export these too sometime?)
  case class PbRead(zmw: Int,
                    accuracy: Float,
                    avgSnrA: Float, avgSnrC: Float, avgSnrG: Float, avgSnrT: Float,
                    // char-base 33+ascii values for quality
                    delQV: Categorical,
                    delTag: Categorical,
                    insQV: Categorical,
                    mergeQV: Categorical,
                    subQV: Categorical,
                    subTag: Categorical,
                    labelQV: Categorical,
                    altLabel: Categorical,
                    altLabelQV: Categorical,
                    pulseMergeQV: Categorical,
                    // base calls
                    pulseCall: Categorical,
                    // frame counts and related measurements
                    ipdFrames: Discrete,
                    pulseWidthFrames: Discrete,
                    pkMid: Discrete,
                    pkMean: Discrete,
                    prePulseFrames: Discrete,
                    pulseCallWidthFrames: Discrete)

  lazy val (header, reads): (SAMFileHeader, List[PbRead]) = Try {
    val factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT)
    val bam = factory.open(p)

    // REMOVE after debugging
    //val i = bam.iterator.asScala
    //(bam.getFileHeader, List((0 to 100000).map(v => i.next).map(i => Future(parse(i))) :_ *).map(r => Await.result(r, Duration.Inf)))

    (bam.getFileHeader, (for (r <- bam.iterator.asScala) yield Future(parse(r))).toList.map(r => Await.result(r, Duration.Inf)))
  } match {
    case Success(s) => s
    case Failure(_) if p == null => (null, null) // support AlignedPacBioBam.blank
    case Failure(t) => throw t
  }

  def parse(r: SAMRecord): PbRead = {
    val rm = r.getAttributes.asScala.map(tv => (tv.tag, tv.value)).toMap
    PbRead(
      rm("zm").asInstanceOf[Int],
      rm("rq").asInstanceOf[Float],
      // SNR avg per base
      rm("sn").asInstanceOf[Array[Float]](0),
      rm("sn").asInstanceOf[Array[Float]](1),
      rm("sn").asInstanceOf[Array[Float]](2),
      rm("sn").asInstanceOf[Array[Float]](3),
      // println("ZMW Hole Number: "+rm("zm")) // skip, no summary stat here
      // distributions
      calcCategorical(ListMap(rm("dq").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // DeletionQC=dq
      calcCategorical(ListMap(rm("dt").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // DeletionTag=dt
      calcCategorical(ListMap(rm("iq").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // InsertionQV=iq
      calcCategorical(ListMap(rm("mq").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // MergeQV=mq
      calcCategorical(ListMap(rm("sq").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // SubstitutionQV=sq
      calcCategorical(ListMap(rm("st").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // SubstitutionTag=st
      calcCategorical(ListMap(rm("pq").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // LabelQV
      calcCategorical(ListMap(rm("pt").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // AltLabel
      calcCategorical(ListMap(rm("pv").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // AltLabelQV
      calcCategorical(ListMap(rm("pg").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // PulseMergeQV
      calcCategorical(ListMap(rm("pc").toString.toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // PulseCall
      calcShort(rm("ip").asInstanceOf[Array[Short]]),
      calcShort(rm("pw").asInstanceOf[Array[Short]]),
      calcShort(rm("pm").asInstanceOf[Array[Short]]),
      calcShort(rm("pa").asInstanceOf[Array[Short]]),
      calcShort(rm("pd").asInstanceOf[Array[Short]]),
      calcShort(rm("px").asInstanceOf[Array[Short]])
    )
  }

  lazy val rg = header.getReadGroups.asScala.head
  lazy val rgm = rg.getDescription.split(";").map(_.split("=")).map(kv => (kv(0), kv(1))).toMap

  private def pr(key: String) : SAMProgramRecord = header.getProgramRecords.asScala.filter(_.getProgramName == key).head

  // char-base 33+ascii values for quality
  def sumQual(f: (PbRead) => Categorical): ListMap[String, Int] = {
    ListMap((0 to ('~'.toShort - '!'.toShort)).map(_ + 33).map(_.toChar.toString).map(k => (k, reads.flatMap(r => f(r).bins.get(k)).map(_.asInstanceOf[Int]).sum)): _ *)
  }

  // make a giant histogram that summarizes all the per-read ones. gives more insight than mean/median
  def mergeIntDist(f: (PbRead) => Discrete, nBins: Int = 30): Discrete = {
    val min = reads.map(r => f(r).min).min
    val max = reads.map(r => f(r).max).max
    val binWidth = Math.max((max - min) / nBins, 1)
    // convert all dists from (binIndex, count) => (value, count). So all values across all dist are in the same units
    val indexVal = reads.flatMap(r => Seq(f(r)).flatMap(d => d.bins.zipWithIndex.map{ case (v, i) => (d.min + (i * d.binWidth), v)}))
    // sum all counts to a new set of bins that uses the min/max of all dists and all values
    val bins = indexVal.map{ case (v, count) => ((((v - min)/ binWidth).toInt), count)}.groupBy(_._1).map{ case (k, v) => (k, v.map(_._2).sum)}
    val samples = indexVal.map(_._2).sum
    Discrete(
      samples,
      nBins,
      binWidth,
      indexVal.map{ case (v, c) => v * c}.sum.toFloat / samples,
      -1, // TODO: estimate later by using hist
      min,
      max,
      for ( i <- 0 to (nBins - 1))
        yield if (i < nBins - 1) bins.getOrElse(i, 0) else bins.getOrElse(i, 0) + bins.getOrElse(i + 1, 0)
    )
  }
}