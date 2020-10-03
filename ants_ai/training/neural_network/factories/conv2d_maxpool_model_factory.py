from typing import Dict, Union, List

import tensorflow as tf
from ants_ai.training.neural_network.encoders.encoders import TrainingDataset
from kerastuner import HyperParameters
from ants_ai.training.neural_network.factories.model_factory import ModelFactory, EncodingType
from ants_ai.training.neural_network.factories.model_hyper_parameter import ModelHyperParameter
from ants_ai.training.neural_network.factories.hyper_parameter_factory import HyperParameterFactory

from tensorflow.python.keras import Input
from tensorflow.python.keras.layers import Conv2D, Flatten, Dropout, Dense, MaxPooling2D
from tensorflow.python.keras.models import Model, Sequential

FILTER0_NAME = 'filter_0'
FILTER1_NAME = 'filter_1'
FILTER2_NAME = 'filter_2'
MAX_POOL_SIZE0 = 'maxpool_size1'
MAX_POOL_SIZE1 = 'maxpool_size2'
MAX_POOL_SIZE2 = 'maxpool_size3'
DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


# Helpful link
# https://www.quora.com/How-can-I-calculate-the-size-of-output-of-convolutional-layer
class Conv2DMaxPoolModelFactory(ModelFactory):
    def __init__(self, bot_name: str):
        super().__init__('conv_2d_maxpool', bot_name, {
            FILTER0_NAME: ModelHyperParameter(FILTER0_NAME, 32, False),
            FILTER1_NAME: ModelHyperParameter(FILTER1_NAME, 64, False),
            FILTER2_NAME: ModelHyperParameter(FILTER2_NAME, 128, False),
            MAX_POOL_SIZE0: ModelHyperParameter(MAX_POOL_SIZE0, 2, True),
            MAX_POOL_SIZE1: ModelHyperParameter(MAX_POOL_SIZE1, 2, True),
            MAX_POOL_SIZE2: ModelHyperParameter(MAX_POOL_SIZE2, 2, True),
            DENSE_NAME: ModelHyperParameter(DENSE_NAME, 64, False),
            LEARNING_RATE_NAME: ModelHyperParameter(LEARNING_RATE_NAME, 0.001, False)
        })

    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        return self.encode_game_states(game_paths, EncodingType.ANT_VISION_2D)

    def construct_model(self, tuned_params: Dict[str, Union[int, float]], hps: HyperParameters = None) -> Model:
        hpf = HyperParameterFactory(self.default_parameters_values, tuned_params, hps)
        filter_0 = hpf.get_choice(FILTER0_NAME, [4, 8, 16, 32])
        filter_1 = hpf.get_choice(FILTER1_NAME, [32, 48, 64])
        filter_2 = hpf.get_choice(FILTER2_NAME, [64, 96, 128])
        max_pool_0 = hpf.get_choice(MAX_POOL_SIZE0, [1, 2])
        max_pool_1 = hpf.get_choice(MAX_POOL_SIZE1, [1, 2])
        max_pool_2 = hpf.get_choice(MAX_POOL_SIZE2, [1, 2, 4, 8])
        dense = hpf.get_int(DENSE_NAME, lambda default: hps.Int(DENSE_NAME, 32, 128, step=8, default=default))
        lr = hpf.get_choice(LEARNING_RATE_NAME, [1e-2, 1e-3, 1e-4])

        model = Sequential([
            Input(name='Input', shape=(12, 12, 7)),
            Conv2D(filter_0, 2, strides=1, activation=tf.nn.relu, name='Conv2D_0'),
            MaxPooling2D(max_pool_0, name='MaxPool_0'),

            Conv2D(filter_1, 3, strides=1, activation=tf.nn.relu, name='Conv2D_1'),
            MaxPooling2D(max_pool_1, name='MaxPool_1'),
            # Conv2D(self.get_param_value(FILTER2_NAME, tuned_params), 2, strides=1, activation=tf.nn.relu,
            #       name='Conv2D_2'),
            # MaxPooling2D(self.get_param_value(MAX_POOL_SIZE2, tuned_params), name='MaxPool_2'),
            Flatten(name='Flatten'),
            Dropout(0.1, name='Dropout'),
            Dense(dense, activation=tf.nn.relu, name='dense'),
            Dense(5, activation=tf.nn.softmax, name='Output'),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=lr)
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model
