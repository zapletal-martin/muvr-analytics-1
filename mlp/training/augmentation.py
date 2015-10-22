"""Create new examples from existing one reproducing natural variation in the data"""

import numpy as np
import math
from training.examples import ExampleColl


class SignalAugmenter(object):
    """Dataset augmentation specialized for one dimensional data.

    A sliding window will be used to move it over the examples to augment the dataset with new examples."""

    def __init__(self, augmentation_start, augmentation_end):
        self.augmentation_start = augmentation_start
        self.augmentation_end = augmentation_end

    def augment_examples(self, examples, target_length):
        """Augments all the passed examples (should be a np array with 3 dimensions)."""
        augmented = []
        augmented_labels = []
        for i, features in enumerate(examples.features):
            if np.shape(features)[1] >= target_length:
                single_augmented, single_labels = self.augment_example(features, examples.labels[i], target_length)
                augmented.append(single_augmented)
                augmented_labels.append(single_labels)
            else:
                print("Dropped an example because it was to short. Length: %d" % np.shape(features)[1] )

        return ExampleColl(np.vstack(augmented), np.hstack(augmented_labels))

    def augment_example(self, example, label, target_length, window_step_size=5):
        """Example should be a numpy array, label a single label id."""
        dimensions = np.shape(example)[0]
        sample_length = np.shape(example)[1]

        # Those will define start and end of the data augmentation window, e.g. how far the window is moved over the
        # data. This assumes the first fraction of `augmentation_start` and the last fraction of `augmentation_end`
        # measurement points to be noise
        min_idx = int(sample_length * self.augmentation_start)
        max_idx = int(sample_length * self.augmentation_end)

        # Make sure we got enough data to get a complete example
        if (max_idx - min_idx) < target_length:
            start = (sample_length - target_length) / 2
            end = start + target_length
            return example[np.newaxis, :, start:end], np.array(label)
        else:
            num_augmented = math.floor((max_idx - target_length - min_idx) / window_step_size) + 1
            augmented = np.empty((num_augmented, dimensions, target_length))
            labels = np.empty(num_augmented)
            labels.fill(label)
            # Use a sliding window to move it over the input example. this will create new examples that can be used for
            # training a model
            for i in range(min_idx, max_idx - target_length, window_step_size):
                idx = (i - min_idx) / window_step_size
                augmented[idx, :, :] = example[:, i:(i + target_length)]
            return augmented, labels
