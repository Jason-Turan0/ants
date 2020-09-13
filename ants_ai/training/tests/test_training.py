# disables GPUs
import datetime
import glob
import os
from typing import List, Dict, Union

import jsonpickle
import numpy
from functional import seq
from numpy.core.multiarray import ndarray
from tensorflow.python.keras import Model
from tensorflow.python.keras.utils.data_utils import Sequence
from training.game_state.game_state import GameState
from training.game_state.generator import GameStateGenerator
import uuid
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


class LayerStats:
    def __init__(self, layer_name: str, input_shape: tuple, output_shape: tuple):
        self.layer_name = layer_name
        self.input_shape = input_shape
        self.output_shape = output_shape


class RunStats:
    def __init__(self, model_name: str, model: Model, train: Union[ndarray, List[ndarray]],
                 cross_val: Union[ndarray, List[ndarray]], epochs: int, batch_size: int,
                 model_params: Dict[str, float], history: Dict):

        self.model_name = model_name
        self.layer_shapes: List[LayerStats] = seq(model.layers).map(
            lambda layer: LayerStats(layer.name, layer.input_shape, layer.output_shape)) \
            .to_list()

        self.train_shape = self.extract_size(train)
        self.cross_val_shape = self.extract_size(cross_val)
        self.epochs = epochs
        self.batch_size = batch_size
        self.model_params = model_params
        self.history = history

    def extract_size(self, item: Union[ndarray, List[ndarray]]) -> List[tuple]:
        if (isinstance(item, list)):
            return list(map(lambda a: a.shape, item))
        else:
            return [item.shape]


def read_file_contents(path):
    with open(path, "r") as f:
        return f.read()


def plot_learning_curve(data):
    # summarize history for accuracy
    plt.plot([d[0] for d in data], [d[1] for d in data])
    plt.plot([d[0] for d in data], [d[2] for d in data])
    plt.title('Learning curve')
    plt.ylabel('Loss')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation'], loc='upper left')
    plt.show()

    plt.plot([d[0] for d in data], [d[3] for d in data])
    plt.plot([d[0] for d in data], [d[4] for d in data])
    plt.title('Learning curve')
    plt.ylabel('Categorical Accuracy')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation'], loc='upper left')
    plt.show()


