package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.{Files, Path}

import falkner.jayson.metrics.Distribution._
import falkner.jayson.metrics.io.CSV
import falkner.jayson.metrics.{Dist, _}
import htsjdk.samtools._

import scala.collection.immutable.ListMap
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}


object AlignedPacBioBam_v1_5 {
  val version = "1.5"

  def apply(p: Path): AlignedPacBioBam_v1_5 = new AlignedPacBioBam_v1_5(p)

  // just for cigar dist
  val bases = Seq("A", "C", "T", "G")
  val cigarOps = Seq("M", "I", "D", "N", "S", "H", "P", "=", "X")

  class CigarDists(ns: String) {

    val dists: Map[Byte, Map[String, mutable.ArrayBuffer[Short]]] =
      Map(bases.map(b => (b.charAt(0).toByte, Map(cigarOps.map(c => (c, mutable.ArrayBuffer[Short]())): _*))): _*)

    def flatten(): Seq[NamedDiscrete] =
      bases.flatMap(b => cigarOps.map(co => NamedDiscrete(s"$ns: $b: $co", calcShort(dists(b.charAt(0).toByte)(co).toArray))))

  }

  case class NamedDiscrete(name: String, dist: Discrete)

  // stashes the per-read metrics (probably want to export these too sometime?)
  case class Read(name: String,
                  zmw: Int,
                  mappingQuality: Int,
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
                  ipdFrames: Seq[NamedDiscrete], // Discrete,
                  pulseWidthFrames: Seq[NamedDiscrete], // Discrete,
                  pkMid: Seq[NamedDiscrete], // Discrete,
                  pkMean: Seq[NamedDiscrete], // Discrete,
                  prePulseFrames: Seq[NamedDiscrete], // Discrete,
                  pulseCallWidthFrames: Seq[NamedDiscrete]) // Discrete)

  class ReadMetric(r: Read) extends Metrics {
    override val namespace: String = "Read"
    override val version: String = "_"
    override val values: List[Metric] = List(
      Str("Name", r.name),
      Num("ZMW", r.zmw),
      Num("Accuracy", r.accuracy),
      Num("Avg SNR A", r.avgSnrA),
      Num("Avg SNR C", r.avgSnrC),
      Num("Avg SNR G", r.avgSnrG),
      Num("Avg SNR T", r.avgSnrT),
      CatDist("Pulse Call", r.pulseCall, r.pulseCall.bins.keySet.toList)
    ) ++ Seq(r.ipdFrames, r.pulseWidthFrames, r.pkMid, r.pkMean, r.prePulseFrames, r.pulseCallWidthFrames).flatMap(
      _.map(nd => Dist(nd.name, nd.dist)))
  }

  def exportReads(reads: Seq[Read], p: Path): Unit = {
    // TODO: auto-delete on error?
    val bw = Files.newBufferedWriter(p)
    Seq(reads.head).map(r => bw.write(CSV(new ReadMetric(r)).all + "\n"))
    reads.tail.foreach(r => bw.write(CSV(new ReadMetric(r)).values + "\n"))
    bw.flush()
    bw.close()
  }

  case class Chunk(size: Int,
                  // ZMW
                  zmws: Set[Int],
                  // SNR
                  avgSnrA: Continuous,
                   avgSnrC: Continuous,
                   avgSnrG: Continuous,
                   avgSnrT: Continuous,
                  // Calls
                  pulseCalls: Categorical,
                  // per-base metrics per read
                  ipdFrames: Seq[NamedDiscrete],
                  pulseWidthFrames: Seq[NamedDiscrete],
                  pkMid: Seq[NamedDiscrete],
                  pkMean: Seq[NamedDiscrete],
                  prePulseFrames: Seq[NamedDiscrete],
                  pulseCallWidthFrames: Seq[NamedDiscrete])

  val (snrMin, snrMax) = (0, 30)
  val (ipdFramesMin, ipdFramesMax) = (0, 210)
  val (pulseWidthFramesMin, pulseWidthFramesMax) = (0, 60)
  val (pkMidMin, pkMidMax) = (0, 12000)
  val (pkMeanMin, pkMeanMax) = (0, 12000)
  val (prePulseFramesMin, prePulseFramesMax) = (0, 210)
  val (pulseCallWidthFramesMin, pulseCallWidthFramesMax) = (0, 60)
}

/**
  * Exports PacBio specific info and metrics via single-pass through a BAM file
  *
  * See README.md for details about each metric. If you haven't read the docs, the majority of this information comes from
  * PacBioFileFormats 3.0 documentation: "BAM format specification for PacBio".
  *
  * http://pacbiofileformats.readthedocs.io/en/3.0/BAM.html
  */
