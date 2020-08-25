from ants_ai.training_data_gen.engine.map_data import MapData
from typing import List, Any


class ReplayData:
    def __init__(self, dict):
        self.ants: List[List[Any]] = dict["ants"]
        self.attackradius2: int = dict['attackradius2']
        self.bonus: List[int] = dict['bonus']
        self.cutoff: str = dict['cutoff']
        self.engine_seed: int = dict['engine_seed']
        self.food: List[List[int]] = dict['food']
        self.food_rate: int = dict['food_rate']
        self.food_start: int = dict['food_start']
        self.food_turn: int = dict['food_turn']
        self.hills: List[List[int]] = dict['hills']
        self.hive_history: List[List[int]] = dict['hive_history']
        self.loadtime: int = dict['loadtime']
        self.map: MapData = MapData(dict['map']['cols'], dict['map']['data'], dict['map']['rows'])
        self.player_seed: int = dict['player_seed']
        self.players: int = dict['players']
        self.ranking_turn: int = dict['ranking_turn']
        self.revision: int = dict['revision']
        self.scores: List[List[int]] = dict['scores']
        self.spawnradius2: int = dict['spawnradius2']
        self.turns: int = dict['turns']
        self.turntime: int = dict['turntime']
        self.viewradius2: int = dict['viewradius2']
        self.winning_turn: int = dict['winning_turn']
