# disables GPUs
import os
import datetime

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'

from math import floor, sin
import random
from typing import List, Tuple
from tensorflow.keras.utils import plot_model

import numpy
import tensorflow as tf
from numpy import ndarray
from tensorflow.keras.layers import Dense
import unittest
from tensorflow.python.keras import Sequential, Model
from tensorflow.python.keras.callbacks import LambdaCallback, History
from tensorflow.python.keras.layers import Activation, Dropout
from training.game_state.game_map import Direction
from training.neural_network.game_state_translator import PositionState
from training.neural_network.neural_network_example import NeuralNetworkExample
from training.tests.test_utils import create_test_examples, ExampleType
import pandas as pd
from sklearn.preprocessing import OneHotEncoder
import matplotlib.pyplot as plt
import pprint

pp = pprint.PrettyPrinter(indent=4)


class TestTraining(unittest.TestCase):

    def show_history(self, history: History):
        print(history.history.keys())

        pp.pprint(history.history)
        pp.pprint(history.epoch)
        # summarize history for accuracy
        plt.plot(history.history['accuracy'])
        plt.plot(history.history['val_accuracy'])
        plt.title('model accuracy')
        plt.ylabel('accuracy')
        plt.xlabel('epoch')
        plt.legend(['train', 'cross_validation'], loc='upper left')
        plt.show()
        # summarize history for loss
        plt.plot(history.history['loss'])
        plt.plot(history.history['val_loss'])
        plt.title('model loss')
        plt.ylabel('loss')
        plt.xlabel('epoch')
        plt.legend(['train', 'cross_validation'], loc='upper left')
        plt.show()

    def encode_examples(self, examples: List[NeuralNetworkExample]) -> Tuple[ndarray, ndarray]:
        stringData = [[f.name for f in ex.features] for ex in examples]
        categories = [[m for m in PositionState.__members__] for i in range(len(examples[0].features))]
        columns = [f'pos_{num}' for num, ex in enumerate(examples[0].features)]
        in_df = pd.DataFrame(data=stringData, columns=columns)
        one_hot_encoder = OneHotEncoder(sparse=False, categories=categories)
        one_hot_encoder.fit(in_df)
        encoded_features = one_hot_encoder.transform(in_df)
        labels = [[e.label.name] for e in examples]
        label_df = pd.DataFrame(data=labels, columns=['direction'])
        label_categories = [d for d in Direction.__members__]
        one_hot_label_encoder = OneHotEncoder(sparse=False, categories=[label_categories])
        one_hot_label_encoder.fit(label_df)
        encoded_labels = one_hot_label_encoder.transform(label_df)
        return encoded_features, encoded_labels

    def create_dense_model(self, train_features, train_labels) -> Tuple[str, Model]:
        model = Sequential([
            Dense(64, input_dim=train_features.shape[1], activation=tf.nn.relu),
            Dropout(0.1),
            Dense(64, activation=tf.nn.relu),
            Dense(train_labels.shape[1], activation=tf.nn.softmax),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        model.compile(optimizer='adam',
                      loss=loss_fn,
                      metrics=['accuracy'])
        return 'dense', model

    # @unittest.skip('Integration test')
    def test_training(self):
        botToEmulate = 'memetix_1'
        examples = create_test_examples(100, botToEmulate, ExampleType.ANT_VISION)
        random.shuffle(examples)
        train_cutoff = floor(len(examples) * .60)
        cv_cutoff = floor(len(examples) * .80)
        (train_features, train_labels) = self.encode_examples(examples[0:train_cutoff])
        (cv_features, cv_labels) = self.encode_examples(examples[train_cutoff:cv_cutoff])
        (test_features, test_labels) = self.encode_examples(examples[cv_cutoff:])
        print(
            f'trf={train_features.shape} trl={train_labels.shape} cvf={cv_features.shape} cv;={cv_labels.shape} tf={test_features.shape} tl={test_labels.shape}')

        log_dir = "logs/fit/" + datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        file_writer = tf.summary.create_file_writer(log_dir + "/metrics")
        file_writer.set_as_default()

        def log_sin(epoch, logs):
            data = sin(epoch)
            print(data)
            tf.summary.scalar('sin', data=data, step=epoch)

        tensorboard_callback = tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1)
        (model_type, model) = self.create_dense_model(train_features, train_labels)
        log_sin_cb = LambdaCallback(on_epoch_begin=log_sin)
        fit = model.fit(train_features, train_labels, validation_data=(cv_features, cv_labels), epochs=50,
                        batch_size=10, callbacks=[tensorboard_callback, log_sin_cb])
        model.evaluate(test_features, test_labels, verbose=2)

        prediction0 = model.predict(numpy.array([train_features[0]]))
        print(prediction0)
        print(train_labels[0])

        prediction1 = model.predict(numpy.array([test_features[0]]))
        print(prediction1)
        print(test_labels[0])
