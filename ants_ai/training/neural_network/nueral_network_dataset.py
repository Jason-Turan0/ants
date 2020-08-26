from math import floor
from typing import List

from training.neural_network.neural_network_example import NeuralNetworkExample
import numpy as np
import tensorflow as tf


class NeuralNetworkDataset:
    def __init__(self, examples: List[NeuralNetworkExample]):
        self.examples = examples
        #self.training_features =np.array([e.nn_input for e in examples[0: cutoff]], np.bool)
        #self.training_labels = np.array([e.output for e in examples[0: cutoff]], np.bool)

        #self.test_features =np.array([e.nn_input for e in examples[cutoff:]], np.bool)
        #self.test_labels =np.array([e.output for e in examples[cutoff:]], np.bool)
