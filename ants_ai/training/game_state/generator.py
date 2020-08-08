from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training.game_state.game_state import GameState
class GameStateGenerator:
    def __init__(self, play_result :(PlayResult)):
        self.play_result = play_result

    def generate(self) -> GameState:
        None