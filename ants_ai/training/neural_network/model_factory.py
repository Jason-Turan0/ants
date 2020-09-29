import os
from abc import abstractmethod
from enum import Enum
from math import floor
from random import shuffle
from typing import List, Dict, Union, Tuple, Callable

import jsonpickle
import sklearn as sk
from ants_ai.training.neural_network.encoders import TrainingDataset
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.game_state.generator import GameStateGenerator
import ants_ai.training.neural_network.encoders as enc

import multiprocessing as mp

from functional import seq
from kerastuner import HyperParameters, HyperParameter
from ants_ai.training.neural_network.model_hyper_parameter import ModelHyperParameter
from tensorflow.python.keras.models import Model

import numpy as np


class EncodingType(Enum):
    ANT_VISION_2D = 1
    MAP_2D = 2


def shuffle_and_split(examples: List):
    shuffle(examples)
    train_cutoff = floor(len(examples) * .60)
    cv_cutoff = floor(len(examples) * .80)
    train_examples = examples[0:train_cutoff]
    cv_examples = examples[train_cutoff:cv_cutoff]
    text_examples = examples[cv_cutoff:]
    return train_examples, cv_examples, text_examples


def load_game_state(path: str) -> GameState:
    with open(path, "r") as f:
        json_data = f.read()
        pr = jsonpickle.decode(json_data)
        gsg = GameStateGenerator()
        return gsg.generate(pr)


def map_to_input(task: Tuple[str, EncodingType, str]) -> Tuple[np.ndarray, np.ndarray]:
    bot_name, type, game_path = task
    channel_count = 7
    gst = GameStateTranslator()
    if type == EncodingType.ANT_VISION_2D:
        feature_cache_path = game_path.replace('.json', f'_ANT_VISION_2D_FEATURES_{bot_name}_{channel_count}.npy')
        label_cache_path = game_path.replace('.json', f'_ANT_VISION_2D_LABELS_{bot_name}_{channel_count}.npy')
        if os.path.exists(feature_cache_path):
            return np.load(feature_cache_path), np.load(label_cache_path)
        gs = load_game_state(game_path)
        try:
            ant_vision = gst.convert_to_2d_ant_vision(bot_name, [gs])
            features, labels = enc.encode_2d_examples(ant_vision, channel_count)
            print(f'Saving {feature_cache_path}')
            np.save(feature_cache_path, features)
            np.save(label_cache_path, labels)
            return features, labels
        except:
            print(f'Failed to load ${game_path}')
            return np.empty([0, 12, 12, 7]), np.empty([0, 5])

    elif type == EncodingType.MAP_2D:
        feature_cache_path = game_path.replace('.json', f'_ANT_VISION_2DMAP_FEATURES_{bot_name}_{channel_count}.npy')
        label_cache_path = game_path.replace('.json', f'_ANT_VISION_2DMAP_LABELS_{bot_name}_{channel_count}.npy')
        if os.path.exists(feature_cache_path):
            return np.load(feature_cache_path), np.load(label_cache_path)
        gs = load_game_state(game_path)
        try:
            ant_vision = gst.convert_to_antmap(bot_name, [gs])
            features, labels = enc.encode_map_examples(ant_vision, channel_count)
            print(f'Saving {feature_cache_path}')
            np.save(feature_cache_path, features)
            np.save(label_cache_path, labels)
            return features, labels
        except:
            print(f'Failed to load ${game_path}')
            return np.empty([0, 43, 39, 7]), np.empty([0, 5])
    else:
        raise NotImplementedError()


def combine_ndarrays(encoded_results: List[Tuple[np.ndarray, np.ndarray]]):
    row_count = seq(encoded_results).map(lambda t: t[0].shape[0]).sum()
    features = np.empty([row_count, *encoded_results[0][0].shape[1:]], dtype=int)
    labels = np.empty([row_count, *encoded_results[0][1].shape[1:]], dtype=int)
    current_row = 0
    for r in encoded_results:
        for j in range(r[0].shape[0]):
            features[current_row] = r[0][j]
            labels[current_row] = r[1][j]
            current_row += 1
    return row_count, features, labels


