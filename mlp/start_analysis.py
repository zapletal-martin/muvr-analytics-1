import sys
from pyspark import SparkContext, SparkConf
import os
from converters import neon2iosmlp
from training.acceleration_dataset import AccelerationDataset
from training.mlp_model import MLPMeasurementModel

def learn_model_from_data(dataset_directory, working_directory, user_id):
    dataset = AccelerationDataset(dataset_directory)
    mlpmodel = MLPMeasurementModel(working_directory)

    trainedModel = mlpmodel.train(dataset)

    dataset.save_labels(os.path.join(working_directory, user_id + '_model.labels.txt'))
    neon2iosmlp.convert(mlpmodel.model_path, os.path.join(working_directory, user_id + '_model.weights.raw'))

    layers = mlpmodel.getLayer(dataset, trainedModel)
    neon2iosmlp.write_model_to_file(layers, os.path.join(working_directory, user_id + '_model.layers.txt'))

    return mlpmodel.model_path

def print_it(x):
    print x

def is_spark_dump_file(filename):
    return filename.startswith('part')

def main(sc):
    """Main entry point. Connects to cassandra and starts training."""

    root_directory = '/Users/tombocklisch/data/spark-csv-exercises'

    user_files = filter(is_spark_dump_file, os.listdir(os.path.join(root_directory, 'users')))

    user_rdds = ",".join(map(lambda d: os.path.join(root_directory, 'users', d), user_files))

    rdd = sc.textFile(user_rdds)
    models = rdd.map(lambda user_id:
                     learn_model_from_data(dataset_directory=os.path.join(root_directory, 'datasets', user_id),
                                           working_directory=os.path.join(root_directory, 'models', user_id),
                                           user_id=user_id))

    models.foreach(print_it)

if __name__ == '__main__':
    # TODO: Where to get cluster ip from?
    # TODO: Which keyspace?
    conf = {
        'target_length': 400,
        'number_of_labels': 29
    }

    conf = SparkConf().setAppName('Muvr python analysis').setMaster('local[*]')

    # An external script needs to make sure that all the dependencies are packaged and provided to the workers!
    sc = SparkContext(conf=conf)

    sys.exit(main(sc))
