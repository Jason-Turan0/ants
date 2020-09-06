# disables GPUs
import os
import datetime

from training.game_state.generator import GameStateGenerator

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
from tensorflow.python.keras import Sequential, Model, Input
from tensorflow.python.keras.callbacks import LambdaCallback, History
from tensorflow.python.keras.layers import Activation, Dropout, Flatten, Conv2D, Conv1D, concatenate
from training.game_state.game_map import Direction, Position
from training.neural_network.game_state_translator import PositionState, GameStateTranslater
from training.neural_network.neural_network_example import AntVision1DExample, AntMapExample, AntMapDataset
from training.tests.test_utils import create_test_examples, ExampleType, create_test_play_results
import pandas as pd
from sklearn.preprocessing import OneHotEncoder
import matplotlib.pyplot as plt
import pprint

pp = pprint.PrettyPrinter(indent=4)


class TrainingDataset:
    def __init__(self, features: ndarray, labels: ndarray):
        self.features = features
        self.labels = labels


class ModelSettings:
    def __init__(self, model: Model, model_name: str, training: TrainingDataset, cross_val: TrainingDataset):
        self.cross_val = cross_val
        self.training = training
        self.model_name = model_name
        self.model = model


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

    def encode_2d_examples(self, examples: List[AntVision1DExample]) -> TrainingDataset:
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
        return TrainingDataset(encoded_features, encoded_labels)

    def encode_3d_examples(self, examples: List[AntVision1DExample]) -> TrainingDataset:
        t = GameStateTranslater()
        features = numpy.array(
            [[t.convert_enum_to_array(f, PositionState) for f in ex.features] for ex in examples])
        labels = numpy.array([t.convert_enum_to_array(ex.label, Direction) for ex in examples])
        return TrainingDataset(features, labels)

    def encode_map_examples(self, row_count: int, column_count: int, examples: List[AntMapExample]) -> TrainingDataset:
        t = GameStateTranslater()
        features = []
        for e in examples:
            encoded_example = [[t.convert_enum_to_array(e.features[Position(row, col)], PositionState) for col in
                                range(column_count)] for row in range(row_count)]
            features.append(encoded_example)
        labels = [t.convert_enum_to_array(ex.label, Direction) for ex in examples]
        return TrainingDataset(numpy.array(features), numpy.array(labels))

    def create_dense_3d_model(self, learning_rate, train_examples, cv_examples) -> ModelSettings:
        train_data = self.encode_3d_examples(train_examples)
        cv_data = self.encode_3d_examples(cv_examples)
        # print(train_data.features.shape)
        # print(train_data.labels.shape)
        model = Sequential([
            Input(shape=(train_data.features.shape[1], train_data.features.shape[2])),
            Dense(64, activation=tf.nn.relu),
            Flatten(),
            Dropout(0.1),
            Dense(train_data.labels.shape[1], activation=tf.nn.softmax),
        ])
        model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
                      loss=tf.keras.losses.CategoricalCrossentropy(),
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return ModelSettings(model, 'dense_3d', train_data, cv_data)

    def create_dense_2d_model(self, learning_rate, train_examples, cv_examples) -> ModelSettings:
        train_data = self.encode_2d_examples(train_examples)
        cv_data = self.encode_2d_examples(cv_examples)
        model = Sequential([
            Dense(128, input_dim=train_data.features.shape[1], activation=tf.nn.relu),
            Dropout(0.1),
            Dense(128, activation=tf.nn.relu),
            Dense(train_data.labels.shape[1], activation=tf.nn.softmax),
        ])
        model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
                      loss=tf.keras.losses.CategoricalCrossentropy(),
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return ModelSettings(model, 'dense_2d', train_data, cv_data)

    def create_conv_1d_model(self, learning_rate, train_examples, cv_examples) -> ModelSettings:
        train_data = self.encode_3d_examples(train_examples)
        cv_data = self.encode_3d_examples(cv_examples)
        print(train_data.features.shape)
        print(train_data.labels.shape)
        model = Sequential([
            Input(shape=(train_data.features.shape[1], train_data.features.shape[2])),
            Conv1D(32, 8, strides=4, activation=tf.nn.relu),
            Conv1D(64, 4, strides=2, activation=tf.nn.relu),
            Conv1D(64, 3, strides=1, activation=tf.nn.relu),
            Flatten(),
            Dropout(0.1),
            Dense(64, activation=tf.nn.relu),
            Dense(train_data.labels.shape[1], activation=tf.nn.softmax),
        ])
        for i, l in enumerate(model.layers):
            print(f'Layer {i}')
            print(f'In {l.input_shape}')
            print(f'Out {l.output_shape}')

        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=learning_rate)
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=['accuracy'])
        return ModelSettings(model, f'conv_1d:lr{learning_rate}', train_data, cv_data)

    def create_hybrid_model(self, bot_name):
        play_results = create_test_play_results(1, bot_name)
        generator = GameStateGenerator()
        translator = GameStateTranslater()
        game_states = [generator.generate(pr) for pr in play_results]
        examples_1d = translator.convert_to_1d_ant_vision(bot_name, game_states)
        examples_map = translator.convert_to_antmap(bot_name, game_states)

        combined = [(examples_1d[i], examples_map.examples[i]) for i in range(len(examples_1d))]
        random.shuffle(combined)
        train_cutoff = floor(len(combined) * .70)
        train_examples = combined[0:train_cutoff]
        cv_examples = combined[train_cutoff:]

        av_encoded_train = self.encode_3d_examples([y[0] for y in train_examples])
        map_encoded_train = self.encode_map_examples(examples_map.row_count, examples_map.column_count,
                                                     [y[1] for y in train_examples])

        av_encoded_cv = self.encode_3d_examples([y[0] for y in cv_examples])
        map_encoded_cv = self.encode_map_examples(examples_map.row_count, examples_map.column_count,
                                                  [y[1] for y in cv_examples])

        print(av_encoded_train.features.shape)
        print(map_encoded_train.features.shape)

        # define two sets of inputs
        inputA = Input(shape=(av_encoded_train.features.shape[1], av_encoded_train.features.shape[2]))
        inputB = Input(
            shape=(map_encoded_train.features.shape[1], map_encoded_train.features.shape[2],
                   map_encoded_train.features.shape[3]))
        # the first branch operates on the first input
        x = Dense(8, activation="relu")(inputA)
        x = Flatten()(x)
        x = Dense(4, activation="relu")(x)
        x = Model(inputs=inputA, outputs=x)
        # the second branch operates on the second input
        y = Dense(64, activation="relu")(inputB)
        y = Dense(32, activation="relu")(y)
        y = Flatten()(y)
        y = Dense(4, activation="relu")(y)
        y = Model(inputs=inputB, outputs=y)
        # combine the output of the two branches
        combined = concatenate([x.output, y.output])
        z = Dense(av_encoded_train.labels.shape[1], activation=tf.nn.softmax)(combined)
        # our model will accept the inputs of the two branches and
        # then output a single value
        model = Model(inputs=[x.input, y.input], outputs=z)
        for i, l in enumerate(model.layers):
            print(f'Layer {l.name}')
            print(f'In {l.input_shape}')
            print(f'Out {l.output_shape}')
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam()
        log_dir = f'logs/fit/hybrid_{datetime.datetime.now().strftime("%Y%m%d-%H%M%S")}'
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        callbacks = [tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1)]
        model.fit(
            x=[av_encoded_train.features, map_encoded_train.features], y=av_encoded_train.labels,
            callbacks=callbacks,
            validation_data=([av_encoded_cv.features, map_encoded_cv.features], av_encoded_cv.labels),
            epochs=50, batch_size=10)

    def train_model(self, ms: ModelSettings):
        log_dir = f'logs/fit/{ms.model_name}_{datetime.datetime.now().strftime("%Y%m%d-%H%M%S")}'
        # file_writer = tf.summary.create_file_writer(log_dir + "/metrics")
        # file_writer.set_as_default()
        # def log_sin(epoch, logs):
        #    data = sin(epoch)
        #    print(data)
        #    tf.summary.scalar('sin', data=data, step=epoch)
        # log_sin_cb = LambdaCallback(on_epoch_end=log_sin)
        callbacks = [tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1)] if ms.training.features.shape[
                                                                                               0] > 10000 else []
        fit = ms.model.fit(ms.training.features, ms.training.labels,
                           validation_data=(ms.cross_val.features, ms.cross_val.labels),
                           epochs=50,
                           batch_size=10, callbacks=callbacks)

    # @unittest.skip('Integration test')
    def test_training(self):
        bot_to_emulate = 'memetix_1'
        # examples = create_test_examples(2, bot_to_emulate, ExampleType.ANT_VISION)
        # random.shuffle(examples)
        # train_cutoff = floor(len(examples) * .70)
        # train_examples = examples[0:train_cutoff]
        # cv_examples = examples[train_cutoff:]
        model = self.create_hybrid_model(bot_to_emulate)
        # self.train_model(model)
        # default learning rate 0.001
        # model.evaluate(cv_features, cv_labels, verbose=2)

        # prediction0 = model.predict(numpy.array([train_features[0]]))
        # print(prediction0)
        # print(train_labels[0])

        # prediction1 = model.predict(numpy.array([test_features[0]]))
        # print(prediction1)
        # print(test_labels[0])
