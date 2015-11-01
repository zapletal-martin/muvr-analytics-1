# To execute run

# mkdir output
# mkdir output/1
# mkdir output/2
# mkdir output/3
# mkdir output/4
# mkdir output/5

# (venv) mlp: $SPARK_HOME/bin/spark-submit --master local[5] start_training_spark.py -m chest -d ../../muvr-training-data/labelled/chest -o ../output  -v v.png -e e.csv

from __future__ import print_function
import sys
import argparse
from pyspark import SparkContext
from start_training import main

def spark_main(dataset_directory, working_directory, evaluation_file, visualise_image, model_name):
    sc = SparkContext("local[5]", "StartTrainingSpark")

    lines = sc \
        .parallelize([1, 2, 3, 4, 5]) \
        .map(lambda x: main( \
            dataset_directory, \
            working_directory + "/" + str(x), \
            working_directory + "/" + str(x) + "/" + evaluation_file, \
            working_directory + "/" + str(x) + "/" + visualise_image, \
            model_name))

    lines.foreach(lambda x: print(x))

if __name__ == '__main__':
    """List arguments for this program"""
    parser = argparse.ArgumentParser(description='Train and evaluate the exercise dataset.')
    parser.add_argument('-d', metavar='dataset', type=str, help="folder containing exercise dataset")
    parser.add_argument('-o', metavar='output', default='./output', type=str, help="folder containing generated model")
    parser.add_argument('-e', metavar='evaluation', default='./output/evaluation.csv', type=str, help="evaluation csv file output")
    parser.add_argument('-v', metavar='visualise', default='./output/visualisation.png', type=str, help="visualisation dataset image output")
    parser.add_argument('-m', metavar='modelname', type=str, help="prefix name of model")
    args = parser.parse_args()
    
    #
    # A good example of command-line params is
    # -m core -d ../../muvr-training-data/labelled/core -o ../output/ -v ../output/v.png -e  ../output/e.csv
    #
    sys.exit(spark_main(args.d, args.o, args.e, args.v, args.m))