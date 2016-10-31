package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Path

import falkner.jayson.metrics.Distribution._
import falkner.jayson.metrics._
import htsjdk.samtools._

import scala.collection.immutable.ListMap
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._
import scala.collection.mutable
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
    //CatDist("Del Quality", makeCategorical(sumQual((r) => r.delQV)), qualKeys),
    //CatDist("Del Tag", makeCategorical(mergeCategorical((r) => r.delTag))),
    //CatDist("Ins Quality", makeCategorical(sumQual((r) => r.insQV)), qualKeys),
    //CatDist("Merge Quality", makeCategorical(sumQual((r) => r.mergeQV)), qualKeys),
    //CatDist("Substitution Quality", makeCategorical(sumQual((r) => r.subQV)), qualKeys),
    //CatDist("Substitution Tag", makeCategorical(mergeCategorical((r) => r.subTag))),
    //CatDist("Label Quality", makeCategorical(sumQual((r) => r.labelQV)), qualKeys),
    //CatDist("Alt Label", makeCategorical(mergeCategorical((r) => r.altLabel))),
    //CatDist("Alt Label Qual", makeCategorical(sumQual((r) => r.labelQV)), qualKeys),
    //CatDist("Pulse Merge Qual", makeCategorical(sumQual((r) => r.pulseMergeQV)), qualKeys),
    CatDist("Pulse Call", makeCategorical(mergeCategorical((r) => r.pulseCall)), callKeys), // true categorical
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

  val callKeys = List("A", "C", "G", "T")
  val qualKeys = ('!' to '~').map(_.toString).toList

  // stashes the per-read metrics (probably want to export these too sometime?)
  case class PbRead(zmw: Int,
                    accuracy: Float,
                    avgSnrA: Float, avgSnrC: Float, avgSnrG: Float, avgSnrT: Float,
                    // char-base 33+ascii values for quality
                    //delQV: Array[Int],
                    //delTag: Categorical,
                    //insQV: Array[Int],
                    //mergeQV: Array[Int],
                    //subQV: Array[Int],
                    //subTag: Categorical,
                    //labelQV: Array[Int],
                    //altLabel: Categorical,
                    //altLabelQV: Array[Int],
                    //pulseMergeQV: Array[Int],
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
    case Success(s) =>
      s
    case Failure(t) if p == null => (null, null) // support AlignedPacBioBam.blank
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
      //mapQv(rm("dq").asInstanceOf[String]), // DeletionQV=dq
      //calcCategorical(Map(rm("dt").asInstanceOf[String].toCharArray.toSeq.groupBy(_.toString).toList: _ *), qualKeys), // DeletionTag=dt
      //mapQv(rm("iq").asInstanceOf[String]), // InsertionQV=iq
      //mapQv(rm("mq").asInstanceOf[String]), // MergeQV=mq
      //mapQv(rm("sq").asInstanceOf[String]), // SubstitutionQV=sq
      //calcCategorical(Map(rm("st").asInstanceOf[String].toCharArray.toSeq.groupBy(_.toString).toList: _ *), qualKeys), // SubstitutionTag=st
      //mapQv(rm("pq").asInstanceOf[String]), // LabelQV
      //calcCategorical(Map(rm("pt").asInstanceOf[String].toCharArray.toSeq.groupBy(_.toString).toList: _ *), qualKeys), // AltLabel
      //mapQv(rm("pv").asInstanceOf[String]), // AltLabelQV
      //mapQv(rm("pg").asInstanceOf[String]), // PulseMergeQV
      calcCategorical(Map(rm("pc").asInstanceOf[String].toCharArray.toSeq.groupBy(_.toString).toList: _ *)), // PulseCall
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
  def mergeCategorical(f: (PbRead) => Categorical): ListMap[String, Int] = {
    val dists = reads.map(r => f(r))
    val m = mutable.ListMap[String, Int]()
    dists.foreach(d => d.bins.foreach{ case (k, v) => m.update(k, m.getOrElse(k,  0) + (v match { case i: Int => i })) })
    m.asInstanceOf[ListMap[String, Int]]
    //ListMap((0 to ('~'.toShort - '!'.toShort)).map(_ + 33).map(_.toChar.toString).map(k => (k, reads.flatMap(r => f(r).bins.get(k)).map(_.asInstanceOf[Int]).sum)): _ *)
  }

  // char-base 33+ascii values for quality
  def sumQual(f: (PbRead) => Array[Int]): ListMap[String, Int] = {
    // this loops
    //ListMap((0 to ('~'.toShort - '!'.toShort)).map(_ + 33).map(_.toChar.toString).map(k => (k, reads.flatMap(r => f(r).bins.get(k)).map(_.asInstanceOf[Int]).sum)): _ *)
    val dists = reads.map(r => f(r))
    val a =  new Array[Int]('~' - '!')
    ('~' to '!').map(_ - '!').foreach(i => a(i) = dists.map(d => d(i)).sum)
    ListMap(('~' to '!').map(c => (c.toString, a(c - '!'))) :_ *)
  }

  def mapQv(qv: String): Array[Int] = {
    val a = new Array[Int]('~' - '!')
    qv.foreach(c => a(c - '!') += 1)
    a
    //ListMap(('!' to '~').map(k => (k.toString, a(k-'!'))) :_ *)
  }


  // make a giant histogram that summarizes all the per-read ones. gives more insight than mean/median
  def mergeIntDist(f: (PbRead) => Discrete, nBins: Int = 30): Discrete = {
    val dists = reads.map(r => f(r)) // dist of interest for all reads
    val min = dists.map(_.min).min
    val max = dists.map(_.max).max
    val binWidth = Math.max((max - min) / nBins, 1)
    // make one array to populate
    val a = new Array[Int](nBins)
    // convert all dists from (binIndex, count) => (value, count). So all values across all dist are in the same units
    dists.foreach(
      d => d.bins.zipWithIndex
        .map{ case (v, i) => (d.min + (i * d.binWidth), v) }
        .foreach{ case (v, count) => a(((v - min)/ binWidth)) += count }
    )
    val samples = a.sum
    val mean = a.zipWithIndex.map{ case (v, i) =>  (min + (i * binWidth)) * v }.sum.toFloat / samples
    val median = -1 // TODO: estimate later by using hist
    Discrete(samples, nBins, binWidth, mean, median, min, max, a)
  }
}