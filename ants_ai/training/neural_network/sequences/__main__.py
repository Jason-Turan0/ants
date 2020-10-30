import argparse
import glob
import multiprocessing as mp
import os
from typing import List, Tuple

import jsonpickle
import matplotlib.pyplot as plt
from ants_ai.training.neural_network.sequences.ant_vision_sequence import AntVisionSequence
from ants_ai.training.neural_network.sequences.map_view_sequence import MapViewSequence
from ants_ai.training.neural_network.sequences.combined_sequence import CombinedSequence

from ants_ai.training.neural_network.factories.hybrid_model_factory import HybridModelFactory
from ants_ai.training.neural_network.sequences.hybrid_sequence import HybridSequence
# from ants_ai.training.neural_network.factories.conv2d_maxpool_model_factory import Conv2DMaxPoolModelFactory
from ants_ai.training.neural_network.factories.conv2d_model_factory import Conv2DModelFactory
from ants_ai.training.neural_network.trainer.model_trainer import ModelTrainer
from ants_ai.training.neural_network.trainer.run_stats import RunStats
from functional import seq

from prettytable import PrettyTable
from tensorflow.python.keras.models import Model


# def main():
# bot_to_emulate = 'memetix_1'
# game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
# print(len(game_paths))
# game_lengths = [300, 400, 500, 640]
# # seq = HybridSequence(game_paths, 50, bot_to_emulate)
# # seq.build_indexes()
# mt = ModelTrainer()
# factory = Conv2DModelFactory(bot_to_emulate)
# for gl in game_lengths:
#     mt.train_model(game_paths[0:gl], factory)
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


def build_index(task: Tuple[str, str]):
    game_path, seq_type = task
    bot_to_emulate = 'memetix_1'
    if seq_type == 'MapViewSequence':
        s = MapViewSequence([game_path], 50, bot_to_emulate)
    elif seq_type == 'AntVisionSequence':
        s = AntVisionSequence([game_path], 50, bot_to_emulate)
    elif seq_type == 'CombinedSequence':
        s = CombinedSequence([game_path], 50, bot_to_emulate)
    else:
        raise NotImplementedError(seq_type)
    s.build_indexes(False)
    return True


def rebuild_indexes(data_path):
    sequence_types = ['AntVisionSequence', 'MapViewSequence', 'CombinedSequence']
    game_paths = [f for f in glob.glob(f'{data_path}\\training\\**\\*.json')]
    pool = mp.Pool(mp.cpu_count() - 1)
    pool.map(build_index, [(path, seq_type) for seq_type in sequence_types for path in game_paths])
    pool.close()


if __name__ == "__main__":
    default_data_path = f'{os.getcwd()}\\ants_ai_data'
    parser = argparse.ArgumentParser(description='Rebuilds indexes')
    parser.add_argument('-dp', '--data-path', help='The root folder to look for training data and save training stats',
                        default=default_data_path)
    args = parser.parse_args()
    rebuild_indexes(args.data_path)
