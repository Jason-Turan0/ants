import math
from abc import abstractmethod
from typing import List, Tuple, Union

import jsonpickle
import numpy as np
import sklearn as sk
from functional import seq

from tensorflow.python.keras.utils.data_utils import Sequence

from ants_ai.training.neural_network.sequences.data_structs import GameIndex, LoadedIndex, DatasetType
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from ants_ai.training.game_state.generator import GameStateGenerator


class FileSystemSequence(Sequence):
    def __init__(self, game_paths: List[str], batch_size: int, bot_to_emulate: str, channel_count=7,
                 dataset_type: DatasetType = DatasetType.TRAINING):
        self.batch_size = batch_size
        self.channel_count = channel_count
        self.bot_to_emulate = bot_to_emulate
        self.game_paths = sk.utils.shuffle(game_paths)
        self.gst = GameStateTranslator()
        self.gsg = GameStateGenerator()
        self.game_indexes: List[GameIndex] = []
        self.loaded_indexes: List[LoadedIndex] = []
        self.max_load_count = 10
        self.dataset_type = dataset_type

    @abstractmethod
    def build_indexes(self, rebuild: bool):
        pass

    @abstractmethod
    def load_index(self, index: GameIndex) -> LoadedIndex:
        pass

    @abstractmethod
    def get_train_feature_shape(self) -> List[tuple]:
        pass

    @abstractmethod
    def get_crossval_feature_shape(self) -> List[tuple]:
        pass

    @abstractmethod
    def get_test_feature_shape(self) -> List[tuple]:
        pass

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

    def combine_encoded_examples(self, encoded_results: List[Tuple[Union[np.ndarray, List[np.ndarray]], np.ndarray]]) -> \
            Tuple[Union[np.ndarray, List[np.ndarray]], np.ndarray]:
        if len(encoded_results) == 0:
            raise ValueError('Cannot be empty')
        if len(encoded_results) == 1:
            return encoded_results[0]
        if isinstance(encoded_results[0][0], list):
            row_count = seq(encoded_results).map(lambda t: t[1].shape[0]).sum()
            labels = np.empty([row_count, *encoded_results[0][1].shape[1:]], dtype=int)
            mapped_features = [np.empty([row_count, *feature_input.shape[1:]], dtype=int)
                               for feature_input in encoded_results[0][0]]
            current_row = 0
            for r in encoded_results:
                enc_features, enc_labels = r
                for set_row_num in range(enc_labels.shape[0]):
                    for input_index in range(len(enc_features)):
                        mapped_features[input_index][current_row] = enc_features[input_index][set_row_num]
                    labels[current_row] = enc_labels[set_row_num]
                    current_row += 1
            return mapped_features, labels
        else:
            row_count = seq(encoded_results).map(lambda t: t[0].shape[0]).sum()
            features = np.empty([row_count, *encoded_results[0][0].shape[1:]], dtype=int)
            labels = np.empty([row_count, *encoded_results[0][1].shape[1:]], dtype=int)
            current_row = 0
            for r in encoded_results:
                enc_features, enc_labels = r
                for set_row_num in range(enc_features.shape[0]):
                    features[current_row] = enc_features[set_row_num]
                    labels[current_row] = enc_labels[set_row_num]
                    current_row += 1
            return features, labels

    def select_batch(self, loaded_index: LoadedIndex, batch_start: int, batch_end: int) \
            -> Tuple[Union[np.ndarray, List[np.ndarray]], np.ndarray]:
        examples_to_select = seq(
            range(batch_start - loaded_index.game_index.position_start,
                  (batch_end - loaded_index.game_index.position_start) + 1)) \
            .filter(lambda i: i >= 0) \
            .to_list()

        if isinstance(loaded_index.features, list):
            mapped_features = seq(loaded_index.features).map(
                lambda f: f[min(examples_to_select):max(examples_to_select)]).to_list()
            return mapped_features, loaded_index.labels[min(examples_to_select): max(examples_to_select)]
        else:
            return loaded_index.features[min(examples_to_select):max(examples_to_select)], \
                   loaded_index.labels[min(examples_to_select): max(examples_to_select)]

    # End indexes are exclusive
    def ranges_intersect(self, index_start0: int, index_end0: int, index_start1: int, index_end1: int) -> bool:
        return max(index_start0, index_start1) < min(index_end0 - 1, index_end1 - 1) + 1

    def get_batch(self, set_range: Tuple[int, int], batch_index: int):
        set_start_index, set_end_index = set_range
        number_of_batches = math.ceil((set_end_index - set_start_index) / self.batch_size)
        if number_of_batches == 0:
            raise IndexError(
                f'Invalid index {batch_index}. Sequence was likely not initialized. Call build_indexes to initialize sequence.')
        if batch_index >= number_of_batches or batch_index < 0:
            raise IndexError(f'Invalid index {batch_index}. Max index = {number_of_batches - 1}')
        batch_start = (batch_index * self.batch_size) + set_start_index
        batch_end = min(((batch_index + 1) * self.batch_size) + set_start_index, set_end_index)
        indexes_for_batch = [self.load_index(gi) for gi in self.game_indexes
                             if self.ranges_intersect(gi.position_start, gi.position_end, batch_start, batch_end)]
        examples_within_batch = [self.select_batch(t, batch_start, batch_end) for t in indexes_for_batch]
        return self.combine_encoded_examples(examples_within_batch)

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

    def range_len(self, index_range: Tuple[int, int]):
        return index_range[1] - index_range[0]

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
