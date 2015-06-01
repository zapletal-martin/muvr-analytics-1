#!/bin/sh

spark-submit \
  --class "io.muvr.analytics.basic.FooMain" \
  --master local[4] \
  --driver-class-path basic/target/basic-assembly-1.0.0-SNAPSHOT.jar \
  basic/target/basic-assembly-1.0.0-SNAPSHOT.jar
