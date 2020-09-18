from typing import List
from ants_ai.training.game_state.game_turn import GameTurn
from ants_ai.training.game_state.game_map import GameMap
from ants_ai.engine.bot import BotName


class GameState:
    def __init__(self,
                 game_id: str,
                 game_turns: List[GameTurn],
                 game_map: GameMap,
                 view_radius_squared: int,
                 winning_bot: BotName,
                 ranking_turn: int):
        self.game_id = game_id
        self.winning_bot = winning_bot
        self.game_turns = game_turns
        self.game_map = game_map
        self.view_radius_squared = view_radius_squared
        self.ranking_turn = ranking_turn
