#!/bin/sh

CLASSNAME="basic.SimpleMain"

if [ -n "$1" ]
then
    CLASSNAME=$1
fi

spark-submit \
  --class "io.muvr.analytics.$CLASSNAME" \
  --master local[4] \
  --driver-class-path basic/target/basic-assembly-1.0.0-SNAPSHOT.jar \
  basic/target/basic-assembly-1.0.0-SNAPSHOT.jar
