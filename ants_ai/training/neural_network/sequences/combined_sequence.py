import os
from typing import List, Tuple

import ants_ai.training.neural_network.encoders.encoders as enc
from ants_ai.training.neural_network.sequences.data_structs import GameIndex, LoadedIndex, DatasetType
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.sequences.ant_vision_sequence import AntVisionSequence
from ants_ai.training.neural_network.sequences.map_view_sequence import MapViewSequence
import numpy as np
import sklearn as sk
from functional import seq


class CombinedSequence(FileSystemSequence):
    def __init__(self, game_paths: List[str], batch_size: int, bot_to_emulate: str, channel_count=7,
                 dataset_type: DatasetType = DatasetType.TRAINING):
        super().__init__(game_paths, batch_size, bot_to_emulate, channel_count, dataset_type)

    def get_av_feature_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_COMBINED_AV_FEATURES_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_av_label_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_COMBINED_AV_LABELS_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_map_feature_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_COMBINED_MAP_FEATURES_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_map_label_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_COMBINED_MAP_LABELS_{self.bot_to_emulate}_{self.channel_count}.npy')

    def get_index_path(self, game_path: str) -> str:
        return game_path.replace('.json',
                                 f'_COMBINED_index_{self.bot_to_emulate}_{self.channel_count}.txt')

    def build_indexes(self, rebuild: bool):
        self.game_indexes = []
        for game_path in self.game_paths:

            if os.path.exists(self.get_index_path(game_path)) and not rebuild:
                self.game_indexes.append(self.read_config(game_path, self.get_index_path(game_path)))
            else:
                gs = self.load_game_state(game_path)
                ant_vision = self.gst.convert_to_2d_ant_vision(self.bot_to_emulate, [gs])
                map = self.gst.convert_to_global_antmap(self.bot_to_emulate, [gs])
                av_features, av_labels = enc.encode_2d_examples(ant_vision, self.channel_count)
                map_features, map_labels = enc.encode_map_examples(map, self.channel_count)
                shuffled_av_features, shuffled_av_labels, shuffled_map_features, shuffled_map_labels = \
                    sk.utils.shuffle(av_features, av_labels, map_features, map_labels)

                print(f'Saving {av_features.shape[0]} examples to {self.get_av_feature_path(game_path)}')
                np.save(self.get_av_feature_path(game_path), shuffled_av_features)
                np.save(self.get_av_label_path(game_path), shuffled_av_labels)
                np.save(self.get_map_feature_path(game_path), shuffled_map_features)
                np.save(self.get_map_label_path(game_path), shuffled_map_labels)

                self.write_config(self.get_index_path(game_path), av_features.shape[0])
                self.game_indexes.append(self.read_config(game_path, self.get_index_path(game_path)))

    def load_index(self, index: GameIndex) -> LoadedIndex:
        loaded_index = seq(self.loaded_indexes).find(lambda li: li.game_index.game_path == index.game_path)
        if loaded_index is not None:
            return loaded_index
        else:
            av_features, av_labels = np.load(self.get_av_feature_path(index.game_path)), np.load(
                self.get_av_label_path(index.game_path))
            map_features, map_labels = np.load(self.get_map_feature_path(index.game_path)), np.load(
                self.get_map_label_path(index.game_path))
            self.loaded_indexes.append(LoadedIndex(index, [av_features, map_features], av_labels))
            if len(self.loaded_indexes) > self.max_load_count:
                self.loaded_indexes.pop(0)
            return self.loaded_indexes[-1]

    def get_feature_shape(self, index_range: Tuple[int, int]) -> List[tuple]:
        range_len = self.range_len(index_range)
        return [(range_len, 12, 12, self.channel_count),
                (range_len, 49, 39, self.channel_count)]

    def get_train_feature_shape(self) -> List[tuple]:
        return self.get_feature_shape(self.get_training_range())

    def get_crossval_feature_shape(self) -> List[tuple]:
        return self.get_feature_shape(self.get_cross_val_range())

    def get_test_feature_shape(self) -> List[tuple]:
        return self.get_feature_shape(self.get_test_range())
