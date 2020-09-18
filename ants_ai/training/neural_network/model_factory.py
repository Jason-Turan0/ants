import multiprocessing as mp
from math import floor
from random import shuffle
from typing import List, Tuple, Callable, Dict, Union

import tensorflow as tf
from tensorflow.keras.layers import Dense
from tensorflow.python.keras import Sequential, Model, Input
from tensorflow.python.keras.layers import Dropout, Flatten, concatenate, Conv2D, MaxPooling2D
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.encoders import TrainingDataset, encode_2d_examples, \
    encode_map_examples
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.neural_network_example import AntVision2DExample, AntMapExample


class ModelTrainer:
    def __init__(self, model: Model, model_name: str,
                 game_state_encoder: Callable[[List[GameState]], Tuple[TrainingDataset, TrainingDataset]],
                 model_params: Dict[str, Union[float, int]]):
        self.model_name = model_name
        self.game_state_encoder = game_state_encoder
        self.model = model
        self.model_params = model_params

    def encode_game_states(self, game_states: List[GameState]) -> Tuple[TrainingDataset, TrainingDataset]:
        return self.game_state_encoder(game_states)


def shuffle_and_split(examples: List):
    shuffle(examples)
    train_cutoff = floor(len(examples) * .70)
    train_examples = examples[0:train_cutoff]
    cv_examples = examples[train_cutoff:]
    return train_examples, cv_examples


def print_layers(model: Model):
    for i, l in enumerate(model.layers):
        print(f'{i}. {l.name}')
        print(f'In {l.input_shape}')
        print(f'Out {l.output_shape}')


def map_to_input(task: Tuple[str, str, GameState]):
    bot_name, translator_method_name, gs = task
    t = GameStateTranslator()
    m = getattr(t, translator_method_name)
    return m(bot_name, [gs])


def create_test_examples(bot_name: str, translator_method_name: str, game_states: List[GameState]):
    tasks = [(bot_name, translator_method_name, gs)
             for gs in game_states]
    pool = mp.Pool(mp.cpu_count() - 1)
    results = pool.map(map_to_input, tasks)
    pool.close()
    examples = [item for sublist in results for item in sublist]
    print(f'Generated {len(examples)} examples')
    return examples


def create_game_state_2d_encoder(bot_name: str, number_of_channels: int):
    def game_state_encoder(game_states: List[GameState]) -> Tuple[TrainingDataset, TrainingDataset]:
        examples: List[AntVision2DExample] = create_test_examples(bot_name, 'convert_to_2d_ant_vision', game_states)
        (train_examples, cv_examples) = shuffle_and_split(examples)
        train_data = encode_2d_examples(train_examples, number_of_channels)
        cv_data = encode_2d_examples(cv_examples, number_of_channels)
        return train_data, cv_data

    return game_state_encoder


