from typing import List, Dict, Union

import tensorflow as tf
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.encoders.encoders import TrainingDataset
from ants_ai.training.neural_network.factories.hyper_parameter_factory import HyperParameterFactory
from ants_ai.training.neural_network.factories.model_factory import ModelFactory, EncodingType
from ants_ai.training.neural_network.factories.model_hyper_parameter import ModelHyperParameter
from ants_ai.training.neural_network.sequences.map_view_sequence import MapViewSequence
from kerastuner import HyperParameters
from tensorflow.keras.layers import Dense
from tensorflow.python.keras import Model, Input
from tensorflow.python.keras.layers import Flatten, concatenate, Conv2D, MaxPooling2D
from tensorflow_core.python.keras import Sequential
from tensorflow_core.python.keras.layers import Dropout

MAXPOOL0_NAME = 'max_pool_0'
MAXPOOL1_NAME = 'max_pool_1'
MAXPOOL2_NAME = 'max_pool_2'
FILTER0_NAME = 'filter_0'
FILTER1_NAME = 'filter_1'
FILTER2_NAME = 'filter_2'
DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


class MapViewModelFactory(ModelFactory):
    def __init__(self, bot_name: str):
        super().__init__('mapview_2d', bot_name, {
            MAXPOOL0_NAME: ModelHyperParameter(MAXPOOL0_NAME, 2, True),
            MAXPOOL1_NAME: ModelHyperParameter(MAXPOOL1_NAME, 2, True),
            MAXPOOL2_NAME: ModelHyperParameter(MAXPOOL2_NAME, 2, True),
            FILTER0_NAME: ModelHyperParameter(FILTER0_NAME, 32, False),
            FILTER1_NAME: ModelHyperParameter(FILTER1_NAME, 64, False),
            FILTER2_NAME: ModelHyperParameter(FILTER2_NAME, 128, False),
            DENSE_NAME: ModelHyperParameter(DENSE_NAME, 64, False),
            LEARNING_RATE_NAME: ModelHyperParameter(LEARNING_RATE_NAME, 0.001, False)
        })
        self.channel_count = 7

    def create_sequence(self, game_paths: List[str], batch_size: int) -> FileSystemSequence:
        return MapViewSequence(game_paths, batch_size, self.bot_name, 7)

    def construct_model(self, tuned_params: Dict[str, Union[int, float]], hps: HyperParameters = None) -> Model:
        hpf = HyperParameterFactory(self.default_parameters_values, tuned_params, hps)
        max_pool0 = hpf.get_choice(MAXPOOL0_NAME, [1, 2, 4, 8])
        max_pool1 = hpf.get_choice(MAXPOOL1_NAME, [1, 2, 4, 8])
        max_pool2 = hpf.get_choice(MAXPOOL2_NAME, [1, 2, 4, 8])
        filter_0 = hpf.get_choice(FILTER0_NAME, [4, 8, 16, 32])
        filter_1 = hpf.get_choice(FILTER1_NAME, [32, 48, 64])
        filter_2 = hpf.get_choice(FILTER2_NAME, [64, 96, 128])
        dense = hpf.get_int(DENSE_NAME, 32, 128, 8)
        lr = hpf.get_choice(LEARNING_RATE_NAME, [1e-2, 1e-3, 1e-4])

        model = Sequential([
            Input(name='MapView_Input', shape=(43, 39, 7)),
            MaxPooling2D(max_pool0, name='MapView_MaxPool_0'),
            Conv2D(filter_0, 2, strides=1, activation=tf.nn.relu, name='MapView_Conv2D_1'),
            MaxPooling2D(max_pool1, name='MapView_MaxPool_1'),
            Conv2D(filter_1, 3, strides=1, activation=tf.nn.relu, name='MapView_Conv2D_2'),
            MaxPooling2D(max_pool2, name='MapView_MaxPool_2'),
            Conv2D(filter_2, 2, strides=1, activation=tf.nn.relu, name='MapView_Conv2D_3'),
            Flatten(name='MapView_Flatten'),
            Dropout(0.1, name='MapView_Dropout'),
            Dense(dense, activation=tf.nn.relu, name='MapView_Dense'),
            Dense(5, activation=tf.nn.softmax, name='MapView_Output'),
        ])
        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=lr)
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model
