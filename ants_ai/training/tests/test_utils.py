import glob
import multiprocessing as mp
import os
from typing import List

import jsonpickle
from ants_ai.engine.play_result import PlayResult
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.game_state.generator import GameStateGenerator
from functional import seq


def get_play_result(dataPath) -> PlayResult:
    with open(dataPath, "r") as f:
        json_data = f.read()
        return jsonpickle.decode(json_data)


def get_test_play_result() -> PlayResult:
    return get_play_result(os.path.abspath('./training/tests/test_data/0acf0270-1f31-4015-aa2c-0f3a52cc80fb.json'))

def create_test_game_state() -> GameState:
    play_result = get_test_play_result()
    generator = GameStateGenerator()
    return generator.generate(play_result)


def map_to_game_state(pr: PlayResult):
    gsg = GameStateGenerator()
    print('Parsing game ' + pr.game_id)
    return gsg.generate(pr)
