package com.pacb.itg.metrics.pbbam.aligned

import java.nio.file.Paths

import falkner.jayson.metrics.io.{CSV, JSON}
import htsjdk.samtools.{SamReaderFactory, ValidationStringency}

import scala.collection.JavaConverters._
import scala.util.Success

/**
  * Reads an aligned BAM and produces metrics in either CSV or JSON format
  */
object MainTest extends App {

  val p = Paths.get("/Users/jfalkner/tokeep/bitbucket/itg/metrics-bam-aligned/example.bam")
  //JSON.write(Paths.get("example.json"), ab)
  //CSV.write(Paths.get("example.csv"), ab)

  val factory = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT)
  val bam = factory.open(p)

  bam.getFileHeader.getAttributes
  println("Program Records")
  for (pm <- bam.getFileHeader.getProgramRecords.iterator.asScala)
    println(pm)


  println("Resource Description")
  println(bam.getResourceDescription)

  // comments
  println("\nComments")
  bam.getFileHeader.getComments.asScala.foreach(println)

  // FileHeader
  // dump the program records
  println("\nProgram Records")
  def programRecord(key: String) : Unit = {
    // pull the BLASR header
    val pr = bam.getFileHeader.getProgramRecords.asScala.filter(_.getProgramName == key).headOption
    pr match {
      case Some(pr) => println(s"$key: VersionNumber: ${pr.getProgramVersion}, CommandLine: ${pr.getCommandLine}")
      case None => println("No match")
    }
  }
  programRecord("BLASR")
  programRecord("baz2bam")
  programRecord("bazformat")
  programRecord("bazwriter")


  println("\nCreator")
  println(bam.getFileHeader.getCreator)

  println("\nVersion")
  println(bam.getFileHeader.getVersion)

  println("\nSeq Dictionary")
  println(bam.getFileHeader.getSequenceDictionary)

  println("FileHeader Attribute:" + bam.getFileHeader.getAttributes)

  println("\nRead Groups")
  val rg = bam.getFileHeader.getReadGroups.asScala.head
  println("Read Groups: " + rg)
  println("Platform: "+ rg.getPlatform)
  println("PlatformModel: "+ rg.getPlatformModel)
  println("PlatformUnit: "+ rg.getPlatformUnit)
  println("Description: "+ rg.getDescription)
  val rgm = rg.getDescription.split(";").map(_.split("=")).map(kv => (kv(0), kv(1))).toMap
  println("BINDINGKIT: "+ rgm("BINDINGKIT"))
  println("SEQUENCINGKIT: "+ rgm("SEQUENCINGKIT"))
  println("BASECALLERVERSION: "+ rgm("BASECALLERVERSION"))
  println("FRAMERATEHZ: "+ rgm("FRAMERATEHZ"))

  println("Library: " + rg.getLibrary)
  println("Sample: " + rg.getSample)
  println("RunDate: " + rg.getRunDate)
  //println("READTYPE: "+ )
  println("\nRunGroup Attributes" + rg.getAttributes)
  println(rg.getAttributes)

  // [PL=PACBIO, DS=READTYPE=SUBREAD;DeletionQV=dq;DeletionTag=dt;InsertionQV=iq;MergeQV=mq;SubstitutionQV=sq;SubstitutionTag=st;Ipd:Frames=ip;PulseWidth:Frames=pw;PkMid=pm;PkMean=pa;LabelQV=pq;AltLabel=pt;AltLabelQV=pv;PulseMergeQV=pg;PulseCall=pc;PrePulseFrames=pd;PulseCallWidth=px;StartFrame=sf;BINDINGKIT=100-619-300;SEQUENCINGKIT=100-867-300;BASECALLERVERSION=3.1.0.179493;FRAMERATEHZ=80.000000, PU=m54054_160927_030637, PM=SEQUEL]



  var count = 0
  for (r <- bam.iterator.asScala if count < 3) {
    println(r.getReadString)
    println(r.getBaseQualityString)

    println(r.getAttributes.asScala.map(_.tag))
    val rm = r.getAttributes.asScala.map(tv => (tv.tag, tv.value)).toMap
    println("DeletionQV: "+rm("dq"))
    println("DeletionTag: "+rm("dt"))
    println("InsertionQV: "+rm("iq"))
    println("MergeQV: "+rm("mq"))
    println("SubstitutionQV: "+rm("sq"))
    println("SubstitutionTag: "+rm("st"))
    println("Ipd:Frames: "+rm("ip").asInstanceOf[Array[Short]].toList)
    println("PulseWidth:Frames: "+rm("pw").asInstanceOf[Array[Short]].toList)
    println("PkMid: "+rm("pm").asInstanceOf[Array[Short]].toList)
    println("PkMean: "+rm("pa").asInstanceOf[Array[Short]].toList)
    println("LabelQV: "+rm("pq"))
    println("AltLabel: "+rm("pt"))
    println("AltLabelQV: "+rm("pv"))
    println("PulseMergeQV: "+rm("pg"))
    println("PulseCall: "+rm("pc"))
    println("PrePulseFrames: "+rm("pd").asInstanceOf[Array[Short]].toList)
    println("PulseCallWidth: "+rm("px").asInstanceOf[Array[Short]].toList)
    println("StartFrame: "+rm("sf").asInstanceOf[Array[Int]].toList)
    println("ZMW Hole Number: "+rm("zm"))
    println("Accuracy: "+rm("rq"))
    println("Avg SNR: A; "+rm("sn").asInstanceOf[Array[Float]](0))
    println("Avg SNR: C; "+rm("sn").asInstanceOf[Array[Float]](1))
    println("Avg SNR: G; "+rm("sn").asInstanceOf[Array[Float]](2))
    println("Avg SNR: T; "+rm("sn").asInstanceOf[Array[Float]](3))
    println("RG: "+rm("RG"))
    println("NM: "+rm("NM"))
    println("AS: "+rm("AS"))
    count += 1
  }

}