class TestTraining(unittest.TestCase):

    def train_model(self, game_states: List[GameState], ms: mf.ModelSettings):
        log_dir = f'logs/fit/{ms.model_name}_{datetime.datetime.now().strftime("%Y%m%d-%H%M%S")}'
        run_stats_path = f'{log_dir}/run_stats.json'
        model_weights_path = f'{log_dir}/{ms.model_name}_weights_{uuid.uuid4()}'
        # file_writer = tf.summary.create_file_writer(log_dir + "/metrics")
        # file_writer.set_as_default()

        # def log_model_params(epoch, logs):
        #    for key in ms.model_params.keys():
        #        tf.summary.scalar(key, data=ms.model_params[key], step=epoch)

        # log_model_params_cb = LambdaCallback(on_epoch_end=log_model_params)
        model_checkpoint_callback = tf.keras.callbacks.ModelCheckpoint(
            filepath=model_weights_path,
            save_weights_only=True,
            monitor='val_categorical_accuracy',
            mode='max',
            save_best_only=True)
        callbacks = [
            tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1),
            model_checkpoint_callback]
        (train, cross_val) = ms.encode_game_states(game_states)
        epochs = 20
        batch_size = 50
        fit = ms.model.fit(train.features, train.labels,
                           validation_data=(cross_val.features, cross_val.labels),
                           epochs=epochs,
                           callbacks=callbacks,
                           batch_size=batch_size)
        stats = RunStats(ms.model_name,
                         ms.model,
                         train.features,
                         cross_val.features,
                         epochs,
                         batch_size,
                         ms.model_params,
                         fit.history)
        with open(run_stats_path, 'w') as stream:
            stream.write(jsonpickle.encode(stats))

    @unittest.skip('Skipped')
    def test_training(self):
        bot_to_emulate = 'memetix_1'
        # examples = create_test_examples(2, bot_to_emulate, ExampleType.ANT_VISION)

        # training_set_sizes = [1, 2, 10, 20, 50, 100, 150, 180]

        # for set_size in training_set_sizes:
        game_states = create_test_game_states(400, bot_to_emulate)
        ms = mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 7)
        self.train_model(game_states, ms)

        models = [
            # mf.create_hybrid_model(0.0005, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.001, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.002, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.0005, 1, bot_to_emulate, 7),
            # mf.create_hybrid_model(0.001, 1, bot_to_emulate, 7),
            # mf.create_hybrid_model(0.002, 1, bot_to_emulate, 7),
            # mf.create_conv_2d_model(0.0005, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.002, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.0005, 1, bot_to_emulate, 7),

            # mf.create_conv_2d_model(0.002, 1, bot_to_emulate, 7),
        ]

        # models = [
        #    mf.create_dense_2d_model(0.0005, bot_to_emulate),
        #    mf.create_dense_2d_model(0.001, bot_to_emulate),
        #    mf.create_dense_2d_model(0.002, bot_to_emulate),
        #    mf.create_dense_2d_model(0.01, bot_to_emulate),

        #    mf.create_conv_2d_model(0.0005, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.001, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.002, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.01, 1, bot_to_emulate),

        #    mf.create_conv_2d_model(0.0005, 2, bot_to_emulate),
        #    mf.create_conv_2d_model(0.001, 2, bot_to_emulate),
        #    mf.create_conv_2d_model(0.002, 2, bot_to_emulate),
        # for model in models:

        #    mf.create_conv_2d_model(0.01, 2, bot_to_emulate),
        #        ]
        # default learning rate 0.001
        # model.evaluate(cv_features, cv_labels, verbose=2)

        # prediction0 = model.predict(numpy.array([train_features[0]]))
        # print(prediction0)
        # print(train_labels[0])

        # prediction1 = model.predict(numpy.array([test_features[0]]))
        # print(prediction1)
        # print(test_labels[0])

    @unittest.skip('Skipped')
    def test_load_weights(self):
        bot_to_emulate = 'memetix_1'
        weight_path = f'logs/fit/conv_2d_20200913-114036/conv_2d_weights_741e221d-0fa7-4a29-9884-390f771a3007_09'
        ms = mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 7)
        ms.model.load_weights(weight_path)
        game_states = create_test_game_states(1, bot_to_emulate)
        (train, cross_val) = ms.encode_game_states(game_states)
        predictions = ms.model.predict(cross_val.features)
        blah = [(cross_val.labels[index], y, cross_val.labels[index].argmax() == numpy.array(y).argmax()) for index, y
                in enumerate(predictions)]
        pprint.pprint(blah)
        print(seq(blah).sum(lambda t: 1 if t[2] == True else 0) / len(predictions))

    @unittest.skip('Skipped')
    def test_run_report(self):
        run_stats: List[RunStats] = [jsonpickle.decode(read_file_contents(path)) for path in
                                     glob.glob(f'{os.getcwd()}\\logs\\fit\\**\\run_stats.json')]

        def get_data_points(rs: RunStats):
            # train_stop_index = rs.history['val_loss'].index(seq(rs.history["val_loss"]).min())
            return (
                rs.train_shape[0][0],
                seq(rs.history["loss"]).last(),
                seq(rs.history["val_loss"]).last(),
                seq(rs.history["categorical_accuracy"]).last(),
                seq(rs.history["val_categorical_accuracy"]).last(),
            )

        data_points = seq([get_data_points(rs) for
                           rs in run_stats if
                           rs.model_name == 'conv_2d']).order_by(lambda t: t[0]).to_list()
        plot_learning_curve(data_points)
        pprint.pprint(data_points)
