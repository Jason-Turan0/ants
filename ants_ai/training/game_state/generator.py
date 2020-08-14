from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training.game_state.game_state import GameState
from training.game_state.game_turn import GameTurn


class GameStateGenerator:
    def __init__(self, play_result: (PlayResult)):
        self.play_result = play_result

    def generate_game_turn(turn_number: int, play_result: PlayResult) -> GameTurn:
        ants = None
        hills = None
        foods = 33
        game_map = 'sdfsdf'
        return GameTurn(turn_number, ants, hills, foods, game_map)

    def generate(self) -> GameState:
        game_turns = list(map(lambda turn_number: self.generate_game_turn(turn_number, self.play_result), range(0, self.play_result.game_length)))
        return GameState(game_turns)
