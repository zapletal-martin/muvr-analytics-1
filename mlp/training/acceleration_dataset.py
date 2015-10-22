import collections
import numpy as np
from neon.data import DataIterator
import logging
import os
import csv
from training.augmentation import SignalAugmenter
from training.examples import ExampleColl


class AccelerationDataset(object):
    """Dataset containing examples based on acceleration data and their labels."""
    logger = logging.getLogger("analysis.AccelerationDataset")

    TRAIN_RATIO = 0.8

    # This defines the range of the values the accelerometer measures
    Feature_Range = 8000
    Feature_Mean = 0

    # Augmenter used to increase the number of training examples
    augmenter = None

    # Number of examples
    num_train_examples = None
    num_test_examples = None

    # Number of classes
    num_labels = None
    num_features = None

    # Indicator if the data has been loaded yet
    initialized = False

    # Mapping of integer class labels to strings
    human_labels = {}
    label_mapping = {}

    def human_label_for(self, label_id):
        """Convert a label id into a human readable string label."""
        return self.human_labels[label_id]

    def save_labels(self, filename):
        """Store the label <--> id mapping to file. The id is defined by the line number."""
        labels = collections.OrderedDict(sorted(self.human_labels.items())).values()

        f = open(filename, 'wb')

        for label in labels:
            f.write("%s\n" % label)
        f.close()

    def load_examples(self, directory):
        """Load examples contained in the directory into an example collection. Examples need to be stored in CSVs."""
        self.label_mapping = {}
        csv_files = filter(lambda f: f.endswith("csv"), os.listdir(directory))
        Xs = []
        ys = []
        for f in csv_files:
            try:
                X, label = self.load_example(os.path.join(directory, f))

            except Exception as e:
                print("Can't read csv file: %s, error: %s" % (f, e.message))
                continue

            if label not in self.label_mapping:
                self.label_mapping[label] = len(self.label_mapping)
            Xs.append(X)
            ys.append(self.label_mapping[label])

        return ExampleColl(Xs, ys)

    @staticmethod
    def load_example(filename):
        """Load a single example from a CSV file."""
        with open(filename, 'rb') as csvfile:
            dialect = csv.Sniffer().sniff(csvfile.read(1024))
            csvfile.seek(0)
            csv_data = csv.reader(csvfile, dialect)
            X = []
            for row in csv_data:
                label = row[0] + "/" + row[1]
                X.append(row[2:])

            X = np.transpose(np.reshape(np.asarray(X, dtype=float), (len(X), len(X[0]))))

        return X, label

    # Load label mapping and train / test data from disk.
    def __init__(self, directory):
        """Load the dataset data from the directory."""

        self.logger.info("Loading DS from files...")
        self.augmenter = SignalAugmenter(augmentation_start=0.1, augmentation_end=0.9)
        examples = self.load_examples(directory)
        examples.shuffle()

        train, test = examples.split(self.TRAIN_RATIO)

        augmented_train = self.augmenter.augment_examples(train, 400)
        print "Augmented `train` with %d examples, %d originally" % (
            augmented_train.num_examples - train.num_examples, train.num_examples)
        augmented_train.shuffle()
        augmented_train.scale_features(self.Feature_Range, self.Feature_Mean)

        augmented_test = self.augmenter.augment_examples(test, 400)
        print "Augmented `test` with %d examples, %d originally" % (
            augmented_test.num_examples - test.num_examples, test.num_examples)
        augmented_test.shuffle()
        augmented_test.scale_features(self.Feature_Range, self.Feature_Mean)

        self.human_labels = {v: k for k, v in self.label_mapping.items()}
        self.X_train = self.flatten2d(augmented_train.features)
        self.y_train = augmented_train.labels
        self.X_test = self.flatten2d(augmented_test.features)
        self.y_test = augmented_test.labels

        self.num_labels = len(self.human_labels)
        self.num_features = self.X_train.shape[1]
        self.num_train_examples = self.X_train.shape[0]
        self.num_test_examples = self.X_test.shape[0]

    @staticmethod
    def flatten2d(npa):
        """Take a 3D array and flatten the last dimension."""
        return npa.reshape((npa.shape[0], -1))

    # Get the dataset ready for Neon training
    def train(self):
        """Provide neon data iterator for training purposes."""
        return DataIterator(
            X=self.X_train,
            y=self.y_train,
            nclass=self.num_labels,
            make_onehot=True,
            lshape=(self.num_features, 1, 1))

    def test(self):
        """Provide neon data iterator for testing purposes."""
        return DataIterator(
            X=self.X_test,
            y=self.y_test,
            nclass=self.num_labels,
            make_onehot=True,
            lshape=(self.num_features, 1, 1))
