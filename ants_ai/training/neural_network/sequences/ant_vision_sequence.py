import os
from typing import List

import sklearn as sk
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from functional import seq
import numpy as np
import ants_ai.training.neural_network.encoders.encoders as enc
from ants_ai.training.neural_network.sequences.data_structs import GameIndex, LoadedIndex, DatasetType


class AntVisionSequence(FileSystemSequence):
    def __init__(self, game_paths: List[str], batch_size: int, bot_to_emulate: str, channel_count=7,
                 dataset_type: DatasetType = DatasetType.TRAINING):
        super().__init__(game_paths, batch_size, bot_to_emulate, channel_count, dataset_type)

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

    def load_index(self, index: GameIndex) -> LoadedIndex:
        loaded_index = seq(self.loaded_indexes).find(lambda li: li.game_index.game_path == index.game_path)
        if loaded_index is not None:
            return loaded_index
        else:
            features, labels = np.load(self.get_feature_path(index.game_path)), np.load(
                self.get_label_path(index.game_path))
            self.loaded_indexes.append(LoadedIndex(index, features, labels))
            if len(self.loaded_indexes) > self.max_load_count:
                self.loaded_indexes.pop(0)
            return self.loaded_indexes[-1]

    def get_train_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_training_range()), 12, 12, self.channel_count)]

    def get_crossval_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_cross_val_range()), 12, 12, self.channel_count)]

    def get_test_feature_shape(self) -> List[tuple]:
        return [(self.range_len(self.get_test_range()), 12, 12, self.channel_count)]
