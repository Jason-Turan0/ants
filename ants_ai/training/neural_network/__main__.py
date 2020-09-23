import glob
import os
import uuid
from datetime import datetime
from typing import List, Dict, Union

import jsonpickle
import ants_ai.training.neural_network.model_factory as mf
from ants_ai.training.neural_network.conv2d_model_trainer import Conv2DModelTrainer
from encoders import TrainingDataset
from functional import seq
from kerastuner import HyperParameter, HyperParameters
from numpy.core.multiarray import ndarray
from tensorflow.python.keras import Model
import kerastuner as kt

from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.tests.test_utils import create_test_game_states, create_test_play_results

# disables GPUs
os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
import tensorflow as tf

import matplotlib.pyplot as plt
from pprint import pprint


class LayerStats:
    def __init__(self, layer_name: str, input_shape: tuple, output_shape: tuple):
        self.layer_name = layer_name
        self.input_shape = input_shape
        self.output_shape = output_shape


class RunStats:
    def __init__(self, model_name: str, model: Model, ds: TrainingDataset, epochs: int, batch_size: int,
                 model_params: Dict[str, float], history: Dict, discovery_path: str):
        self.model_name = model_name
        self.layer_shapes: List[LayerStats] = seq(model.layers).map(
            lambda layer: LayerStats(layer.name, layer.input_shape, layer.output_shape)) \
            .to_list()
        test_eval = model.evaluate(ds.test.features, ds.test.labels)
        self.test_loss = test_eval[0]
        self.test_categorical_accuracy = test_eval[1]
        self.train_shape = self.extract_size(ds.train.features)
        self.cross_val_shape = self.extract_size(ds.cross_val.features)
        self.test_shape = self.extract_size(ds.test.features)
        self.epochs = epochs
        self.batch_size = batch_size
        self.model_params = model_params
        self.history = history
        self.discovery_path = discovery_path
        self.timestamp = datetime.now().timestamp()

    def extract_size(self, item: Union[ndarray, List[ndarray]]) -> List[tuple]:
        if isinstance(item, list):
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
    pprint(data_points)


def discover_model(game_states: List[GameState], mt: mf.ModelTrainer):
    discovery_path = f'model_discovery/{mt.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
    tuner = kt.BayesianOptimization(mt.construct_discover_model,
                                    objective='val_categorical_accuracy',
                                    max_trials=20,
                                    directory=discovery_path,
                                    project_name='ants_ai')
    tds: TrainingDataset = mt.encode_games(game_states)
    tuner.search(tds.train.features, tds.train.labels, epochs=5, batch_size=50,
                 validation_data=(tds.cross_val.features, tds.cross_val.labels))
    for best_hps in tuner.get_best_hyperparameters(3):
        train_model(mt, best_hps, tds, discovery_path)


def train_model(ms: mf.ModelTrainer,
                hps: HyperParameters,
                tds: TrainingDataset,
                discovery_path: str):
    log_dir = f'logs/fit/{ms.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
    run_stats_path = f'{log_dir}/run_stats.json'
    model_weights_path = f'{log_dir}/{ms.model_name}_weights'
    model_checkpoint_callback = tf.keras.callbacks.ModelCheckpoint(
        filepath=model_weights_path,
        save_weights_only=True,
        monitor='val_categorical_accuracy',
        mode='max',
        save_best_only=True)
    callbacks = [
        tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1),
        model_checkpoint_callback]
    epochs = 10
    batch_size = 50
    model = ms.construct_model(hps)
    fit = model.fit(tds.train.features, tds.train.labels,
                    validation_data=(tds.cross_val.features, tds.cross_val.labels),
                    epochs=epochs,
                    callbacks=callbacks,
                    batch_size=batch_size)
    stats = RunStats(ms.model_name,
                     model,
                     tds,
                     epochs,
                     batch_size,
                     hps.values,
                     fit.history,
                     discovery_path)
    with open(run_stats_path, 'w') as stream:
        stream.write(jsonpickle.encode(stats))
    print(run_stats_path)
    print('Finished')


def main():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    mt = Conv2DModelTrainer(bot_to_emulate)
    print(mt.encode_games(game_paths))
    # discover_model(game_states, mt)


if __name__ == "__main__":
    main()
