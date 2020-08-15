from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training.game_state.game_state import GameState
from functional import seq
from training.game_state.ant_turn import AntTurn
from training.game_state.food_turn import FoodTurn
from training.game_state.game_map import GameMap, Position, Direction
from training.game_state.game_turn import GameTurn
import ants_ai.training_data_gen.engine.bot as bot
from ants_ai.training_data_gen.engine.bot import Bot

from typing import List

from training.game_state.hill_turn import HillTurn


class GameStateGenerator:
    def __init__(self, play_result: (PlayResult)):
        self.play_result = play_result

    def generate_game_turn(self, turn_number: int, bots: List[Bot], game_map: GameMap,
                           play_result: PlayResult) -> GameTurn:
        def map_ant_turn(row) -> AntTurn:
            start_row, start_col, start_turn, end_turn, bot_index, direction_history = row
            position_at_turn = seq(range(turn_number - start_turn)) \
                .map(lambda movement_index: Direction(direction_history[movement_index])) \
                .reduce(lambda position, direction: game_map.adjacent_movement_position(position, direction),
                        Position(start_row, start_col))

            nextDirectionCode = direction_history[(turn_number- start_turn)] if (turn_number - start_turn) < len(direction_history) else None
            return AntTurn(turn_number, bots[bot_index], position_at_turn, row, Direction(nextDirectionCode))

        ants = seq(play_result.replaydata.ants) \
            .filter(lambda row: (turn_number >= row[2]) and (turn_number < (row[3]))) \
            .map(map_ant_turn) \
            .list()

        def map_hill_turn(row):
            row_num, col_num, bot_index, capture_turn = row
            is_alive = turn_number < capture_turn
            return HillTurn(turn_number, bots[bot_index], Position(row_num, col_num), is_alive)

        hills = list(map(map_hill_turn, play_result.replaydata.hills))

        foods = seq(play_result.replaydata.food) \
            .filter(lambda row: (turn_number >= row[2]) and (turn_number < row[3])) \
            .map(lambda row: FoodTurn(turn_number, Position(row[0], row[1]))) \
            .to_list()

        return GameTurn(turn_number, ants, hills, foods, game_map)

    def generate(self) -> GameState:
        game_map = GameMap(self.play_result.replaydata.map)
        bots: List[Bot] = list(map(bot.from_name, self.play_result.playernames))
        game_turns = list(
            map(lambda turn_number: self.generate_game_turn(turn_number, bots, game_map, self.play_result),
                range(0, self.play_result.game_length)))
        return GameState(game_turns)
