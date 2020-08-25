from typing import List
from ants_ai.training.game_state.game_turn import GameTurn
from training.game_state.game_map import GameMap
from training_data_gen.engine.bot import Bot


class GameState:
    def __init__(self,
                game_turns : List[GameTurn],
                 game_map : GameMap,
                 view_radius_squared: int,
                 winning_bot: Bot,
                 ranking_turn: int):
        self.winning_bot = winning_bot
        self.game_turns = game_turns
        self.game_map = game_map
        self.view_radius_squared = view_radius_squared
        self.ranking_turn= ranking_turn
        