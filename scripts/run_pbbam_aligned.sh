#!/bin/bash

DEPLOY={{ deploy_dir }}

java -Xmx32G -cp $( ls $DEPLOY/lib/itg_metrics_pbbam_aligned*.jar ):$DEPLOY/lib/* com.pacb.itg.metrics.pbbam.aligned.Main $1 $2