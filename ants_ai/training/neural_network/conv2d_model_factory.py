from typing import Dict, Union, List, Callable

import tensorflow as tf
from encoders import TrainingDataset
from kerastuner import HyperParameters
from ants_ai.training.neural_network.model_factory import ModelFactory, EncodingType
from ants_ai.training.neural_network.model_hyper_parameter import ModelHyperParameter
from tensorflow.python.keras import Input
from tensorflow.python.keras.layers import Conv2D, Flatten, Dropout, Dense
from tensorflow.python.keras.models import Model, Sequential

FILTER0_NAME = 'filter_0'
FILTER1_NAME = 'filter_1'
FILTER2_NAME = 'filter_2'
DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


# Helpful link
# https://www.quora.com/How-can-I-calculate-the-size-of-output-of-convolutional-layer
class Conv2DModelFactory(ModelFactory):
    def __init__(self, bot_name: str):
        super().__init__('conv_2d', bot_name, {
            FILTER0_NAME: ModelHyperParameter(FILTER0_NAME, 32, True),
            FILTER1_NAME: ModelHyperParameter(FILTER1_NAME, 64, True),
            FILTER2_NAME: ModelHyperParameter(FILTER2_NAME, 128, False),
            DENSE_NAME: ModelHyperParameter(DENSE_NAME, 64, False),
            LEARNING_RATE_NAME: ModelHyperParameter(LEARNING_RATE_NAME, 0.001, False)
        })

    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        return self.encode_game_states(game_paths, EncodingType.ANT_VISION_2D)

    def construct_model(self, tuned_params: Dict[str, Union[int, float]]) -> Model:
        model = Sequential([
            Input(name='Input',
                  shape=(12, 12, 7)),
            Conv2D(self.get_param_value(FILTER0_NAME, tuned_params), 2, strides=1, activation=tf.nn.relu,
                   name='Conv2D_1'),
            Conv2D(self.get_param_value(FILTER1_NAME, tuned_params), 3, strides=1, activation=tf.nn.relu,
                   name='Conv2D_2'),
            Conv2D(self.get_param_value(FILTER2_NAME, tuned_params), 2, strides=1, activation=tf.nn.relu,
                   name='Conv2D_3'),
            Flatten(name='Flatten'),
            Dropout(0.1, name='Dropout'),
            Dense(self.get_param_value(DENSE_NAME, tuned_params), activation=tf.nn.relu, name='dense'),
            Dense(5, activation=tf.nn.softmax, name='Output'),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=self.get_param_value(LEARNING_RATE_NAME, tuned_params))
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model

    def construct_discover_model(self, hp: HyperParameters) -> Model:
        filter_0 = self.get_hyper_param(FILTER0_NAME,
                                        lambda default: hp.Choice(FILTER0_NAME, values=[4, 8, 16, 32], default=default))
        filter_1 = self.get_hyper_param(FILTER1_NAME, lambda default: hp.Choice(FILTER1_NAME, values=[32, 48, 64],
                                                                                default=default))
        filter_2 = self.get_hyper_param(FILTER2_NAME,
                                        lambda default: hp.Choice(FILTER2_NAME, values=[64, 96, 128], default=default))
        dense = self.get_hyper_param(DENSE_NAME, lambda default: hp.Int(DENSE_NAME, 32, 128, step=8, default=default))
        hp_learning_rate = self.get_hyper_param(LEARNING_RATE_NAME,
                                                lambda default: hp.Choice(LEARNING_RATE_NAME, values=[1e-2, 1e-3, 1e-4],
                                                                          default=default))
        model = Sequential([
            Input(name='Input',
                  shape=(12, 12, 7)),
            Conv2D(filter_0, 2, strides=1,
                   activation=tf.nn.relu, name='Conv2D_1'),
            Conv2D(filter_1, 3, strides=1, activation=tf.nn.relu, name='Conv2D_2'),
            Conv2D(filter_2, 2, strides=1, activation=tf.nn.relu, name='Conv2D_3'),
            Flatten(name='Flatten'),
            Dropout(0.05, name='Dropout'),
            Dense(dense, activation=tf.nn.relu, name='dense'),
            Dense(5, activation=tf.nn.softmax, name='Output'),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=hp_learning_rate)
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model
