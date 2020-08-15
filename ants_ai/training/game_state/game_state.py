from typing import List
from ants_ai.training.game_state.game_turn import GameTurn
class GameState:
    def __init__(self,
                game_turns : List[GameTurn]):
        self.game_turns = game_turns
        