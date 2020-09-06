from typing import List, Dict

from training.game_state.game_map import Direction, Position
from training.neural_network.position_state import PositionState


class AntVision1DExample:
    def __init__(self, features: List[PositionState], label: Direction):
        self.features = features
        self.label = label


class AntVision2DExample:
    def __init__(self, features: Dict[Position, PositionState], label: Direction):
        self.features = features
        self.label = label


class AntMapExample:
    def __init__(self, features: Dict[Position, PositionState], label: Direction):
        self.features = features
        self.label = label


class AntMapDataset:
    def __init__(self, row_count: int, column_count: int, examples: List[AntMapExample]):
        self.examples = examples
        self.column_count = column_count
        self.row_count = row_count
