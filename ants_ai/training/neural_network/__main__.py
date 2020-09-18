# disables GPUs
import glob
import os
import uuid
from datetime import datetime
from typing import List, Dict, Union

import jsonpickle
import ants_ai.training.neural_network.model_factory as mf
from functional import seq
from numpy.core.multiarray import ndarray
from tensorflow.python.keras import Model
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.tests.test_utils import create_test_game_states

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
import tensorflow as tf

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


def show_learning_curve():
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


def train_model(game_states: List[GameState], ms: mf.ModelTrainer):
    log_dir = f'logs/fit/{ms.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
    run_stats_path = f'{log_dir}/run_stats.json'
    model_weights_path = f'{log_dir}/{ms.model_name}_weights_{uuid.uuid4()}'
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


def main(args=None):
    bot_to_emulate = 'memetix_1'
    game_states = create_test_game_states(1, bot_to_emulate)
    mt = mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 7)
    train_model(game_states, mt)


if __name__ == "__main__":
    main()
