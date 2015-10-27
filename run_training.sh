#!/bin/bash

mkdir -p dataset
mkdir -p output
python mlp/start_training.py -h
printf "\n\nSTART TRAINING & EVALUATION\n\n"
python mlp/start_training.py -d dataset/*.zip -o output/ -e output/evaluation.csv -v output/visualisation.png
open output/visualisation.png
open output/evaluation.csv