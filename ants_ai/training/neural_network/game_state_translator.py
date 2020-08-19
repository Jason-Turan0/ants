from functional import seq
from training.game_state.ant_turn import AntTurn
from training.game_state.game_map import Position, Direction, TerrainType
from training.game_state.game_state import GameState
from typing import List, Type, TypeVar, Dict
from enum import Enum

from training.neural_network.neural_network_example import NeuralNetworkExample


class PositionState(Enum):
    FRIENDLY_ANT = 0,
    HOSTILE_ANT = 1,
    FRIENDLY_HILL = 2,
    HOSTILE_HILL = 3,
    FOOD = 4,
    WATER = 5,
    LAND = 6


T = TypeVar('T', bound=Enum)


class GameStateTranslater:
    def __init__(self):
        self.cached_types: Dict[(Type[T]), Dict[T, List[bool]]] = {}

    def cache_enum_type(self, enum_class):
        items = enum_class.__members__.items()
        self.cached_types[enum_class] = seq(items) \
            .map(lambda t0: (t0[1], seq(items).map(lambda t1: t1[1] == t0[1]).to_list())) \
            .to_dict()

    def convert_enum_to_array(self, enum_val: T, enum_class: Type[T]) -> List[bool]:
        if self.cached_types.get(enum_class) is None:
            self.cache_enum_type(enum_class)
        return  self.cached_types[enum_class].get(enum_val)


    def convert_array_to_enum(self, bool_array: List[bool], enum_class: Type[T]) -> T:
        enum_items = enum_class.__members__.items()
        if len(enum_items) != len(bool_array): raise ValueError(
            f'array mismatch enumCount: ${len(enum_items)} boolCount:${len(bool_array)}')
        if True in bool_array:
            return list(enum_items)[bool_array.index(True)][1]
        else:
            return None

    def convert_to_example(self, ant_turn: AntTurn, game_state: GameState) -> NeuralNetworkExample:
        ant_vision = game_state.game_map.get_positions_within_distance(ant_turn.position,
                                                                       game_state.view_radius_squared)
        turn_state = game_state.game_turns[ant_turn.turn_number]

        def convert_pos_to_state(position, game_state):
            if turn_state.ants.get(position) is not None:
                return PositionState.FRIENDLY_ANT if turn_state.ants[
                                                         position].bot.bot_name == ant_turn.bot.bot_name else PositionState.HOSTILE_ANT
            if turn_state.hills.get(position) is not None and turn_state.hills[position].is_alive:
                return PositionState.FRIENDLY_HILL if turn_state.hills[
                                                          position].owner_bot.bot_name == ant_turn.bot.bot_name else PositionState.HOSTILE_HILL
            if turn_state.foods.get(position) is not None:
                return PositionState.FOOD
            return PositionState.WATER if game_state.game_map.terrain[
                                              position] == TerrainType.WATER else PositionState.LAND

        nn_input = seq(ant_vision) \
            .filter(lambda p: p != ant_turn.position) \
            .map(lambda p: convert_pos_to_state(p, game_state)) \
            .flat_map(lambda ps: self.convert_enum_to_array(ps, PositionState)) \
            .list()

        return NeuralNetworkExample(nn_input, self.convert_enum_to_array(ant_turn.next_direction, Direction))
