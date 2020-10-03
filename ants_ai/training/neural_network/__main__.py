import glob
import os
from pprint import pprint
from typing import List

import jsonpickle
import matplotlib.pyplot as plt

from ants_ai.training.neural_network.sequences.hybrid_sequence import HybridSequence
from ants_ai.training.neural_network.factories.hybrid_model_factory import HybridModelFactory
from ants_ai.training.neural_network.factories.conv2d_maxpool_model_factory import Conv2DMaxPoolModelFactory
from ants_ai.training.neural_network.factories.conv2d_model_factory import Conv2DModelFactory
from ants_ai.training.neural_network.trainer.model_trainer import ModelTrainer
from ants_ai.training.neural_network.trainer.run_stats import RunStats

from functional import seq, pseq

from tensorflow.python.keras.models import Model
import multiprocessing as mp
from prettytable import PrettyTable

# disables GPUs
# os.environ['CUDA_VISIBLE_DEVICES'] = '-1'
import tensorflow as tf

# GeForce GTX 750 Ti
# python 3.7.8
# tf version 2.1.1
print(tf.version.VERSION)


def read_file_contents(path):
    with open(path, "r") as f:
        return f.read()


def plot_learning_curve(data, model_name: str):
    # summarize history for accuracy
    plt.plot([d[0] for d in data], [d[1] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[2] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[3] for d in data], linestyle='--', marker='.')
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Loss')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation', 'test'], loc='upper left')
    plt.show()

    plt.plot([d[0] for d in data], [d[4] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[5] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[6] for d in data], linestyle='--', marker='.')
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Categorical Accuracy')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation', 'test'], loc='upper left')
    plt.show()


def show_learning_curve(model_name: str):
    run_stats: List[RunStats] = [jsonpickle.decode(read_file_contents(path)) for path in
                                 glob.glob(f'{os.getcwd()}\\logs\\fit\\**\\run_stats.json')]

    def get_data_points(rs: RunStats):
        # train_stop_index = rs.history['val_loss'].index(seq(rs.history["val_loss"]).min())
        val_loss = seq(rs.history["val_loss"]).last()
        val_cat_acc = seq(rs.history["val_categorical_accuracy"]).last()
        return (
            rs.train_shape[0][0],
            seq(rs.history["loss"]).last(),
            val_loss,
            rs.test_loss if hasattr(rs, 'test_loss') else val_loss,

            seq(rs.history["categorical_accuracy"]).last(),
            val_cat_acc,
            rs.test_categorical_accuracy if hasattr(rs, 'test_categorical_accuracy') else val_cat_acc
        )

    data_points = seq([get_data_points(rs) for
                       rs in run_stats if
                       rs.model_name == model_name]) \
        .filter(lambda t: t[0] > 4000) \
        .order_by(lambda t: t[0]) \
        .to_list()
    x = PrettyTable()
    for row in data_points:
        x.add_row(row)
    print(x)

    plot_learning_curve(data_points, model_name)


def print_layers(model: Model):
    for l in model.layers:
        print(f'{l.name}, {l.input_shape}, {l.output_shape}')


def main():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    # print(len(game_paths))
    game_lengths = [1]
    # seq = HybridSequence(game_paths, 50, bot_to_emulate)
    # seq.build_indexes()
    mt = ModelTrainer()
    factory = HybridModelFactory(bot_to_emulate)
    for gl in game_lengths:
        mt.train_model(game_paths[0:gl], factory)
    # mt = ModelTrainer()
    # mt.train_model(game_paths, HybridModelFactory(bot_to_emulate))
    # factories = [
    #     Conv2DModelFactory(bot_to_emulate),
    #     Conv2DMaxPoolModelFactory(bot_to_emulate),
    #     HybridModelFactory(bot_to_emulate)
    # ]
    # mt = ModelTrainer()
    # for factory in factories:
    #     mt.train_model(game_paths, factory)


def build_index(game_path: str):
    s = HybridSequence([game_path], 50, 'memetix_1')
    s.build_indexes()
    return True


def rebuild_indexes():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    pool = mp.Pool(mp.cpu_count() - 1)
    pool.map(build_index, game_paths)
    pool.close()


def compare_model_learning_curve():
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    show_learning_curve('conv_2d')
    show_learning_curve('hybrid_2d')


if __name__ == "__main__":
    main()
