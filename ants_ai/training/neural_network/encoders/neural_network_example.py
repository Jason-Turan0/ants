from typing import List, Dict

from ants_ai.training.game_state.game_map import Direction, Position
from ants_ai.training.neural_network.encoders.position_state import PositionState


class AntVision1DExample:
    def __init__(self, features: List[PositionState], label: Direction):
        self.features = features
        self.label = label


class AntVision2DExample:
    def __init__(self, features: Dict[Position, PositionState], label: Direction):
        self.features = features
        self.label = label


class AntMapExample:
    def __init__(self, features: Dict[Position, PositionState], label: Direction, row_count: int, column_count: int):
        self.column_count = column_count
        self.row_count = row_count
        self.features = features
        self.label = label
