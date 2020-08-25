from math import floor
from typing import List

from training.neural_network.neural_network_example import NeuralNetworkExample
import numpy as np
import tensorflow as tf


class NeuralNetworkDataset:
    def __init__(self, examples: List[NeuralNetworkExample]):
        cutoff = floor(len(examples) * .60)
        training_features =np.array([e.nn_input for e in examples[0: cutoff]], np.bool)
        training_labels = np.array([e.output for e in examples[0: cutoff]], np.bool)

        test_features =np.array([e.nn_input for e in examples[cutoff:]], np.bool)
        test_labels =np.array([e.output for e in examples[cutoff:]], np.bool)
        self.train = tf.data.Dataset.from_tensor_slices((training_features, training_labels))
        self.test = tf.data.Dataset.from_tensor_slices((test_features, test_labels))
