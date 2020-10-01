import glob
import os
from pprint import pprint
from typing import List

import jsonpickle
import matplotlib.pyplot as plt
from ants_ai.training.neural_network.hybrid_model_factory import HybridModelFactory
from ants_ai.training.neural_network.model_trainer import ModelTrainer
from ants_ai.training.neural_network.run_stats import RunStats
from ants_ai.training.neural_network.conv2d_maxpool_model_factory import Conv2DMaxPoolModelFactory
from ants_ai.training.neural_network.conv2d_model_factory import Conv2DModelFactory
from functional import seq

# disables GPUs
from tensorflow.python.keras.models import Model

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'


def read_file_contents(path):
    with open(path, "r") as f:
        return f.read()


def plot_learning_curve(data, model_name: str):
    # summarize history for accuracy
    plt.plot([d[0] for d in data], [d[1] for d in data])
    plt.plot([d[0] for d in data], [d[2] for d in data])
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Loss')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation'], loc='upper left')
    plt.show()

    plt.plot([d[0] for d in data], [d[3] for d in data])
    plt.plot([d[0] for d in data], [d[4] for d in data])
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Categorical Accuracy')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation'], loc='upper left')
    plt.show()


def show_learning_curve(model_name: str):
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
                       rs.model_name == model_name]).order_by(lambda t: t[0]).to_list()
    plot_learning_curve(data_points, model_name)


def print_layers(model: Model):
    for l in model.layers:
        print(f'{l.name}, {l.input_shape}, {l.output_shape}')


def main0():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')][200:202]
    # print(len(game_paths))
    mt = ModelTrainer()
    mt.train_model(game_paths, HybridModelFactory('memetix_1'))
    factories = [
        Conv2DModelFactory(bot_to_emulate),
        Conv2DMaxPoolModelFactory(bot_to_emulate),
        HybridModelFactory(bot_to_emulate)
    ]
    mt = ModelTrainer()
    for factory in factories:
        mt.train_model(game_paths, factory)


def main():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    # print(len(game_paths))
    mt = ModelTrainer()
    mt.train_model(game_paths[:20], Conv2DModelFactory(bot_to_emulate))


if __name__ == "__main__":
    main()