def create_test_examples(bot_name: str, game_paths: List[str], enc_type: EncodingType) -> TrainingDataset:
    print(f'Loading {len(game_paths)} games')
    pool_count = min(mp.cpu_count() - 1, len(game_paths))
    if enc_type == EncodingType.ANT_VISION_2D:
        tasks = [(bot_name, enc_type, gp) for gp in game_paths]
        pool = mp.Pool(pool_count)
        encoded_results: List[Tuple[np.ndarray, np.ndarray]] = pool.map(map_to_input, tasks)
        pool.close()
        print(f'Loaded {len(game_paths)} games')
        row_count, features, labels = combine_ndarrays(encoded_results)
        print(f'Loaded {row_count} examples')
        shuffled_features, shuffled_labels = sk.utils.shuffle(features, labels)
        train_cutoff = floor(row_count * .60)
        cv_cutoff = floor(row_count * .80)
        train_examples = enc.LabeledDataset(shuffled_features[0:train_cutoff], shuffled_labels[0:train_cutoff])
        cv_examples = enc.LabeledDataset(shuffled_features[train_cutoff:cv_cutoff],
                                         shuffled_labels[train_cutoff:cv_cutoff])
        test_examples = enc.LabeledDataset(shuffled_features[cv_cutoff:], shuffled_labels[cv_cutoff:])
        return TrainingDataset(train_examples, cv_examples, test_examples)
    elif enc_type == EncodingType.MAP_2D:
        tasks = [(bot_name, EncodingType.ANT_VISION_2D, gp) for gp in game_paths]
        pool = mp.Pool(pool_count)
        av_encoded_results: List[Tuple[np.ndarray, np.ndarray]] = pool.map(map_to_input, tasks)
        pool.close()

        tasks = [(bot_name, EncodingType.MAP_2D, gp) for gp in game_paths]
        pool = mp.Pool(pool_count)
        map_encoded_results: List[Tuple[np.ndarray, np.ndarray]] = pool.map(map_to_input, tasks)
        pool.close()
        print(f'Loaded {len(game_paths)} games')
        av_row_count, av_features, av_labels = combine_ndarrays(av_encoded_results)
        map_row_count, map_features, map_labels = combine_ndarrays(map_encoded_results)
        if av_row_count != map_row_count: raise ValueError(f'{av_row_count} does not match {map_row_count}')
        print(f'Loaded {av_row_count} examples')
        shuffled_av_features, shuffled_av_labels, shuffled_map_features, shuffled_map_labels = \
            sk.utils.shuffle(av_features, av_labels, map_features, map_labels)
        train_cutoff = floor(av_row_count * .60)
        cv_cutoff = floor(av_row_count * .80)

        return TrainingDataset(
            enc.LabeledDataset([shuffled_av_features[:train_cutoff], shuffled_map_features[:train_cutoff]],
                               shuffled_av_labels[:train_cutoff]),
            enc.LabeledDataset(
                [shuffled_av_features[train_cutoff:cv_cutoff], shuffled_map_features[train_cutoff:cv_cutoff]],
                shuffled_av_labels[train_cutoff:cv_cutoff]),
            enc.LabeledDataset([shuffled_av_features[cv_cutoff:], shuffled_map_features[cv_cutoff:]],
                               shuffled_av_labels[cv_cutoff:])
        )
    else:
        raise NotImplementedError()


class ModelFactory:
    def __init__(self, model_name: str, bot_name: str, default_parameters_values: Dict[str, ModelHyperParameter]):
        self.model_name = model_name
        self.bot_name = bot_name
        self.default_parameters_values = default_parameters_values

    @abstractmethod
    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        pass

    @abstractmethod
    def construct_model(self, tuned_params: Dict[str, Union[int, float]], hps: HyperParameters = None) -> Model:
        pass

    def construct_discover_model(self, hps: HyperParameters) -> Model:
        return self.construct_model({}, hps)

    def encode_game_states(self, game_paths: List[str], enc_type: EncodingType) -> TrainingDataset:
        return create_test_examples(self.bot_name, game_paths, enc_type)

    def get_model_params(self, tuned_model_params: Dict[str, Union[int, float]]):
        return {
            k: tuned_model_params.get(k)
            if k in tuned_model_params.keys()
            else self.default_parameters_values[k].default_value
            for k in self.default_parameters_values.keys()
        }
