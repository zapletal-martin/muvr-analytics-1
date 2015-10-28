import collections
import numpy as np
from neon.data import DataIterator
import logging
import os
import csv
import zipfile
import tempfile
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
    id_label_mapping = {}
    label_id_mapping = {}

    def human_label_for(self, label_id):
        """Convert a label id into a human readable string label."""
        return self.id_label_mapping[label_id]

    def save_labels(self, filename):
        """Store the label <--> id mapping to file. The id is defined by the line number."""
        labels = collections.OrderedDict(sorted(self.id_label_mapping.items())).values()

        f = open(filename, 'wb')

        for label in labels:
            f.write("%s\n" % label)
        f.close()

    def load_examples(self, path, label_mapper):
        """Load examples contained in the path into an example collection. Examples need to be stored in CSVs.

        The label_mapper allows to map a loaded label to a different label, e.g. to combine multiple labels into one.
        Arguments:
        path -- can be directory or zipfile. If zipfile, it will be extract to a tmp path with prefix /tmp/muvr-training-
        """
        self.label_id_mapping = {}
        root_directory = ""
        if os.path.isdir(path):
            root_directory = path
        else:
            # Zip file - extract to temp root_directory first
            root_directory = tempfile.mkdtemp(prefix="/tmp/muvr-training-")
            zipfile.ZipFile(path, 'r').extractall(root_directory)

        #csv_files = filter(lambda f: f.endswith("csv"), os.listdir(root_directory))
        csv_files = []
        def append_csv_file(arg, direname, names):
            for name in names:
                f = os.path.join(direname, name)
                if os.path.isfile(f) and f.endswith("csv"):
                    csv_files.append(f)

        os.path.walk(root_directory, append_csv_file, None)
        Xs = []
        ys = []
        for f in csv_files:
            X, read_label = self.load_example(f)
            label = label_mapper(read_label)
            if label not in self.label_id_mapping:
                self.label_id_mapping[label] = len(self.label_id_mapping)
            Xs.append(X)
            ys.append(self.label_id_mapping[label])

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
    def __init__(self, directory, test_directory=None, label_mapper=lambda x: x):
        """Load the dataset data from the directory.

        If two directories are passed the second is interpreted as the test dataset. If only one dataset gets passed,
         this dataset will get split into test and train. The label_mapper`allows to modify loaded labels. This is
         useful e.g. to map multiple labels to a single on ("arms/biceps-curl" --> "-/exercising", ...)."""

        self.logger.info("Loading DS from files...")
        self.augmenter = SignalAugmenter(augmentation_start=0.1, augmentation_end=0.9)

        # If we get provided with a test directory, we are going to use that. Otherwise we will split the dataset in
        # test and train on our own.
        if test_directory:
            train = self.load_examples(directory, label_mapper)
            test = self.load_examples(test_directory, label_mapper)
        else:
            examples = self.load_examples(directory, label_mapper)
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

        self.id_label_mapping = {v: k for k, v in self.label_id_mapping.items()}
        self.X_train = self.flatten2d(augmented_train.features)
        self.y_train = augmented_train.labels
        self.X_test = self.flatten2d(augmented_test.features)
        self.y_test = augmented_test.labels

        self.num_labels = len(self.id_label_mapping)
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
