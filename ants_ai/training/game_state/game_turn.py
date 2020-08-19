from typing import List, Dict
from ants_ai.training.game_state.hill_turn import HillTurn
from training.game_state.ant_turn import AntTurn
from training.game_state.game_map import GameMap, Position
from training.game_state.food_turn import FoodTurn
class GameTurn():
    def __init__(self, turn_number : int, ants: Dict[Position, AntTurn], hills: Dict[Position, HillTurn], foods: Dict[Position, FoodTurn], game_map: GameMap):
        self.turn_number = turn_number
        self.ants = ants
        self.hills = hills
        self.foods = foods
        self.game_map = game_map
