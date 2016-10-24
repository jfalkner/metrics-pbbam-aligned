# PacBio Aligned BAM Metrics

Exports commonly used metrics from PacBio reads (aka subread.bam)
aligned to references (e.g. lambdaNEB.fasta). The majority of these
metrics are already calculated and in the BAM file according to the "[BAM
format specification for PacBio](http://pacbiofileformats.readthedocs.io/en/3.0/BAM.html)".

Any [BAM file](http://samtools.github.io/hts-specs/SAMv1.pdf) can be
parsed by this library; however, it was designed with the intention of
being useful when analyzing alignments of PacBio's long reads from the
Sequel instrument.

A full [list of metrics](#metrics) and what they mean is below. It'll be
moved to better docs in the future.

## Usage

This build is based on SBT. You can use it from the command-line as follows.

```
# Export metrics from `example.bam` in CSV format as `metrics.csv`
sbt "run example.bam metrics.csv"

# Export metrics from `example.bam` in JSON format as `metrics.json`
sbt "run example.bam metrics.csv"
```

CSV is often the simplest to work with, especially if you are using JMP,
Excel or similar programs. Since CSV's are flat, bin counts from
distributions aren't included. Only calculated values such as mean
and median are.

```
# e.g. CSV is flat and omits lists of values and nested data
BLASR Version,BLASR Command Line,baz2bam Version,baz2bam Command Line,bazformat Version,bazwriter,Instrument Model,Movie,Binding Kit,Sequencing Kit,Base Caller Version,Frame Rate,Accuracy: Samples,Accuracy: Bins,Accuracy: BinWidth,Accuracy: Mean,Accuracy: Median,Accuracy: Min,Accuracy: Max,Mean of SnR A Mean: Samples,Mean of SnR A Mean: Bins,Mean of SnR A Mean: BinWidth,Mean of SnR A Mean: Mean,Mean of SnR A Mean: Median,Mean of SnR A Mean: Min,Mean of SnR A Mean: Max,Mean of SnR C Mean: Samples,Mean of SnR C Mean: Bins,Mean of SnR C Mean: BinWidth,Mean of SnR C Mean: Mean,Mean of SnR C Mean: Median,Mean of SnR C Mean: Min,Mean of SnR C Mean: Max,Mean of SnR G Mean: Samples,Mean of SnR G Mean: Bins,Mean of SnR G Mean: BinWidth,Mean of SnR G Mean: Mean,Mean of SnR G Mean: Median,Mean of SnR G Mean: Min,Mean of SnR G Mean: Max,Mean of SnR T Mean: Samples,Mean of SnR T Mean: Bins,Mean of SnR T Mean: BinWidth,Mean of SnR T Mean: Mean,Mean of SnR T Mean: Median,Mean of SnR T Mean: Min,Mean of SnR T Mean: Max,Mean of IPD Frames Mean: Samples,Mean of IPD Frames Mean: Bins,Mean of IPD Frames Mean: BinWidth,Mean of IPD Frames Mean: Mean,Mean of IPD Frames Mean: Median,Mean of IPD Frames Mean: Min,Mean of IPD Frames Mean: Max,Mean of IPD Frames Median: Samples,Mean of IPD Frames Median: Bins,Mean of IPD Frames Median: BinWidth,Mean of IPD Frames Median: Mean,Mean of IPD Frames Median: Median,Mean of IPD Frames Median: Min,Mean of IPD Frames Median: Max,Mean of Pulse Width Frames Mean: Samples,Mean of Pulse Width Frames Mean: Bins,Mean of Pulse Width Frames Mean: BinWidth,Mean of Pulse Width Frames Mean: Mean,Mean of Pulse Width Frames Mean: Median,Mean of Pulse Width Frames Mean: Min,Mean of Pulse Width Frames Mean: Max,Median of Pulse Width Frames Median: Samples,Median of Pulse Width Frames Median: Bins,Median of Pulse Width Frames Median: BinWidth,Median of Pulse Width Frames Median: Mean,Median of Pulse Width Frames Median: Median,Median of Pulse Width Frames Median: Min,Median of Pulse Width Frames Median: Max,Mean of pkMid: Samples,Mean of pkMid: Bins,Mean of pkMid: BinWidth,Mean of pkMid: Mean,Mean of pkMid: Median,Mean of pkMid: Min,Mean of pkMid: Max,Median of pkMid: Samples,Median of pkMid: Bins,Median of pkMid: BinWidth,Median of pkMid: Mean,Median of pkMid: Median,Median of pkMid: Min,Median of pkMid: Max,Mean of pkMean: Samples,Mean of pkMean: Bins,Mean of pkMean: BinWidth,Mean of pkMean: Mean,Mean of pkMean: Median,Mean of pkMean: Min,Mean of pkMean: Max,Median of pkMean: Samples,Median of pkMean: Bins,Median of pkMean: BinWidth,Median of pkMean: Mean,Median of pkMean: Median,Median of pkMean: Min,Median of pkMean: Max,Mean of Pre-Pulse Frames: Samples,Mean of Pre-Pulse Frames: Bins,Mean of Pre-Pulse Frames: BinWidth,Mean of Pre-Pulse Frames: Mean,Mean of Pre-Pulse Frames: Median,Mean of Pre-Pulse Frames: Min,Mean of Pre-Pulse Frames: Max,Median of Pre-Pulse Frames: Samples,Median of Pre-Pulse Frames: Bins,Median of Pre-Pulse Frames: BinWidth,Median of Pre-Pulse Frames: Mean,Median of Pre-Pulse Frames: Median,Median of Pre-Pulse Frames: Min,Median of Pre-Pulse Frames: Max,Mean of Pulse Call Width Frames Mean: Samples,Mean of Pulse Call Width Frames Mean: Bins,Mean of Pulse Call Width Frames Mean: BinWidth,Mean of Pulse Call Width Frames Mean: Mean,Mean of Pulse Call Width Frames Mean: Median,Mean of Pulse Call Width Frames Mean: Min,Mean of Pulse Call Width Frames Mean: Max,Median of Pulse Call Width Frames Median: Samples,Median of Pulse Call Width Frames Median: Bins,Median of Pulse Call Width Frames Median: BinWidth,Median of Pulse Call Width Frames Median: Mean,Median of Pulse Call Width Frames Median: Median,Median of Pulse Call Width Frames Median: Min,Median of Pulse Call Width Frames Median: Max,IPD Frames: Samples,IPD Frames: Bins,IPD Frames: BinWidth,IPD Frames: Mean,IPD Frames: Median,IPD Frames: Min,IPD Frames: Max,Pulse Width Frames: Samples,Pulse Width Frames: Bins,Pulse Width Frames: BinWidth,Pulse Width Frames: Mean,Pulse Width Frames: Median,Pulse Width Frames: Min,Pulse Width Frames: Max,PkMean Width Frames: Samples,PkMean Width Frames: Bins,PkMean Width Frames: BinWidth,PkMean Width Frames: Mean,PkMean Width Frames: Median,PkMean Width Frames: Min,PkMean Width Frames: Max,PkMid Width Frames: Samples,PkMid Width Frames: Bins,PkMid Width Frames: BinWidth,PkMid Width Frames: Mean,PkMid Width Frames: Median,PkMid Width Frames: Min,PkMid Width Frames: Max,Pre-Pulse Frames: Samples,Pre-Pulse Frames: Bins,Pre-Pulse Frames: BinWidth,Pre-Pulse Frames: Mean,Pre-Pulse Frames: Median,Pre-Pulse Frames: Min,Pre-Pulse Frames: Max
5.3.90d98ad*,/pbi/dept/itg/smrtlink-chips/pitchfork/workspace/blasr/blasr /pbi/collections/312/3120165/r54054_20160926_223846/11_C01/m54054_160927_030637.subreads.bam /pbi/dept/itg/references/lambdaNEB/sequence/lambdaNEB.fasta --bam --out ./example.bam,2.5.4.179493,/opt/pacbio/ppa-2.5.4/bin/baz2bam /data/pa/m54054_160927_030637.baz -o /data/pa//m54054_160927_030637 --metadata /data/pa/m54054_160927_030637.metadata.xml -j 12 -b 12 --progress --silent --minSubLength 50 --minSnr 4.000000 --adapters /data/pa//m54054_160927_030637.adapters.fasta --controls /etc/pacbio/600bp_Control_c2.fasta,1.3.0,2.5.4.179493,SEQUEL,m54054_160927_030637,100-619-300,100-867-300,3.1.0.179493,80.000000,313782,30,0.0,0.7978341,0.8,0.8,0.8,313782,30,0.2643791,5.922769,6.006561,4.000033,11.931406,313782,30,0.5282866,10.933327,11.110972,4.900301,20.748898,313782,30,0.3439346,8.554817,8.670342,4.0105743,14.328612,313782,30,0.56079346,12.83415,12.992999,6.0898585,22.913662,313782,30,15.8719015,5.8713055,-5.2827163,-177.51955,298.6375,313782,30,3,24.850494,24,5,116,313782,30,2.4644737,10.791893,10.703704,-28.89064,45.04357,313782,30,1,7.628962,8,2,28,313782,30,40.673355,-0.03930941,-0.05263158,-610.22,609.9808,313782,30,274,4022.8196,3968,0,8227,313782,30,41.979332,-0.05027073,-0.12415197,-631.24,628.14,313782,30,230,4075.6768,4016,1122,8022,313782,30,15.8719015,5.8713055,-5.2827163,-177.51955,298.6375,313782,30,3,24.850494,24,5,116,313782,30,2.4644737,10.791893,10.703704,-28.89064,45.04357,313782,30,1,7.628962,8,2,28,259345527,30,32,-7.2760673,-1,1,965,259345527,30,32,-7.2760673,-1,1,965,260086136,30,999,5.083147,-1,333,30309,260086136,30,1026,4.8908725,-1,0,30792,260037167,30,1073,-3.6026993,-1,0,32197
```

JSON will include the full output data.

```
# e.g. JSON shows the histogram's bins. In general, will have all nested data.
{
  "Binding Kit": "100-619-300",
  "Mean of Pre-Pulse Frames": {
    "Min": -177.51955,
    "Mean": 5.8713055,
    "Max": 298.6375,
    "Bins": [4, 0, 5, 15, 32, 142, 690, 3833, 16785, 46315, 122760, 6309, 12242, 62651, 32062, 7253, 1785, 471, 238, 103, 44, 24, 12, 3, 1, 1, 0, 0, 1, 1],
    "BinWidth": 15.8719015,
    "Samples": 313782,
    "Median": -5.2827163
  },
  "Instrument Model": "SEQUEL",
  "Sequencing Kit": "100-867-300",
  "BLASR Command Line": "/pbi/dept/itg/smrtlink-chips/pitchfork/workspace/blasr/blasr /pbi/collections/312/3120165/r54054_20160926_223846/11_C01/m54054_160927_030637.subreads.bam /pbi/dept/itg/references/lambdaNEB/sequence/lambdaNEB.fasta --bam --out ./example.bam ",
...
```

## Metrics

Below is a list of exported metrics.

PacBio-specific headers and run info (PB's BLASR output should have just
one run group).

TODO: Better descriptions.

- Str("BLASR Version", () => pr("BLASR").getProgramVersion),
- Str("BLASR Command Line", () => pr("BLASR").getCommandLine),
- Str("baz2bam Version", () => pr("baz2bam").getProgramVersion),
- Str("baz2bam Command Line", () => pr("baz2bam").getCommandLine),
- Str("bazformat Version", () => pr("bazformat").getProgramVersion),
- Str("bazwriter", () => pr("bazwriter").getProgramVersion),
- Str("Instrument Model", () => rg.getPlatformModel),
- Str("Movie", () => rg.getPlatformUnit),
- Str("Binding Kit", () => rgm("BINDINGKIT")),
- Str("Sequencing Kit", () => rgm("SEQUENCINGKIT")),
- Str("Base Caller Version", () => rgm("BASECALLERVERSION")),
- Num("Frame Rate", () => rgm("FRAMERATEHZ")),

Per-read information summarized via mean, media and distributions that 
are suitable to plot as histograms.

TODO: Better descriptions.

- Dist("Accuracy", calcContinuous(reads.map(_.accuracy))),
- Dist("Mean of SnR A Mean", calcContinuous(reads.map(_.avgSnrA))),
- Dist("Mean of SnR C Mean", calcContinuous(reads.map(_.avgSnrC))),
- Dist("Mean of SnR G Mean", calcContinuous(reads.map(_.avgSnrG))),
- Dist("Mean of SnR T Mean", calcContinuous(reads.map(_.avgSnrT))),
- CatDist("Del Quality", makeCategorical(sumQual((r) => r.delQV))),
- CatDist("Del Tag", makeCategorical(sumQual((r) => r.delTag))),
- CatDist("Ins Quality", makeCategorical(sumQual((r) => r.insQV))),
- CatDist("Merge Quality", makeCategorical(sumQual((r) => r.mergeQV))),
- CatDist("Substitution Quality", makeCategorical(sumQual((r) => r.subQV))),
- CatDist("Substitution Tag", makeCategorical(sumQual((r) => r.subTag))),
- CatDist("Label Quality", makeCategorical(sumQual((r) => r.labelQV))),
- CatDist("Alt Label", makeCategorical(sumQual((r) => r.altLabel))),
- CatDist("Alt Label Qual", makeCategorical(sumQual((r) => r.labelQV))),
- CatDist("Pulse Merge Qual", makeCategorical(sumQual((r) => r.pulseMergeQV))),
- CatDist("Pulse Call", makeCategorical(sumQual((r) => r.pulseCall))), // true categorical
- Dist("Mean of IPD Frames Mean", calcContinuous(reads.map(_.ipdFrames.mean))),
- Dist("Mean of IPD Frames Median", calcDiscrete(reads.map(_.ipdFrames.median))),
- Dist("Mean of Pulse Width Frames Mean", calcContinuous(reads.map(_.pulseWidthFrames.mean))),
- Dist("Median of Pulse Width Frames Median", calcDiscrete(reads.map(_.pulseWidthFrames.median))),
- Dist("Mean of pkMid", calcContinuous(reads.map(_.pkMid.mean))),
- Dist("Median of pkMid", calcDiscrete(reads.map(_.pkMid.median))),
- Dist("Mean of pkMean", calcContinuous(reads.map(_.pkMean.mean))),
- Dist("Median of pkMean", calcDiscrete(reads.map(_.pkMean.median))),
- Dist("Mean of Pre-Pulse Frames", calcContinuous(reads.map(_.prePulseFrames.mean))),
- Dist("Median of Pre-Pulse Frames", calcDiscrete(reads.map(_.prePulseFrames.median))),
- Dist("Mean of Pulse Call Width Frames Mean", calcContinuous(reads.map(_.pulseCallWidthFrames.mean))),
- Dist("Median of Pulse Call Width Frames Median", calcDiscrete(reads.map(_.pulseCallWidthFrames.median))),

Aggregate distributions that summarize all values across all reads. Useful
to examine if the overall sequencing run had trends that aren't well 
characterized by metrics that assume normality (namely mean and median).

- Dist("IPD Frames", mergeIntDist((r) => r.pulseWidthFrames)),
- Dist("Pulse Width Frames", mergeIntDist((r) => r.pulseWidthFrames)),
- Dist("PkMean Width Frames", mergeIntDist((r) => r.pkMean)),
- Dist("PkMid Width Frames", mergeIntDist((r) => r.pkMid)),
- Dist("Pre-Pulse Frames", mergeIntDist((r) => r.prePulseFrames))