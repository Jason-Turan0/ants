from typing import List, Dict, Union

import tensorflow as tf
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.encoders import TrainingDataset
from ants_ai.training.neural_network.hyper_parameter_factory import HyperParameterFactory
from ants_ai.training.neural_network.model_factory import ModelFactory, EncodingType
from ants_ai.training.neural_network.model_hyper_parameter import ModelHyperParameter
from ants_ai.training.neural_network.sequences.hybrid_sequence import HybridSequence
from kerastuner import HyperParameters
from tensorflow.keras.layers import Dense
from tensorflow.python.keras import Model, Input
from tensorflow.python.keras.layers import Flatten, concatenate, Conv2D, MaxPooling2D

MAXPOOL_NAME = 'max_pool_0'
FILTER0_NAME = 'filter_0'
FILTER1_NAME = 'filter_1'
FILTER2_NAME = 'filter_2'
DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


class HybridModelFactory(ModelFactory):
    def __init__(self, bot_name: str):
        super().__init__('hybrid_2d', bot_name, {
            MAXPOOL_NAME: ModelHyperParameter(MAXPOOL_NAME, 2, True),
            FILTER0_NAME: ModelHyperParameter(FILTER0_NAME, 32, True),
            FILTER1_NAME: ModelHyperParameter(FILTER1_NAME, 64, True),
            FILTER2_NAME: ModelHyperParameter(FILTER2_NAME, 128, False),
            DENSE_NAME: ModelHyperParameter(DENSE_NAME, 64, False),
            LEARNING_RATE_NAME: ModelHyperParameter(LEARNING_RATE_NAME, 0.001, False)
        })
        self.channel_count = 7

    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        return self.encode_game_states(game_paths, EncodingType.MAP_2D)

    def create_sequence(self, game_paths: List[str], batch_size: int) -> FileSystemSequence:
        return HybridSequence(game_paths, batch_size, self.bot_name, self.channel_count)

    def construct_model(self, tuned_params: Dict[str, Union[int, float]], hps: HyperParameters = None) -> Model:
        hpf = HyperParameterFactory(self.default_parameters_values, tuned_params, hps)
        max_pool = hpf.get_choice(MAXPOOL_NAME, [1, 2, 4, 8])
        filter_0 = hpf.get_choice(FILTER0_NAME, [4, 8, 16, 32])
        filter_1 = hpf.get_choice(FILTER1_NAME, [32, 48, 64])
        filter_2 = hpf.get_choice(FILTER2_NAME, [64, 96, 128])
        dense = hpf.get_int(DENSE_NAME, 32, 128, step=8)
        hp_learning_rate = hpf.get_choice(LEARNING_RATE_NAME, [1e-2, 1e-3, 1e-4])

        input_ant_view = Input(shape=(12, 12, 7), name='input_ant_view')
        avm = Conv2D(filter_0, 2, strides=1, activation=tf.nn.relu, name='Conv2D_av1_32')(input_ant_view)
        avm = Conv2D(filter_1, 3, strides=1, activation=tf.nn.relu, name='Conv2D_av2_64')(avm)
        avm = Conv2D(filter_2, 2, strides=1, activation=tf.nn.relu, name='Conv2D_av3_128')(avm)
        avm = Flatten(name='Flatten_av')(avm)
        avm = Dense(dense, activation=tf.nn.relu, name='Dense_av')(avm)
        avm = Model(inputs=input_ant_view, outputs=avm)

        input_map_view = Input(shape=(43, 39, 7), name='input_map_view')
        mvm = MaxPooling2D(max_pool, name='MaxPool_mv')(input_map_view)
        mvm = Conv2D(filter_0, 2, strides=1, activation=tf.nn.relu, name='Conv2D_mv1_32')(mvm)
        mvm = Conv2D(filter_1, 3, strides=1, activation=tf.nn.relu, name='Conv2D_mv2_64')(mvm)
        mvm = Conv2D(filter_2, 2, strides=1, activation=tf.nn.relu, name='Conv2D_mv3_128')(mvm)
        mvm = Flatten(name='Flatten_mv')(mvm)
        mvm = Dense(dense, activation=tf.nn.relu, name='Dense_wmv')(mvm)
        mvm = Model(inputs=input_map_view, outputs=mvm)
        combined = concatenate([avm.output, mvm.output])
        output = Dense(5, activation=tf.nn.softmax)(combined)
        model = Model(inputs=[avm.input, mvm.input], outputs=output)

        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=hp_learning_rate)
        model.compile(optimizer=opt,
                      loss=loss_fn,
                      metrics=[tf.keras.metrics.categorical_accuracy])
        return model
