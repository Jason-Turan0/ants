from enum import IntEnum
from typing import Union, List

import numpy as np


class GameIndex:
    def __init__(self, game_path: str, position_start: int, length: int):
        self.game_path = game_path
        self.position_start = position_start
        self.length = length
        self.position_end = self.position_start + self.length


class LoadedIndex:
    def __init__(self, game_index: GameIndex, features: Union[np.ndarray, List[np.ndarray]], labels: np.ndarray):
        self.game_index = game_index
        self.features = features
        self.labels = labels


class DatasetType(IntEnum):
    TRAINING = 1
    CROSS_VAL = 2
    TEST = 3