# Helpful link
# https://www.quora.com/How-can-I-calculate-the-size-of-output-of-convolutional-layer
def create_conv_2d_model(learning_rate: float, strides: int, bot_name: str, number_of_channels: int) -> ModelTrainer:
    model = Sequential([
        Input(name='Input',
              shape=(12, 12, number_of_channels)),
        Conv2D(32, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_1_32'),
        Conv2D(64, 3, strides=strides, activation=tf.nn.relu, name='Conv2D_2_64'),
        Conv2D(128, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_3_128'),
        Flatten(name='Flatten'),
        Dropout(0.1, name='Dropout'),
        Dense(64, activation=tf.nn.relu, name='dense'),
        Dense(5, activation=tf.nn.softmax, name='Output'),
    ])
    loss_fn = tf.keras.losses.CategoricalCrossentropy()
    opt = tf.keras.optimizers.Adam(learning_rate=learning_rate)
    model.compile(optimizer=opt,
                  loss=loss_fn,
                  metrics=[tf.keras.metrics.categorical_accuracy])
    return ModelTrainer(model, 'conv_2d', create_game_state_2d_encoder(bot_name, number_of_channels),
                        {'strides': strides, 'learning_rate': learning_rate, 'number_of_channels': number_of_channels})


def create_dense_2d_model(learning_rate: float, bot_name: str):
    model = Sequential([
        Input(shape=(12, 12, 7)),
        Dense(64, activation=tf.nn.relu),
        Flatten(),
        Dropout(0.1),
        Dense(5, activation=tf.nn.softmax),
    ])

    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
                  loss=tf.keras.losses.CategoricalCrossentropy(),
                  metrics=[tf.keras.metrics.categorical_accuracy])
    return ModelTrainer(model, 'dense_2d', create_game_state_2d_encoder(bot_name, 7), {'learning_rate': learning_rate})


def create_hybrid_model(learning_rate: float, strides: int, bot_name: str, channel_count: int):
    input_ant_view = Input(shape=(12, 12, channel_count), name='input_ant_view')
    avm = Conv2D(32, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_av1_32')(input_ant_view)
    avm = Conv2D(64, 3, strides=strides, activation=tf.nn.relu, name='Conv2D_av2_64')(avm)
    avm = Conv2D(128, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_av3_128')(avm)
    avm = Flatten(name='Flatten_av')(avm)
    avm = Dense(64, activation=tf.nn.relu, name='Dense_av')(avm)
    avm = Model(inputs=input_ant_view, outputs=avm)

    input_map_view = Input(shape=(43, 39, channel_count), name='input_map_view')
    mvm = MaxPooling2D(2, name='MaxPool_mv')(input_map_view)
    mvm = Conv2D(32, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_mv1_32')(mvm)
    mvm = Conv2D(64, 3, strides=strides, activation=tf.nn.relu, name='Conv2D_mv2_64')(mvm)
    mvm = Conv2D(128, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_mv3_128')(mvm)
    mvm = Flatten(name='Flatten_mv')(mvm)
    mvm = Dense(64, activation=tf.nn.relu, name='Dense_wmv')(mvm)
    mvm = Model(inputs=input_map_view, outputs=mvm)
    combined = concatenate([avm.output, mvm.output])
    output = Dense(5, activation=tf.nn.softmax)(combined)
    model = Model(inputs=[avm.input, mvm.input], outputs=output)
    print_layers(model)

    loss_fn = tf.keras.losses.CategoricalCrossentropy()
    opt = tf.keras.optimizers.Adam(learning_rate=learning_rate)
    model.compile(optimizer=opt,
                  loss=loss_fn,
                  metrics=[tf.keras.metrics.categorical_accuracy])

    def encoder(game_states: List[GameState]) -> Tuple[TrainingDataset, TrainingDataset]:
        examples_2d: List[AntVision2DExample] = create_test_examples(bot_name, 'convert_to_2d_ant_vision', game_states)
        examples_map: List[AntMapExample] = create_test_examples(bot_name, 'convert_to_antmap', game_states)
        combined = [(examples_2d[i], examples_map[i]) for i in range(len(examples_2d))]
        (train_examples, cv_examples) = shuffle_and_split(combined)
        av_encoded_train = encode_2d_examples([y[0] for y in train_examples], channel_count)
        map_encoded_train = encode_map_examples([y[1] for y in train_examples], channel_count)
        av_encoded_cv = encode_2d_examples([y[0] for y in cv_examples], channel_count)
        map_encoded_cv = encode_map_examples([y[1] for y in cv_examples], channel_count)
        return (TrainingDataset([av_encoded_train.features, map_encoded_train.features], av_encoded_train.labels),
                TrainingDataset([av_encoded_cv.features, map_encoded_cv.features], av_encoded_cv.labels))

    return ModelTrainer(model, 'hybrid_2d', encoder,
                        {'learning_rate': learning_rate, 'strides': strides, 'channel_count': channel_count})
