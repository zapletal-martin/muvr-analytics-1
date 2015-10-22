import random
import numpy as np
from time import time


class ExampleColl(object):
    """Collection of training examples. Provides useful helpers to modify the collection."""

    # To make debugging easier, lets avoid randomness
    Seed = 42  # time()

    def __init__(self, features, labels):
        self.features = features
        self.labels = labels
        self.num_examples = len(features)
        random.seed(self.Seed)

    def split(self, ratio):
        """Split the collection into two parts.

        The first part will be of the given ratio. Second part will contain the remaining examples."""
        split_point = int(self.num_examples * ratio)
        first = ExampleColl(self.features[0:split_point], self.labels[0:split_point])
        second = ExampleColl(self.features[split_point:self.num_examples], self.labels[split_point:self.num_examples])
        return first, second

    def scale_features(self, feature_range, feature_mean):
        """Scale the features of the examples using the passed range and mean."""
        self.features = np.divide(np.subtract(self.features, feature_mean), feature_range / 2.0)

    def shuffle(self):
        """Shuffle the examples in this collection randomly."""
        shuffled_idx = range(self.num_examples)
        random.shuffle(shuffled_idx)

        shuffled = []
        shuffled_labels = []
        for i in shuffled_idx:
            shuffled.append(self.features[i])
            shuffled_labels.append(self.labels[i])

        if isinstance(self.features, list):
            self.features = shuffled
            self.labels = shuffled_labels
        else:
            self.features = np.reshape(np.array(shuffled), (len(shuffled_labels),) + shuffled[0].shape)
            self.labels = np.reshape(np.array(shuffled_labels), (len(shuffled_labels)))
