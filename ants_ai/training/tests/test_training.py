# disables GPUs
import datetime
import os
from typing import List

from tensorflow.python.keras.utils.data_utils import Sequence
from training.game_state.game_state import GameState
from training.game_state.generator import GameStateGenerator

import training.neural_network.model_factory as mf
from training.neural_network.game_state_translator import GameStateTranslator
from training.tests.test_utils import create_test_game_states

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
import tensorflow as tf

import unittest
from tensorflow.python.keras.callbacks import History, LambdaCallback

import matplotlib.pyplot as plt
import pprint

pp = pprint.PrettyPrinter(indent=4)


class TestTraining(unittest.TestCase):

    def train_model(self, game_states: List[GameState], ms: mf.ModelSettings):
        log_dir = f'logs/fit/{ms.model_name}_{datetime.datetime.now().strftime("%Y%m%d-%H%M%S")}'

        file_writer = tf.summary.create_file_writer(log_dir + "/metrics")
        file_writer.set_as_default()

        def log_model_params(epoch, logs):
            for key in ms.model_params.keys():
                tf.summary.scalar(key, data=ms.model_params[key], step=epoch)

        log_model_params_cb = LambdaCallback(on_epoch_end=log_model_params)
        callbacks = [
            tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1), log_model_params_cb]
        (train, cross_val) = ms.encode_game_states(game_states)
        fit = ms.model.fit(train.features, train.labels,
                           validation_data=(cross_val.features, cross_val.labels),
                           epochs=10,
                           batch_size=10, callbacks=callbacks)

    # @unittest.skip('Integration test')
    def test_training(self):
        bot_to_emulate = 'memetix_1'
        # examples = create_test_examples(2, bot_to_emulate, ExampleType.ANT_VISION)

        game_states = create_test_game_states(5, bot_to_emulate)

        models = [
            mf.create_dense_2d_model(0.0005, 1, bot_to_emulate),
            # mf.create_conv_2d_model(0.001, bot_to_emulate),
            # mf.create_conv_2d_model(0.002, bot_to_emulate),
            # mf.create_conv_2d_model(0.01, bot_to_emulate),

        ]
        for model in models:
            self.train_model(game_states, model)
        # default learning rate 0.001
        # model.evaluate(cv_features, cv_labels, verbose=2)

        # prediction0 = model.predict(numpy.array([train_features[0]]))
        # print(prediction0)
        # print(train_labels[0])

        # prediction1 = model.predict(numpy.array([test_features[0]]))
        # print(prediction1)
        # print(test_labels[0])
