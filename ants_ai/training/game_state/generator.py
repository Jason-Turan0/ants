import jsonpickle
from ants_ai.engine.play_result import PlayResult
from ants_ai.training.game_state.game_state import GameState
from functional import seq
from ants_ai.training.game_state.ant_turn import AntTurn
from ants_ai.training.game_state.food_turn import FoodTurn
from ants_ai.training.game_state.game_map import GameMap, Position, Direction, create_from_map_data
from ants_ai.training.game_state.game_turn import GameTurn
import ants_ai.engine.bot_name as bot_name
from ants_ai.engine.bot_name import BotName

from typing import List

from ants_ai.training.game_state.hill_turn import HillTurn


class GameStateGenerator:

    def generate_game_turn(self, turn_number: int, bots: List[BotName], game_map: GameMap,
                           play_result: PlayResult) -> GameTurn:
        def map_ant_turn(row):
            start_row, start_col, start_turn, _, bot_index, direction_history = row
            position_at_turn = seq(range(turn_number - start_turn)) \
                .map(lambda movement_index: Direction(direction_history[movement_index])) \
                .reduce(game_map.adjacent_movement_position, Position(start_row, start_col))
            next_direction_code = direction_history[(turn_number - start_turn)] if (turn_number - start_turn) < len(
                direction_history) else None
            return position_at_turn, AntTurn(turn_number, bots[bot_index], position_at_turn, row,
                                             Direction(next_direction_code))

        ants = seq(play_result.replaydata.ants) \
            .filter(lambda row: (turn_number >= row[2]) and (turn_number < (row[3]))) \
            .map(map_ant_turn) \
            .to_dict()

        def map_hill_turn(row):
            row_num, col_num, bot_index, capture_turn = row
            is_alive = turn_number < capture_turn
            return Position(row_num, col_num), HillTurn(turn_number, bots[bot_index], Position(row_num, col_num),
                                                        is_alive)

        hills = seq(play_result.replaydata.hills).map(map_hill_turn).to_dict()

        foods = seq(play_result.replaydata.food) \
            .filter(lambda row: (turn_number >= row[2]) and (turn_number < row[3])) \
            .map(lambda row: (Position(row[0], row[1]), FoodTurn(turn_number, Position(row[0], row[1])))) \
            .to_dict()

        return GameTurn(turn_number, ants, hills, foods, game_map)

    def generate(self, play_result: PlayResult) -> GameState:
        game_map = create_from_map_data(play_result.replaydata.map)
        bots: List[BotName] = list(map(bot_name.from_name, play_result.playernames))
        game_turns = list(
            map(lambda turn_number: self.generate_game_turn(turn_number, bots, game_map, play_result),
                range(0, play_result.game_length)))
        return GameState(play_result.game_id, game_turns, game_map, play_result.replaydata.viewradius2,
                         bots[play_result.rank[0]],
                         play_result.replaydata.ranking_turn)

    def generate_from_file(self, play_result_path: str):
        with open(play_result_path, "r") as f:
            json_data = f.read()
            pr = jsonpickle.decode(json_data)
            return self.generate(pr)
