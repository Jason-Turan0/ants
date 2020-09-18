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
    f = open(dataPath, "r")
    json_data = f.read()
    f.close()
    return jsonpickle.decode(json_data)


def get_test_play_result() -> PlayResult:
    return get_play_result(
        f'{os.getcwd()}\\training\\tests\\test_data\\tournament 2020-08-07-23-59-20\\0acf0270-1f31-4015-aa2c-0f3a52cc80fb.json')


def create_test_game_state() -> GameState:
    play_result = get_test_play_result()
    generator = GameStateGenerator()
    return generator.generate(play_result)


def map_to_game_state(pr: PlayResult):
    gsg = GameStateGenerator()
    print('Parsing game ' + pr.game_id)
    return gsg.generate(pr)


def create_test_play_results(game_count: int, bot_name: str) -> List[PlayResult]:
    files = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    play_results = seq([get_play_result(file) for file in files]) \
        .filter(lambda pr: bot_name in pr.playernames) \
        .take(game_count) \
        .to_list()
    print(f'Got {len(play_results)} play results!')
    return play_results


def create_test_game_states(game_count: int, bot_name: str) -> List[GameState]:
    play_results = create_test_play_results(game_count, bot_name)
    pool = mp.Pool(mp.cpu_count() - 1)
    game_states = pool.map(map_to_game_state, play_results)
    pool.close()
    return game_states
