from enum import Enum
from typing import List, Type, TypeVar, Dict, Tuple

from ants_ai.training.game_state.ant_turn import AntTurn
from ants_ai.training.game_state.game_map import Position, TerrainType
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.game_state.game_turn import GameTurn
from ants_ai.training.neural_network.encoders.neural_network_example import AntVision1DExample, AntMapExample, \
    AntVision2DExample
from ants_ai.training.neural_network.encoders.position_state import PositionState
from functional import seq

T = TypeVar('T', bound=Enum)


class GameStateTranslator:
    def __init__(self):
        self.cached_types: Dict[(Type[T]), Dict[T, List[int]]] = {}

    def cache_enum_type(self, enum_class):
        items = enum_class.__members__.items()
        self.cached_types[enum_class] = seq(items) \
            .map(lambda t0: (t0[1], seq(items).map(lambda t1: 1 if t1[1] == t0[1] else 0).to_list())) \
            .to_dict()

    def convert_enum_to_array(self, enum_val: T, enum_class: Type[T]) -> List[int]:
        if self.cached_types.get(enum_class) is None:
            self.cache_enum_type(enum_class)
        return self.cached_types[enum_class].get(enum_val)

    def convert_array_to_enum(self, int_array: List[int], enum_class: Type[T]) -> T:
        enum_items = enum_class.__members__.items()
        if len(enum_items) != len(int_array): raise ValueError(
            f'array mismatch enumCount: ${len(enum_items)} boolCount:${len(int_array)}')
        if 1 in int_array:
            return list(enum_items)[int_array.index(1)][1]
        else:
            return None

    def convert_pos_to_state(self, pos: Position, bot_name: str, turn_state: GameTurn, game_state: GameState):
        if turn_state.ants.get(pos) is not None:
            return PositionState.FRIENDLY_ANT if turn_state.ants[
                                                     pos].bot.bot_name == bot_name else PositionState.HOSTILE_ANT
        if turn_state.hills.get(pos) is not None and turn_state.hills[pos].is_alive:
            return PositionState.FRIENDLY_HILL if turn_state.hills[
                                                      pos].owner_bot.bot_name == bot_name else PositionState.HOSTILE_HILL
        if turn_state.foods.get(pos) is not None:
            return PositionState.FOOD
        return PositionState.WATER if game_state.game_map.terrain[
                                          pos] == TerrainType.WATER else PositionState.LAND

    def convert_to_1d_example(self, ant_turn: AntTurn, game_state: GameState) -> AntVision1DExample:
        ant_vision = [p for p in game_state.game_map.get_positions_within_distance(ant_turn.position,
                                                                                   game_state.view_radius_squared)
                      if p != ant_turn.position]
        turn_state = game_state.game_turns[ant_turn.turn_number]
        return AntVision1DExample(
            [self.convert_pos_to_state(av, ant_turn.bot.bot_name, turn_state, game_state) for av in ant_vision],
            ant_turn.next_direction)

    def convert_to_2d_example(self, ant_turn: AntTurn, game_state: GameState) -> AntVision2DExample:
        gm = game_state.game_map
        atp = ant_turn.position
        ant_vision = [p for p in gm.get_positions_within_distance(ant_turn.position,
                                                                  game_state.view_radius_squared,
                                                                  use_absolute=False,
                                                                  crop_to_square=True)
                      ]
        turn_state = game_state.game_turns[ant_turn.turn_number]
        return AntVision2DExample(
            {av: self.convert_pos_to_state(gm.wrap_position(atp.row + av.row, atp.column + av.column),
                                           ant_turn.bot.bot_name, turn_state, game_state)
             for av in ant_vision},
            ant_turn.next_direction)

    def convert_to_map_example(self, bot_name: str, turn_number, game_state: GameState) -> Dict[
        Position, PositionState]:
        turn_state = game_state.game_turns[turn_number]
        map_positions = seq(game_state.game_map.terrain.keys()) \
            .map(lambda p: (p, self.convert_pos_to_state(p, bot_name, turn_state, game_state))) \
            .to_dict()
        return map_positions

    def convert_to_global_antmap_example(self, ant_turn: AntTurn, game_state: GameState) -> AntMapExample:
        gm = game_state.game_map
        atp = ant_turn.position
        ant_vision = gm.get_positions_within_distance(ant_turn.position,
                                                      game_state.view_radius_squared,
                                                      use_absolute=False,
                                                      crop_to_square=True)

        turn_state = game_state.game_turns[ant_turn.turn_number]
        position_states = {p: self.convert_pos_to_state(gm.wrap_position(atp.row + p.row, atp.column + p.column),
                                                        ant_turn.bot.bot_name, turn_state, game_state) \
            if p not in ant_vision else PositionState.WATER if game_state.game_map.terrain.get(
            p) == TerrainType.WATER else PositionState.LAND
                           for p in game_state.game_map.terrain.keys()}
        return AntMapExample(position_states, ant_turn.next_direction, gm.row_count, gm.column_count)

    def convert_to_1d_ant_vision(self, bot_name: str, game_states: List[GameState]) -> List[AntVision1DExample]:
        examples = [self.convert_to_1d_example(at, gs) \
                    for gs in game_states for gt in gs.game_turns for at in gt.ants.values() \
                    if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]
        return examples

    def convert_to_2d_ant_vision(self, bot_name: str, game_states: List[GameState]) -> List[AntVision2DExample]:
        examples = [self.convert_to_2d_example(at, gs) \
                    for gs in game_states for gt in gs.game_turns for at in gt.ants.values() \
                    if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]
        return examples

    def convert_to_global_antmap(self, bot_name: str, game_states: List[GameState]) -> List[AntMapExample]:
        examples = [self.convert_to_global_antmap_example(at, gs) \
                    for gs in game_states for gt in gs.game_turns for at in gt.ants.values() \
                    if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]
        return examples

    def convert_to_antmap(self, bot_name: str, game_states: List[GameState]) -> List[AntMapExample]:
        map_states: Dict[Tuple[str, int], Dict[
            Position, PositionState]] = seq(game_states) \
            .flat_map(lambda gs: seq(gs.game_turns).map(lambda gt: (gs, gt))) \
            .filter(lambda t: t[1].turn_number <= t[0].ranking_turn + 1) \
            .map(
            lambda t: (
                (t[0].game_id, t[1].turn_number), self.convert_to_map_example(bot_name, t[1].turn_number, t[0]))) \
            .to_dict()
        # Need to check all maps in set are identical.
        game_map = game_states[0].game_map
        examples = [
            AntMapExample(map_states[(gs.game_id, gt.turn_number)], at.next_direction, game_map.row_count,
                          game_map.column_count)
            for gs in game_states for gt in gs.game_turns for at in gt.ants.values()
            if (gt.turn_number <= gs.ranking_turn + 1) and at.bot.bot_name == bot_name]

        return examples
