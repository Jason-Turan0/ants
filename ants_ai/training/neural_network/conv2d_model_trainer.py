from typing import Dict, Union, List

import tensorflow as tf
from encoders import TrainingDataset
from kerastuner import HyperParameters
from ants_ai.training.neural_network.model_trainer import ModelTrainer, EncodingType
from tensorflow.python.keras import Input
from tensorflow.python.keras.layers import Conv2D, Flatten, Dropout, Dense
from tensorflow.python.keras.models import Model, Sequential

FILTER0_NAME = 'filter_0'
FILTER1_NAME = 'filter_1'
FILTER2_NAME = 'filter_2'
DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


class Conv2DModelTrainer(ModelTrainer):
    def __init__(self, bot_name: str):
        super().__init__('conv_2d', bot_name)

    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        return super().encode_game_states(game_paths, EncodingType.ANT_VISION_2D)

    def construct_model(self, model_params: Dict[str, Union[int, float]]) -> Model:
        def get_or_default(name: str, default_val):
            val = model_params.get(name)
            if val is None:
                print(f'Failed to find hyper parameter {name}')
                return default_val
            return val

        model = Sequential([
            Input(name='Input',
                  shape=(12, 12, 7)),
            Conv2D(get_or_default(FILTER0_NAME, 32), 2, strides=1, activation=tf.nn.relu, name='Conv2D_1_32'),
            Conv2D(get_or_default(FILTER1_NAME, 64), 3, strides=1, activation=tf.nn.relu, name='Conv2D_2_64'),
            Conv2D(get_or_default(FILTER2_NAME, 128), 2, strides=1, activation=tf.nn.relu, name='Conv2D_3_128'),
            Flatten(name='Flatten'),
            Dropout(0.1, name='Dropout'),
            Dense(get_or_default(DENSE_NAME, 64), activation=tf.nn.relu, name='dense'),
            Dense(5, activation=tf.nn.softmax, name='Output'),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=get_or_default(LEARNING_RATE_NAME, 1e-2))
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model

    def construct_discover_model(self, hp: HyperParameters) -> Model:
        filter_0 = hp.Choice(FILTER0_NAME, values=[4, 8, 16, 32], default=32)
        filter_1 = hp.Choice(FILTER1_NAME, values=[32, 48, 64], default=64)
        filter_2 = hp.Choice(FILTER2_NAME, values=[64, 96, 128], default=128)
        dense = hp.Int(DENSE_NAME, 32, 128, step=8, default=64)
        hp_learning_rate = hp.Choice(LEARNING_RATE_NAME, values=[1e-2, 1e-3, 1e-4], default=0.001)
        model = Sequential([
            Input(name='Input',
                  shape=(12, 12, 7)),
            Conv2D(filter_0, 2, strides=1, activation=tf.nn.relu, name='Conv2D_1'),
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
