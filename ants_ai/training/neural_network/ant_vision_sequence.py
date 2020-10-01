import math
import os
from enum import IntEnum
from typing import List, Tuple

import jsonpickle
import sklearn as sk
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from functional import seq
from tensorflow.python.keras.utils.data_utils import Sequence
import numpy as np
import ants_ai.training.neural_network.encoders as enc


class GameIndex:
    def __init__(self, game_path: str, position_start: int, length: int):
        self.game_path = game_path
        self.position_start = position_start
        self.length = length
        self.position_end = self.position_start + self.length


class DatasetType(IntEnum):
    TRAINING = 1
    CROSS_VAL = 2
    TEST = 3


class AntVisionSequence(Sequence):
    def __init__(self, game_paths: List[str], batch_size: int, bot_to_emulate: str, channel_count=7,
                 dataset_type: DatasetType = DatasetType.TRAINING):
        self.batch_size = batch_size
        self.channel_count = channel_count
        self.bot_to_emulate = bot_to_emulate
        self.game_paths = sk.utils.shuffle(game_paths)
        self.gst = GameStateTranslator()
        self.gsg = GameStateGenerator()
        self.game_indexes: List[GameIndex] = []
        self.loaded_indexes: List[Tuple[str, np.ndarray, np.ndarray]] = []
        self.max_load_count = 10
        self.dataset_type = dataset_type

    def read_config(self, game_path: str, config_path: str) -> GameIndex:
        with open(config_path, "r") as f:
            return GameIndex(game_path,
                             seq(self.game_indexes).map(lambda gi: gi.length).sum(),
                             int(f.read()))

    def write_config(self, config_path: str, example_count: int):
        with open(config_path, "w")as f:
            f.write(str(example_count))

    def load_game_state(self, path: str) -> GameState:
        with open(path, "r") as f:
            json_data = f.read()
            pr = jsonpickle.decode(json_data)
            return self.gsg.generate(pr)

    def get_feature_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_ANT_VISION_2D_FEATURES_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_label_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_ANT_VISION_2D_LABELS_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_index_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_ANT_VISION_2D_FEATURES_index_{self.bot_to_emulate}_{self.channel_count}.txt')

    def build_indexes(self):
        self.game_indexes = []
        for game_path in self.game_paths:
            if os.path.exists(self.get_index_path(game_path)):
                self.game_indexes.append(self.read_config(game_path, self.get_index_path(game_path)))
            else:
                gs = self.load_game_state(game_path)
                ant_vision = self.gst.convert_to_2d_ant_vision(self.bot_to_emulate, [gs])
                features, labels = enc.encode_2d_examples(ant_vision, self.channel_count)
                shuffled_features, shuffled_labels = sk.utils.shuffle(features, labels)
                print(f'Saving {features.shape[0]} examples to {self.get_feature_path(game_path)}')
                np.save(self.get_feature_path(game_path), shuffled_features)
                np.save(self.get_label_path(game_path), shuffled_labels)
                self.write_config(self.get_index_path(game_path), features.shape[0])
                self.game_indexes.append(self.read_config(game_path, self.get_index_path(game_path)))

    def load_index(self, index: GameIndex) -> Tuple[GameIndex, np.ndarray, np.ndarray]:
        loaded_index = seq(self.loaded_indexes).find(lambda t: t[0] == index.game_path)
        if loaded_index is not None:
            return index, loaded_index[1], loaded_index[2]
        else:
            features, labels = np.load(self.get_feature_path(index.game_path)), np.load(
                self.get_label_path(index.game_path))
            self.loaded_indexes.append((index.game_path, features, labels))
            if len(self.loaded_indexes) > self.max_load_count:
                self.loaded_indexes.pop(0)
            return index, features, labels

    def combine_ndarrays(self, encoded_results: List[Tuple[np.ndarray, np.ndarray]]) -> Tuple[
        np.ndarray, np.ndarray]:
        if len(encoded_results) == 1:
            return encoded_results[0]
        row_count = seq(encoded_results).map(lambda t: t[0].shape[0]).sum()

        features = np.empty([row_count, *encoded_results[0][0].shape[1:]], dtype=int)
        labels = np.empty([row_count, *encoded_results[0][1].shape[1:]], dtype=int)
        current_row = 0
        for r in encoded_results:
            for j in range(r[0].shape[0]):
                features[current_row] = r[0][j]
                labels[current_row] = r[1][j]
                current_row += 1
        return features, labels

    def select_batch(self, loaded_index: Tuple[GameIndex, np.ndarray, np.ndarray], batch_start: int, batch_end: int) \
            -> Tuple[np.ndarray, np.ndarray]:
        game_index, features, labels = loaded_index
        examples_to_select = seq(
            range(batch_start - game_index.position_start, (batch_end - game_index.position_start) + 1)) \
            .filter(lambda i: i >= 0) \
            .to_list()
        return features[min(examples_to_select):max(examples_to_select)], \
               labels[min(examples_to_select): max(examples_to_select)]

    # End indexes are exclusive
    def ranges_intersect(self, index_start0: int, index_end0: int, index_start1: int, index_end1: int) -> bool:
        return max(index_start0, index_start1) < min(index_end0 - 1, index_end1 - 1) + 1

    def get_batch(self, set_range: Tuple[int, int], batch_index: int):
        set_start_index, set_end_index = set_range
        number_of_batches = math.ceil((set_end_index - set_start_index) / self.batch_size)
        if batch_index >= number_of_batches or batch_index < 0:
            raise IndexError(f'Invalid index {batch_index}. Max index = {number_of_batches - 1}')
        batch_start = (batch_index * self.batch_size) + set_start_index
        batch_end = min(((batch_index + 1) * self.batch_size) + set_start_index, set_end_index)
        indexes_for_batch = [self.load_index(gi) for gi in self.game_indexes
                             if self.ranges_intersect(gi.position_start, gi.position_end, batch_start, batch_end)]
        examples_within_batch = [self.select_batch(t, batch_start, batch_end) for t in indexes_for_batch]
        return self.combine_ndarrays(examples_within_batch)

    def calculate_batch_count(self, range: Tuple[int, int]):
        start_index, end_index = range
        return math.ceil((end_index - start_index) / self.batch_size)

    def get_training_batch_count(self):
        return self.calculate_batch_count(self.get_training_range())

    def get_cross_val_batch_count(self):
        return self.calculate_batch_count(self.get_cross_val_range())

    def get_test_batch_count(self):
        return self.calculate_batch_count(self.get_test_range())

    def get_training_batch(self, index: int):
        return self.get_batch(self.get_training_range(), index)

    def get_cross_val_batch(self, index: int):
        return self.get_batch(self.get_cross_val_range(), index)

    def get_test_batch(self, index: int):
        return self.get_batch(self.get_test_range(), index)

    def get_total_count(self):
        return seq(self.game_indexes).map(lambda gi: gi.length).sum()

    def get_training_range(self) -> Tuple[int, int]:
        return 0, math.floor(self.get_total_count() * .60)

    def get_cross_val_range(self) -> Tuple[int, int]:
        return math.floor(self.get_total_count() * .60), math.floor(self.get_total_count() * .80)

    def get_test_range(self) -> Tuple[int, int]:
        return math.floor(self.get_total_count() * .80), self.get_total_count()

    def set_dataset_type(self, set_type: DatasetType):
        self.dataset_type = set_type

    def get_dataset_type(self):
        return self.dataset_type

    def range_len(self, range: Tuple[int, int]):
        return range[1] - range[0]

    def get_train_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_training_range()), 12, 12, self.channel_count)]

    def get_crossval_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_cross_val_range()), 12, 12, self.channel_count)]

    def get_test_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_test_range()), 12, 12, self.channel_count)]

    def __getitem__(self, index):
        if self.dataset_type == DatasetType.TRAINING:
            return self.get_training_batch(index)
        elif self.dataset_type == DatasetType.CROSS_VAL:
            return self.get_cross_val_batch(index)
        elif self.dataset_type == DatasetType.TEST:
            return self.get_test_batch(index)
        else:
            raise ValueError('Unknown enumeration used ' + self.dataset_type.name)

    def __len__(self):
        if self.dataset_type == DatasetType.TRAINING:
            return self.get_training_batch_count()
        elif self.dataset_type == DatasetType.CROSS_VAL:
            return self.get_cross_val_batch_count()
        elif self.dataset_type == DatasetType.TEST:
            return self.get_test_batch_count()
        else:
            raise ValueError('Unknown enumeration used ' + self.dataset_type.name)
