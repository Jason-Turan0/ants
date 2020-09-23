import os
from abc import abstractmethod
from enum import Enum
from math import floor
from random import shuffle
from typing import List, Dict, Union, Tuple

import jsonpickle
import sklearn as sk
from encoders import TrainingDataset
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.game_state.generator import GameStateGenerator
import multiprocessing as mp

from functional import seq
from kerastuner import HyperParameters
from tensorflow.python.keras.models import Model
import encoders as enc
import numpy as np
from functools import reduce


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
    t = GameStateTranslator()
    if type == EncodingType.ANT_VISION_2D:
        feature_cache_path = game_path.replace('.json', f'_ANT_VISION_2D_FEATURES_{bot_name}_{channel_count}.npy')
        label_cache_path = game_path.replace('.json', f'_ANT_VISION_2D_LABELS_{bot_name}_{channel_count}.npy')
        if os.path.exists(feature_cache_path):
            return np.load(feature_cache_path), np.load(label_cache_path)
        gs = load_game_state(game_path)
        try:
            ant_vision = t.convert_to_2d_ant_vision(bot_name, [gs])
            features, labels = enc.encode_2d_examples(ant_vision, 7)
            print(f'Saving {feature_cache_path}')
            np.save(feature_cache_path, features)
            np.save(label_cache_path, labels)
            return features, labels
        except:
            print(f'Failed to load ${game_path}')
            return np.empty([0, 12, 12, 7]), np.empty([0, 5])

    else:
        raise NotImplementedError()


def create_test_examples(bot_name: str, game_paths: List[str], enc_type: EncodingType):
    print(f'Loading {len(game_paths)} games')
    if enc_type == EncodingType.ANT_VISION_2D:
        tasks = [(bot_name, enc_type, gp) for gp in game_paths]
        pool = mp.Pool(min(mp.cpu_count() - 1, len(game_paths)))
        encoded_results: List[Tuple[np.ndarray, np.ndarray]] = pool.map(map_to_input, tasks)
        pool.close()
        print(f'Loaded {len(game_paths)} games')
        row_count = seq(encoded_results).map(lambda t: t[0].shape[0]).sum()
        features = np.empty([row_count, *encoded_results[0][0].shape[1:]])
        labels = np.empty([row_count, *encoded_results[0][1].shape[1:]])
        current_row = 0
        for r in encoded_results:
            for j in range(r[0].shape[0]):
                features[current_row] = r[0][j]
                labels[current_row] = r[1][j]
                current_row += 1
        example_size = features.shape[0]
        shuffled_features, shuffled_labels = sk.utils.shuffle(features, labels)
        train_cutoff = floor(example_size * .60)
        cv_cutoff = floor(example_size * .80)
        train_examples = enc.LabeledDataset(shuffled_features[0:train_cutoff], shuffled_labels[0:train_cutoff])
        cv_examples = enc.LabeledDataset(shuffled_features[train_cutoff:cv_cutoff],
                                         shuffled_labels[train_cutoff:cv_cutoff])
        test_examples = enc.LabeledDataset(shuffled_features[cv_cutoff:], shuffled_labels[cv_cutoff:])
        return TrainingDataset(train_examples, cv_examples, test_examples)

    else:
        raise NotImplementedError()


class ModelTrainer:
    def __init__(self, model_name: str, bot_name: str):
        self.model_name = model_name
        self.bot_name = bot_name

    @abstractmethod
    def encode_games(self, game_paths: List[str]) -> TrainingDataset:
        pass

    @abstractmethod
    def construct_model(self, model_params: Dict[str, Union[int, float]]) -> Model:
        pass

    @abstractmethod
    def construct_discover_model(self, hps: HyperParameters) -> Model:
        pass

    def encode_game_states(self, game_paths: List[str], enc_type: EncodingType) -> TrainingDataset:
        return create_test_examples(self.bot_name, game_paths, enc_type)
