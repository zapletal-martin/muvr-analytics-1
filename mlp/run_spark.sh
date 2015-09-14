#!/bin/bash

python setup.py sdist --formats=zip

spark-submit start_analysis.py --py-files dist/muvr-analysis-mlp-1.0-SNAPSHOT.zip