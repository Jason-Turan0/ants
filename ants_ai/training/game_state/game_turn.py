from typing import List
from ants_ai.training.game_state.hill_turn import HillTurn
from training.game_state.ant_turn import AntTurn
from training.game_state.game_map import GameMap
from training.game_state.food_turn import FoodTurn
class GameTurn():
    def __init__(self, turn_number : int, ants: List[AntTurn], hills: List[HillTurn], foods: List[FoodTurn], game_map: GameMap):
        self.turn_number = turn_number
        self.ants = ants
        self.hills = hills
        self.foods = foods
        self.game_map = game_map
