from math import floor
from pprint import pprint
from random import random, shuffle
from typing import List, Tuple, Callable, Dict

from training.game_state.game_state import GameState
from training.game_state.generator import GameStateGenerator
from training.neural_network.encoders import TrainingDataset, encode_1d_examples, encode_2d_examples, \
    encode_flat_examples, encode_map_examples
from tensorflow.python.keras import Sequential, Model, Input
from tensorflow.python.keras.layers import Dropout, Flatten, Conv1D, concatenate, Conv2D, MaxPooling2D
from training.neural_network.game_state_translator import GameStateTranslator
from training.neural_network.neural_network_example import AntVision2DExample
from training.tests.test_utils import create_test_play_results
import tensorflow as tf
import datetime
import multiprocessing as mp
from tensorflow.keras.layers import Dense


class ModelSettings:
    def __init__(self, model: Model, model_name: str,
                 game_state_encoder: Callable[[List[GameState]], Tuple[TrainingDataset, TrainingDataset]],
                 model_params: Dict[str, float]):
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


def create_game_state_2d_encoder(bot_name: str):
    def game_state_encoder(game_states: List[GameState]) -> Tuple[TrainingDataset, TrainingDataset]:
        examples: List[AntVision2DExample] = create_test_examples(bot_name, 'convert_to_2d_ant_vision', game_states)
        (train_examples, cv_examples) = shuffle_and_split(examples)
        train_data = encode_2d_examples(train_examples)
        cv_data = encode_2d_examples(cv_examples)
        return train_data, cv_data

    return game_state_encoder


# Helpful link
# https://www.quora.com/How-can-I-calculate-the-size-of-output-of-convolutional-layer
def create_conv_2d_model(learning_rate: float, strides: int, bot_name: str) -> ModelSettings:
    model = Sequential([
        Input(name='Input',
              shape=(12, 12, 7)),
        Conv2D(32, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_1_32'),
        # MaxPooling2D(pool_size=(2, 2), name='MaxPooling2D_1', padding="same"),
        Conv2D(64, 3, strides=strides, activation=tf.nn.relu, name='Conv2D_2_64'),
        # MaxPooling2D(pool_size=(2, 2), name='MaxPooling2D_2', padding="same"),
        Conv2D(128, 2, strides=strides, activation=tf.nn.relu, name='Conv2D_3_128'),
        # MaxPooling2D(pool_size=(2, 2), name='MaxPooling2D_3', padding="same"),
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
    return ModelSettings(model, f'conv_2d', create_game_state_2d_encoder(bot_name),
                         {'strides': strides, 'learning_rate': learning_rate})


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
    return ModelSettings(model, 'dense_2d', create_game_state_2d_encoder(bot_name), {'learning_rate': learning_rate})


def create_hybrid_model(bot_name):
    play_results = create_test_play_results(1, bot_name)
    generator = GameStateGenerator()
    translator = GameStateTranslator()
    game_states = [generator.generate(pr) for pr in play_results]
    examples_1d = translator.convert_to_1d_ant_vision(bot_name, game_states)
    examples_map = translator.convert_to_antmap(bot_name, game_states)

    combined = [(examples_1d[i], examples_map.examples[i]) for i in range(len(examples_1d))]
    shuffle(combined)
    train_cutoff = floor(len(combined) * .70)
    train_examples = combined[0:train_cutoff]
    cv_examples = combined[train_cutoff:]

    av_encoded_train = encode_1d_examples([y[0] for y in train_examples])
    map_encoded_train = encode_map_examples(examples_map.row_count, examples_map.column_count,
                                            [y[1] for y in train_examples])

    av_encoded_cv = encode_1d_examples([y[0] for y in cv_examples])
    map_encoded_cv = encode_map_examples(examples_map.row_count, examples_map.column_count,
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
