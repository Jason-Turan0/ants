import os
import jsonpickle
from training.game_state.game_state import GameState
from training.game_state.generator import GameStateGenerator
from training_data_gen.engine.play_result import PlayResult


def get_play_result(dataPath) -> PlayResult:
    f = open(dataPath, "r")
    json_data = f.read()
    f.close()
    return jsonpickle.decode(json_data)


def get_test_play_result() -> PlayResult:
    return get_play_result(
        f'{os.getcwd()}\\training\\tests\\test_data\\tournament 2020-08-07-23-59-20\\0acf0270-1f31-4015-aa2c-0f3a52cc80fb.json')


def create_test_game_state() -> GameState:
    play_result = get_test_play_result()
    generator = GameStateGenerator(play_result)
    return generator.generate()
