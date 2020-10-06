from typing import List, Dict, Union

import tensorflow as tf
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.factories.hyper_parameter_factory import HyperParameterFactory
from ants_ai.training.neural_network.factories.model_factory import ModelFactory
from ants_ai.training.neural_network.factories.model_hyper_parameter import ModelHyperParameter
from ants_ai.training.neural_network.factories.conv2d_model_factory import Conv2DModelFactory
from ants_ai.training.neural_network.factories.map_view_model_factory import MapViewModelFactory
from ants_ai.training.neural_network.sequences.combined_sequence import CombinedSequence

from kerastuner import HyperParameters
from tensorflow.keras.layers import Dense
from tensorflow.python.keras import Model, Input
from tensorflow.python.keras.layers import Flatten, concatenate, Conv2D, MaxPooling2D

DENSE_NAME = 'dense'
LEARNING_RATE_NAME = 'learning_rate'


class CombinedModelFactory(ModelFactory):
    def __init__(self, bot_name: str, conv2d_weight_path: str, map_view_weight_path: str):
        super().__init__('combined_2d', bot_name, {
            DENSE_NAME: ModelHyperParameter(DENSE_NAME, 64, False),
            LEARNING_RATE_NAME: ModelHyperParameter(LEARNING_RATE_NAME, 0.001, False)
        })
        self.conv_2d_model_factory = Conv2DModelFactory(bot_name)
        self.map_view_model_factory = MapViewModelFactory(bot_name)
        self.channel_count = 7
        self.conv2d_weight_path = conv2d_weight_path
        self.map_view_weight_path = map_view_weight_path

    def create_sequence(self, game_paths: List[str], batch_size: int) -> FileSystemSequence:
        return CombinedSequence(game_paths, batch_size, self.bot_name, self.channel_count)

    def construct_model(self, tuned_params: Dict[str, Union[int, float]], hps: HyperParameters = None) -> Model:
        hpf = HyperParameterFactory(self.default_parameters_values, tuned_params, hps)
        dense = hpf.get_int(DENSE_NAME, 32, 128, step=8)
        hp_learning_rate = hpf.get_choice(LEARNING_RATE_NAME, [1e-2, 1e-3, 1e-4])

        conv_2d_model = self.conv_2d_model_factory.construct_model(tuned_params, hps)
        conv_2d_model.load_weights(self.conv2d_weight_path)
        conv_2d_model.trainable = False

        map_view_model = self.map_view_model_factory.construct_model(tuned_params, hps)
        map_view_model.load_weights(self.map_view_weight_path)
        map_view_model.trainable = False

        combined_model = Flatten(name='Combined_Flatten')(conv_2d_model.input)
        combined_model = Dense(dense, activation=tf.nn.relu, name='Combined_Dense0')(combined_model)
        combined_model = concatenate([combined_model, conv_2d_model.output, map_view_model.output])
        combined_model = Dense(dense, activation=tf.nn.relu, name='Combined_Dense1')(combined_model)
        output = Dense(5, activation=tf.nn.relu)(combined_model)
        combined_model = Model(inputs=[conv_2d_model.input, map_view_model.input], outputs=output)

        loss_fn = tf.keras.losses.CategoricalCrossentropy()
        opt = tf.keras.optimizers.Adam(learning_rate=hp_learning_rate)
        combined_model.compile(optimizer=opt,
                               loss=loss_fn,
                               metrics=[tf.keras.metrics.categorical_accuracy])
        return combined_model
