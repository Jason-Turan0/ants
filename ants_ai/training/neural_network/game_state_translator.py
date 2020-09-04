from functional import seq
from training.game_state.ant_turn import AntTurn
from training.game_state.game_map import Position, Direction, TerrainType
from training.game_state.game_state import GameState
from typing import List, Type, TypeVar, Dict
from enum import Enum

from training.game_state.game_turn import GameTurn
from training.neural_network.neural_network_example import NeuralNetworkExample
from training.neural_network.nueral_network_dataset import NeuralNetworkDataset
from training.neural_network.position_state import PositionState

TRAINING_VIEW_RADIUS = 77;
TRAINING_VIEW_SIZE = 241;

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
        return self.cached_types[enum_class].get(enum_val)

    def convert_array_to_enum(self, bool_array: List[bool], enum_class: Type[T]) -> T:
        enum_items = enum_class.__members__.items()
        if len(enum_items) != len(bool_array): raise ValueError(
            f'array mismatch enumCount: ${len(enum_items)} boolCount:${len(bool_array)}')
        if True in bool_array:
            return list(enum_items)[bool_array.index(True)][1]
        else:
            return None

    def convert_pos_to_state(self, pos:Position, ant_turn: AntTurn, turn_state: GameTurn, game_state: GameState):
        if turn_state.ants.get(pos) is not None:
            return PositionState.FRIENDLY_ANT if turn_state.ants[
                                                     pos].bot.bot_name == ant_turn.bot.bot_name else PositionState.HOSTILE_ANT
        if turn_state.hills.get(pos) is not None and turn_state.hills[pos].is_alive:
            return PositionState.FRIENDLY_HILL if turn_state.hills[
                                                      pos].owner_bot.bot_name == ant_turn.bot.bot_name else PositionState.HOSTILE_HILL
        if turn_state.foods.get(pos) is not None:
            return PositionState.FOOD
        return PositionState.WATER if game_state.game_map.terrain[
                                          pos] == TerrainType.WATER else PositionState.LAND

    def convert_to_example_ant_vision(self, ant_turn: AntTurn, game_state: GameState) -> NeuralNetworkExample:
        ant_vision = seq(game_state.game_map.get_positions_within_distance(ant_turn.position,
                                                                           game_state.view_radius_squared)) \
            .filter(lambda p: p != ant_turn.position) \
            .to_list()
        turn_state = game_state.game_turns[ant_turn.turn_number]
        return NeuralNetworkExample([self.convert_pos_to_state(av,ant_turn,turn_state, game_state) for av in ant_vision],
                                    ant_turn.next_direction)

    def convert_to_example_entire_map(self, ant_turn: AntTurn, game_state: GameState) -> NeuralNetworkExample:
        map_positions = seq(game_state.game_map.terrain.keys()) \
            .order_by(lambda p: p)\
            .filter(lambda p: p != ant_turn.position) \
            .to_list()
        turn_state = game_state.game_turns[ant_turn.turn_number]
        return NeuralNetworkExample([self.convert_pos_to_state(pos,ant_turn,turn_state, game_state) for pos in map_positions],
                                    ant_turn.next_direction)

    def convert_to_nn_input_ant_vision(self, bot_name: str, game_states: List[GameState]) -> NeuralNetworkDataset:
        examples = [self.convert_to_example_ant_vision(at, gs) \
                    for gs in game_states for gt in gs.game_turns for at in gt.ants.values() \
                    if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]
        return NeuralNetworkDataset(examples)


    def convert_to_nn_input_entire_map(self, bot_name: str, game_states: List[GameState]) -> NeuralNetworkDataset:
        examples = [self.convert_to_example_entire_map(at, gs) \
                    for gs in game_states for gt in gs.game_turns for at in gt.ants.values() \
                    if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]
        return NeuralNetworkDataset(examples)