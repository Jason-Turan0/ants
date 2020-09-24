import glob
import os
from pprint import pprint
from typing import List

import jsonpickle
import matplotlib.pyplot as plt
from ants_ai.training.neural_network.hybrid_model_factory import HybridModelFactory
from ants_ai.training.neural_network.model_trainer import ModelTrainer
from ants_ai.training.neural_network.run_stats import RunStats
from functional import seq

# disables GPUs

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'


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


def main():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    hmf = HybridModelFactory(bot_to_emulate)
    mt = ModelTrainer()
    # print(mt.encode_games(game_paths))
    mt.discover_model(game_paths[0:1], hmf)


if __name__ == "__main__":
    main()
