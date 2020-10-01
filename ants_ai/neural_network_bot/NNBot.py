from typing import Dict, List, Optional, Tuple, Set
import numpy
from functional import seq
from tensorflow import keras

from ants_ai.training.game_state.game_map import create_map, GameMap, Position, TerrainType, Direction
from ants_ai.engine.bot import Bot
from ants_ai.engine.bot_name import BotName
from ants_ai.training.neural_network.encoders import encode_2d_features
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.position_state import PositionState


class VisibleAnt:
    def __init__(self, position: Position, owning_bot: int):
        self.position = position
        self.owning_bot = owning_bot

    def is_friendly(self):
        return self.owning_bot == 0


class Order:
    def __init__(self, position: Position, d: Direction, next_position: Position):
        self.position = position
        self.dir = d
        self.next_position = next_position

    def __str__(self):
        return f'o {self.position.row} {self.position.column} {self.dir.value}'


class VisibleHill:
    def __init__(self, position: Position, owning_bot: int):
        self.position = position
        self.owning_bot = owning_bot

    def is_friendly(self):
        return self.owning_bot == 0


class NNBot(Bot):
    def __init__(self, game_identifier: str, name: BotName, model_path: str):
        super().__init__(game_identifier, name)
        self.game_options: Dict[str, int] = {}
        self.game_map: GameMap = None
        self.visible_ants: Dict[Position, VisibleAnt] = {}
        self.visible_food: Set[Position] = set()
        self.visible_hills: Dict[Position, VisibleHill] = {}
        self.pending_orders: List[Order] = []
        self.channel_count = 7
        self.gst = GameStateTranslator()
        self.model = keras.models.load_model(model_path)

    def start(self, start_data: str):
        self.game_options = seq(start_data.split('\n')) \
            .filter(lambda line: line != '') \
            .map(lambda opt: (opt.split(' ')[0], int(opt.split(' ')[1]))) \
            .to_dict()
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

    def create_predictions(self, ants: List[VisibleAnt]) -> List[Tuple[VisibleAnt, Direction]]:

        def map_to_position_state(ant: VisibleAnt) -> Dict[Position, PositionState]:
            ant_vision = self.game_map.get_positions_within_distance(ant.position,
                                                                     self.game_options['viewradius2'],
                                                                     use_absolute=False,
                                                                     crop_to_square=True)
            position_states = {av: self.convert_pos_to_state(
                self.game_map.wrap_position(ant.position.row + av.row, ant.position.column + av.column)
            ) for av in ant_vision}
            return position_states

        def convert_prediction_to_direction(ant: VisibleAnt, prediction: List) -> Tuple[VisibleAnt, Direction]:
            pred = [0] * 5
            pred[numpy.array(prediction).argmax()] = 1
            d: Direction = self.gst.convert_array_to_enum(pred, Direction)
            return ant, d

        position_states = seq(ants).map(map_to_position_state).to_list()
        features = encode_2d_features(position_states, self.gst, self.channel_count)
        predictions = self.model.predict(features)
        mapped_predictions = [convert_prediction_to_direction(ants[index], prediction) for index, prediction in
                              enumerate(predictions)]
        return mapped_predictions

    def create_orders(self) -> List[Order]:
        friendly_ants = seq(self.visible_ants.values()) \
            .filter(lambda a: a.is_friendly()) \
            .to_list()

        pending_orders = seq(friendly_ants) \
            .map(lambda va: Order(va.position_start, Direction.NONE,
                                  self.game_map.adjacent_movement_position(va.position_start, Direction.NONE))) \
            .to_list()
        predictions: List[Tuple[VisibleAnt, Direction]] = self.create_predictions(friendly_ants)

        pass_through_count = 0
        while seq(pending_orders).filter(lambda po: po.dir == Direction.NONE).len() > 0 and pass_through_count < 3:
            for index, order in enumerate(pending_orders):
                # pylint: disable=cell-var-from-loop
                matching_prediction = seq(predictions).find(lambda t: t[0].position_start == order.position_start)
                new_order_position = self.game_map.adjacent_movement_position(order.position_start,
                                                                              matching_prediction[1])
                # pylint: disable=cell-var-from-loop
                conflicting_order = seq(pending_orders).find(lambda po: po.next_position == new_order_position)
                if conflicting_order is None:
                    pending_orders[index] = Order(order.position_start, matching_prediction[1], new_order_position)
            pass_through_count += 1
        return pending_orders

    def play_turn(self, play_turn_data: str):
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
        self.pending_orders = self.create_orders()

    def read_lines(self):
        orders = seq(self.pending_orders).map(str).to_list()
        return orders
