from typing import List

from training.game_state.game_map import Direction
from training.neural_network.position_state import PositionState


class NeuralNetworkExample:
    def __init__(self, features: List[PositionState], label: Direction):
        self.features= features
        self.label = label