class AlignedPacBioBam_v1_5(p: Path, nBins: Int = 30) extends Metrics {
  import AlignedPacBioBam_v1_5._
  override val namespace = "PBBAM"
  override val version = s"${AlignedPacBioBam.version}~${AlignedPacBioBam_v1_5.version}"
  override val values: List[Metric] = List(
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
    Num("Unique ZMWs", chunks.flatMap(_.zmws).toSet.size), // number of unique ZMWs
    // all of the read-based distributions. many mean of means here
//    DistCon("Accuracy", calcContinuous(reads.map(_.accuracy))),
    //Dist("Mapping Quality", calcContinuous(reads.map(_.mappingQuality))),
    DistCon("SnR A Mean", mergeContinuous(chunks.map(_.avgSnrA), forceMin=Some(snrMin), forceMax=Some(snrMax))),
    DistCon("SnR C Mean", mergeContinuous(chunks.map(_.avgSnrC), forceMin=Some(snrMin), forceMax=Some(snrMax))),
    DistCon("SnR G Mean", mergeContinuous(chunks.map(_.avgSnrG), forceMin=Some(snrMin), forceMax=Some(snrMax))),
    DistCon("SnR T Mean", mergeContinuous(chunks.map(_.avgSnrT), forceMin=Some(snrMin), forceMax=Some(snrMax))),
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
    CatDist("Pulse Call", makeCategorical(mergeCategorical(chunks.map(_.pulseCalls))), callKeys) // true categorical
  ) ++
  ipdFramesAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(ipdFramesAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(ipdFramesMin), forceMax=Some(ipdFramesMax)))) ++
  pulseWidthFramesAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(pulseWidthFramesAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(pulseWidthFramesMin), forceMax=Some(pulseWidthFramesMax)))) ++
  pkMidAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(pkMidAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(pkMidMin), forceMax=Some(pkMidMax)))) ++
  pkMeanAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(pkMeanAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(pkMeanMin), forceMax=Some(pkMeanMax)))) ++
  prePulseFramesAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(prePulseFramesAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(prePulseFramesMin), forceMax=Some(prePulseFramesMax)))) ++
  pulseCallWidthFramesAgg.head.map(_.name).map(n => Dist(n, mergeIntDist(pulseCallWidthFramesAgg.map(_.filter(_.name == n).head.dist), forceMin=Some(pulseCallWidthFramesMin), forceMax=Some(pulseCallWidthFramesMax))))

  lazy val callKeys = List("A", "C", "G", "T")
  lazy val qualKeys = ('!' to '~').map(_.toString).toList

  // for updating
  lazy val ipdFramesAgg = chunks.map(_.ipdFrames)
  lazy val pulseWidthFramesAgg = chunks.map(_.pulseWidthFrames)
  lazy val pkMidAgg = chunks.map(_.pkMid)
  lazy val pkMeanAgg = chunks.map(_.pkMean)
  lazy val prePulseFramesAgg = chunks.map(_.prePulseFrames)
  lazy val pulseCallWidthFramesAgg = chunks.map(_.pulseCallWidthFrames)

  // can't assume enough mem to buffer everything in-mem. handle chunks here
  def handleReads(buf: Seq[Read]): Chunk = Chunk(
    buf.size,
    // unique sequencing ZMWs
    buf.map(_.zmw).toSet,
    // SNR means
    calcContinuous(buf.map(_.avgSnrA), forceMin=Some(snrMin), forceMax=Some(snrMax)),
    calcContinuous(buf.map(_.avgSnrC), forceMin=Some(snrMin), forceMax=Some(snrMax)),
    calcContinuous(buf.map(_.avgSnrG), forceMin=Some(snrMin), forceMax=Some(snrMax)),
    calcContinuous(buf.map(_.avgSnrT), forceMin=Some(snrMin), forceMax=Some(snrMax)),
    // pulse calls
    makeCategorical(mergeCat(buf, (r) => r.pulseCall)),
    // all the per-read metrics
    buf.head.ipdFrames.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.ipdFrames.filter(_.name == n).head.dist, ipdFramesMin, ipdFramesMax))),
    buf.head.pulseWidthFrames.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.pulseWidthFrames.filter(_.name == n).head.dist, pulseWidthFramesMin, pulseWidthFramesMax))),
    buf.head.pkMid.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.pkMid.filter(_.name == n).head.dist, pkMidMin, pkMidMax))),
    buf.head.pkMean.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.pkMean.filter(_.name == n).head.dist, pkMeanMin, pkMeanMax))),
    buf.head.prePulseFrames.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.prePulseFrames.filter(_.name == n).head.dist, prePulseFramesMin, prePulseFramesMax))),
    buf.head.pulseCallWidthFrames.map(_.name).map(n => NamedDiscrete(n, mergeDisc(buf, (r) => r.pulseCallWidthFrames.filter(_.name == n).head.dist, pulseCallWidthFramesMin, pulseCallWidthFramesMax)))
  )

  lazy val chunkSize = 10000

  lazy val (header, chunks): (SAMFileHeader, List[Chunk]) = Try {
    val factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT)
    val bam = factory.open(p)
    
    (bam.getFileHeader, bam.iterator.asScala.grouped(chunkSize).map(g => Future(g.map(parse))).map(c =>
      handleReads(Await.result(c, Duration.Inf))).toList)
  } match {
    case Success(s) =>
      s
    case Failure(t) if p == null => (null, null) // support AlignedPacBioBam.blank
    case Failure(t) => throw t
  }

  def parse(r: SAMRecord): Read = {
    val rm = r.getAttributes.asScala.map(tv => (tv.tag, tv.value)).toMap
    // PacBio specific metrics passed by Primary
    val ipdVals = rm("ip").asInstanceOf[Array[Short]]
    val pulseWidthVals = rm("pw").asInstanceOf[Array[Short]]
    val pkMidVals = rm("pm").asInstanceOf[Array[Short]]
    val pkMeanVals = rm("pa").asInstanceOf[Array[Short]]
    val prePulseVals = rm("pd").asInstanceOf[Array[Short]]
    val pulseCallVals = rm("px").asInstanceOf[Array[Short]]
    val readBases = r.getReadBases
    // map the PB stats by base called -- these are unique to an aligned reference, not in Primary's sts.xml export
    val ipdFrames = new CigarDists("IPD Frames")
    val pulseWidthFrames = new CigarDists("Pulse Width Frames")
    val pkMid = new CigarDists("PkMid")
    val pkMean = new CigarDists("PkMean")
    val prePulseFrames = new CigarDists("Pre-Pulse Frames")
    val pulseCallWidthFrames = new CigarDists("Pulse Call Width Frames")
    // map the cigar elements to the reads
    //val cigarCount = r.getCigar.getCigarElements.asScala.map(_.getLength).sum
    //val cigarDels = r.getCigar.getCigarElements.asScala.filter(_.getOperator.toString != "D").map(_.getLength).sum
    var readIndex = 0
    for ((c, i) <- r.getCigar.getCigarElements.asScala.zipWithIndex) {
      val co = c.getOperator.toString
      for (j <- 1 to c.getLength) {
        val b = readBases(readIndex) // calc one-time
        ipdFrames.dists(b)(co).append(ipdVals(readIndex))
        pulseWidthFrames.dists(b)(co).append(pulseWidthVals(readIndex))
        pkMid.dists(b)(co).append(pkMidVals(readIndex))
        pkMean.dists(b)(co).append(pkMeanVals(readIndex))
        prePulseFrames.dists(b)(co).append(prePulseVals(readIndex))
        pulseCallWidthFrames.dists(b)(co).append(pulseCallVals(readIndex))
        // increment offset, except for deletes and skips -- they don't use a read base
        if (co != "D" && co != "N") readIndex += 1
      }
    }
    Read(
      r.getReadName,
      rm("zm").asInstanceOf[Int],
      r.getMappingQuality,
      rm("rq").asInstanceOf[Float],
      // SNR avg per base
      rm("sn").asInstanceOf[Array[Float]](0),
      rm("sn").asInstanceOf[Array[Float]](1),
      rm("sn").asInstanceOf[Array[Float]](2),
      rm("sn").asInstanceOf[Array[Float]](3),
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
      ipdFrames.flatten,
      pulseWidthFrames.flatten,
      pkMid.flatten,
      pkMean.flatten,
      prePulseFrames.flatten,
      pulseCallWidthFrames.flatten
    )
  }

  lazy val rg = header.getReadGroups.asScala.head
  lazy val rgm = rg.getDescription.split(";").map(_.split("=")).map(kv => (kv(0), kv(1))).toMap

  private def pr(key: String) : SAMProgramRecord = header.getProgramRecords.asScala.filter(_.getProgramName == key).head

  // char-base 33+ascii values for quality
  def mergeCat(reads: Seq[Read], f: (Read) => Categorical): Map[String, Int] = {
    mergeCategorical(reads.map(r => f(r)))
  }

  // char-base 33+ascii values for quality
  def sumQual(reads: Seq[Read], f: (Read) => Array[Int]): ListMap[String, Int] = {
    val dists = reads.map(r => f(r))
    val a =  new Array[Int]('~' - '!')
    ('~' to '!').map(_ - '!').foreach(i => a(i) = dists.map(d => d(i)).sum)
    ListMap(('~' to '!').map(c => (c.toString, a(c - '!'))) :_ *)
  }

  def mapQv(qv: String): Array[Int] = {
    val a = new Array[Int]('~' - '!')
    qv.foreach(c => a(c - '!') += 1)
    a
  }

  // make a giant histogram that summarizes all the per-read ones. gives more insight than mean/median
  def mergeDisc(buf: Seq[Read], f: (Read) => Discrete, min: Int, max: Int): Discrete = {
    mergeIntDist(buf.map(r => f(r)).filter(_.sampleNum > 0), forceMin=Some(min), forceMax=Some(max)) // dist of interest for all reads
  }
}