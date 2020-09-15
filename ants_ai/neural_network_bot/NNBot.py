from pprint import pprint
from typing import Dict, List, Optional, Tuple, Set

import numpy
from engine.bot import Bot
from engine.bot_name import BotName
from ants_ai.training.game_state.game_map import create_map, GameMap, Position, TerrainType, Direction
from functional import seq
import training.neural_network.model_factory as mf
from training.neural_network.encoders import encode_2d_features
from training.neural_network.game_state_translator import GameStateTranslator
from training.neural_network.position_state import PositionState


class VisibleAnt:
    def __init__(self, position: Position, owning_bot: int):
        self.position = position
        self.owning_bot = owning_bot

    def is_friendly(self):
        return self.owning_bot == 0


class Order:
    def __init__(self, position: Position, dir: Direction):
        self.position = position
        self.dir = dir

    def __str__(self):
        return f'o {self.position.row} {self.position.column} {self.dir.value}'


class VisibleHill:
    def __init__(self, position: Position, owning_bot: int):
        self.position = position
        self.owning_bot = owning_bot

    def is_friendly(self):
        return self.owning_bot == 0


class NNBot(Bot):
    def __init__(self, game_identifier: str, name: BotName, weight_path: str):
        super().__init__(game_identifier, name)
        self.game_options: Dict[str, int] = {}
        self.game_map: GameMap = None
        self.visible_ants: Dict[Position, VisibleAnt] = {}
        self.visible_food: Set[Position] = set()
        self.visible_hills: Dict[Position, VisibleHill] = {}
        self.pending_orders: List[Order] = []

        self.channel_count = 7
        self.gst = GameStateTranslator()
        ms = mf.create_conv_2d_model(0.001, 1, '', self.channel_count)
        self.model = ms.model
        self.model.load_weights(weight_path)

    def start(self, start_data: str):
        print(f'Start NNBOT')
        self.game_options = seq(start_data.split('\n')) \
            .filter(lambda line: line != '') \
            .map(lambda opt: (opt.split(' ')[0], int(opt.split(' ')[1]))) \
            .to_dict()
        # pprint(self.game_options)
        self.game_map = create_map(self.game_options['rows'], self.game_options['cols'])

    def convert_pos_to_state(self, pos: Position) -> PositionState:
        if self.visible_ants.get(pos) is not None:
            return PositionState.FRIENDLY_ANT if self.visible_ants[
                pos].is_friendly() else PositionState.HOSTILE_ANT
        if self.visible_hills.get(pos) is not None:
            return PositionState.FRIENDLY_HILL if self.visible_hills.get(
                pos).is_friendly() else PositionState.HOSTILE_HILL
        if pos in self.visible_food:
            return PositionState.FOOD
        return PositionState.WATER if self.game_map.get_terrain(pos) == TerrainType.WATER else PositionState.LAND

    def create_prediction(self, ant: VisibleAnt) -> Order:
        ant_vision = [p for p in self.game_map.get_positions_within_distance(ant.position,
                                                                             self.game_options['viewradius2'],
                                                                             use_absolute=False,
                                                                             crop_to_square=True)
                      ]
        position_states = {av: self.convert_pos_to_state(
            self.game_map.wrap_position(ant.position.row + av.row, ant.position.column + av.column)
        ) for av in ant_vision}
        features = encode_2d_features([position_states], self.gst, self.channel_count)
        prediction = self.model.predict(features)
        pred = [0] * 5
        pred[numpy.array(prediction[0]).argmax()] = 1
        d: Direction = self.gst.convert_array_to_enum(pred, Direction)
        return Order(ant.position, d)

    def play_turn(self, play_turn_data: str):
        # print(f'Play NNBOT')
        # print(play_turn_data)

        def parse_segments(line: str) -> Tuple[str, Position, Optional[int]]:
            segments = line.split(' ')
            return segments[0], Position(int(segments[1]), int(segments[2])), int(segments[3]) if len(
                segments) == 4 else None

        input_data = seq(play_turn_data.split('\n')) \
            .filter(lambda line: line != '') \
            .map(parse_segments) \
            .to_list()

        seq(input_data).filter(lambda t: t[0] == 'w').for_each(
            lambda t: self.game_map.update_terrain(t[1], TerrainType.WATER))
        self.visible_hills = seq(input_data).filter(lambda t: t[0] == 'h').map(
            lambda t: (t[1], VisibleHill(t[1], t[2]))).to_dict()
        self.visible_ants = seq(input_data).filter(lambda t: t[0] == 'a').map(
            lambda t: (t[1], VisibleAnt(t[1], t[2]))).to_dict()
        self.visible_food = seq(input_data).filter(lambda t: t[0] == 'f').map(lambda t: t[1]).to_set()

        self.pending_orders = seq(self.visible_ants.values()) \
            .filter(lambda a: a.is_friendly()) \
            .map(lambda a: self.create_prediction(a)) \
            .to_list()

    def read_lines(self):
        orders = seq(self.pending_orders).map(lambda o: str(o)).to_list()
        # pprint(orders)
        return orders
