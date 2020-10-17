import logging
import os

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # FATAL
os.environ['TF_CPP_MIN_VLOG_LEVEL'] = '3'  # FATAL
logging.getLogger('tensorflow').setLevel(logging.FATAL)
logging.getLogger('tensorflow').disabled = True
import cProfile
# lg = logging.getLogger('tensorflow')
# lg.disabled = True
# lg.setLevel(logging.ERROR)
#
#
# class NoParsingFilter(logging.Filter):
#     def filter(self, record):
#         print('FILTER!')
#         return True
#         # return not record.getMessage().startswith('parsing')
#
#
# lg.addFilter(NoParsingFilter())
#
# import tensorflow as tf
#
# tf.logging.set_verbosity(tf.logging.ERROR)
#
# print(lg)

import glob
import multiprocessing as mp
import os
from pprint import pprint
from typing import List, Tuple

import jsonpickle
# import matplotlib.pyplot as plt
from ants_ai.training.neural_network.sequences.ant_vision_sequence import AntVisionSequence
from ants_ai.training.neural_network.sequences.map_view_sequence import MapViewSequence

from ants_ai.training.neural_network.factories.hybrid_model_factory import HybridModelFactory
from ants_ai.training.neural_network.sequences.hybrid_sequence import HybridSequence
# from ants_ai.training.neural_network.factories.conv2d_maxpool_model_factory import Conv2DMaxPoolModelFactory
from ants_ai.training.neural_network.factories.conv2d_model_factory import Conv2DModelFactory
from ants_ai.training.neural_network.factories.combined_model_factory import CombinedModelFactory
from ants_ai.training.neural_network.factories.map_view_model_factory import MapViewModelFactory
from ants_ai.training.game_state.game_map import Direction
from ants_ai.training.neural_network.trainer.model_trainer import ModelTrainer
from ants_ai.training.neural_network.trainer.run_stats import RunStats
from ants_ai.training.neural_network.sequences.data_structs import DatasetType
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.encoders import encoders as enc
from functional import seq
from prettytable import PrettyTable
from tensorflow.python.keras.models import Model
from matplotlib import pyplot as plt


# GeForce GTX 750 Ti
# python 3.7.8
# tf version 2.1.1


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
    pt = PrettyTable()
    pt.field_names = ['train_size', 'train_loss', 'val_loss', 'test_loss', 'train_acc', 'val_acc', 'test_acc']
    for row in data_points:
        pt.add_row(row)
    print(pt)

    plot_learning_curve(data_points, model_name)


def print_layers(model: Model):
    for l in model.layers:
        print(f'{l.name}, {l.input_shape}, {l.output_shape}')


def main():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    print(len(game_paths))
    game_lengths = [5, 10, 20, 50, 75, 100]
    # seq = HybridSequence(game_paths, 50, bot_to_emulate)
    # seq.build_indexes()
    mt = ModelTrainer()
    factory = MapViewModelFactory(bot_to_emulate)
    mt.train_model(game_paths[:10], factory)
    for gl in game_lengths:
        mt.train_model(game_paths[0:gl], factory)


def load_game_state(path: str, gsg: GameStateGenerator) -> GameState:
    with open(path, "r") as f:
        json_data = f.read()
        pr = jsonpickle.decode(json_data)
        return gsg.generate(pr)


# def convert_to_dir(arr: List[float]) -> List[Tuple[float, Direction]]:
#     blah = list(Direction.__members__.items())
#     blahReturn: List[Tuple[float, Direction]] = []
#     for index, prob in enumerate(arr):
#         # print(index, prob)
#         blahReturn.append((prob, blah[index][1]))
#     return blahReturn
#
#
# def main1():
#     bot_to_emulate = 'memetix_1'
#     game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
#     train_games = game_paths[:1]
#     test_games = game_paths[2:3]
#     mv_mf = MapViewModelFactory(bot_to_emulate)
#     conv_mf = Conv2DModelFactory(bot_to_emulate)
#     mt = ModelTrainer()
#
#     trained_conv, trained_conv_stats = mt.train_model(train_games, conv_mf)
#     trained_mv, trained_mv_stats = mt.train_model(train_games, mv_mf)
#
#     gst = GameStateTranslator()
#     gsg = GameStateGenerator()
#     game_states = seq(test_games).map(lambda path: load_game_state(path, gsg)).to_list()
#     mv_features, mv_labels = enc.encode_map_examples(gst.convert_to_global_antmap(bot_to_emulate, game_states), 7)
#     av_features, av_labels = enc.encode_2d_examples(gst.convert_to_2d_ant_vision(bot_to_emulate, game_states), 7)
#
#     prediction_conv = trained_conv.predict(av_features)
#     prediction_mv = trained_mv.predict(mv_features)
#     print(prediction_conv.shape)
#     print(prediction_mv.shape)
#     predictions = []
#     actuals = [seq(convert_to_dir(av_labels[index])).max_by(lambda t: t[0])[1] for index in range(av_labels.shape[0])]
#     for row_index in range(prediction_mv.shape[0]):
#         blah = prediction_conv[row_index]
#         blah1 = prediction_mv[row_index]
#         # pprint(blah)
#         # pprint(blah1)
#         dir_probs = convert_to_dir(blah)
#         dir_probs.extend(convert_to_dir(blah1))
#         predictions.append(seq(dir_probs).max_by(lambda t: t[0])[1])
#
#     print(actuals[0])
#     print(predictions[0])
#     acc = seq(range(len(predictions))).filter(lambda i: predictions[i] == actuals[i]).len() / len(actuals)
#     print(acc)


def main2():
    bot_to_emulate = 'memetix_1'
    gst = GameStateTranslator()
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    game_states = seq(game_paths[2:4]).map(lambda path: load_game_state(path, GameStateGenerator())).to_list()
    mv_features, mv_labels = enc.encode_map_examples(gst.convert_to_global_antmap(bot_to_emulate, game_states), 7)
    pprint(mv_features.shape)


def main3():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    print(len(game_paths))
    game_lengths = [1, 2, 5, 10, 20, 30, 50, 60, 80, 100]
    # seq = HybridSequence(game_paths, 50, bot_to_emulate)
    # seq.build_indexes()
    mt = ModelTrainer()
    map_factory = MapViewModelFactory(bot_to_emulate)
    conv2D_factory = Conv2DModelFactory(bot_to_emulate)

    for gl in game_lengths:
        train_game_paths = game_paths[0:gl]

        conv_model, conv_stats = mt.train_model(train_game_paths, conv2D_factory)
        map_model, map_stats = mt.train_model(train_game_paths, map_factory)

        combined_factory = CombinedModelFactory(bot_to_emulate, conv_stats.weight_path, map_stats.weight_path)
        combined_model, combined_stats = mt.train_model(train_game_paths, combined_factory)
        print(f"Finished training on game length {gl}. Test acc {combined_stats.test_categorical_accuracy}")


def compare_model_learning_curve():
    show_learning_curve('mapview_2d')
    show_learning_curve('conv_2d')
    show_learning_curve('combined_2d')


if __name__ == "__main__":
    compare_model_learning_curve()
