from math import floor
import random
from typing import List, Tuple
from tensorflow.keras.utils import plot_model

import numpy
import tensorflow as tf
from numpy import ndarray
from tensorflow.keras.layers import Dense
import unittest
from tensorflow.python.keras import Sequential
from tensorflow.python.keras.callbacks import LambdaCallback
from tensorflow.python.keras.layers import Activation
from training.game_state.game_map import Direction
from training.neural_network.game_state_translator import PositionState
from training.neural_network.neural_network_example import NeuralNetworkExample
from training.tests.test_utils import create_test_examples
import pandas as pd
from sklearn.preprocessing import OneHotEncoder

class TestTraining(unittest.TestCase):

    def encode_examples(self, examples:List[NeuralNetworkExample]) -> Tuple[ndarray, ndarray]:
        stringData = [[f.name for f in ex.features] for ex in examples]
        categories = [[m for m in PositionState.__members__] for i in range(len(examples[0].features))]
        columns = [f'pos_{num}' for num, ex in enumerate(examples[0].features)]
        in_df = pd.DataFrame(data=stringData, columns=columns)
        one_hot_encoder = OneHotEncoder(sparse=False, categories=categories)
        one_hot_encoder.fit(in_df)
        encoded_features = one_hot_encoder.transform(in_df)

        labels = [[e.label.name] for e in examples]
        label_df = pd.DataFrame(data=labels, columns=['direction'])
        #print(label_df)
        label_categories = [d for d in Direction.__members__]
        one_hot_label_encoder = OneHotEncoder(sparse=False, categories=[label_categories])
        one_hot_label_encoder.fit(label_df)
        encoded_labels = one_hot_label_encoder.transform(label_df)
        #print(type(encoded_labels))
        return encoded_features, encoded_labels

    def test_training4(self):
        botToEmulate = 'memetix_1'
        examples = create_test_examples(1, botToEmulate)
        random.shuffle(examples)
        cutoff = floor(len(examples) *.75)
        (train_features, train_labels) = self.encode_examples(examples[0:cutoff])
        (test_features, test_labels) = self.encode_examples(examples[cutoff:])
        print(f'trf={train_features.shape} trl={train_labels.shape} tf={test_features.shape} tl={test_labels.shape}')
        model = Sequential([
            Dense(64, input_dim=train_features.shape[1]),
            Activation('relu'),
            Dense(train_labels.shape[1]),
            Activation('softmax'),
        ])
        #predictions = model(label_df_encoded).numpy()
        #tf.nn.softmax(predictions).numpy()
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        #loss_fn(y_train[:1], predictions).numpy()
        epoch_print_callback = LambdaCallback(
            on_epoch_begin=lambda epoch,logs: print(f'HELLO WORLD! ${epoch}'))
        model.compile(optimizer='adam',
                      loss=loss_fn,
                      metrics=['accuracy'])
        model.fit(train_features, train_labels, epochs=100, callbacks=[epoch_print_callback])
        model.evaluate(test_features,  test_labels, verbose=2)

        prediction0 = model.predict(numpy.array([train_features[0]]))
        print(prediction0)
        print(train_labels[0])

        prediction1 = model.predict(numpy.array([test_features[0]]))
        print(prediction1)
        print(test_labels[0])

        plot_model(model, to_file='ants_model.png')