import os
from typing import List, Tuple

import jsonpickle
from functional import seq
from training.game_state.game_state import GameState
from training.game_state.generator import GameStateGenerator
from training.neural_network.game_state_translator import GameStateTranslater
from training.neural_network.neural_network_example import AntVision1DExample
from training_data_gen.engine.play_result import PlayResult
import glob
import multiprocessing as mp
from enum import Enum


class ExampleType(Enum):
    ANT_VISION = 1,
    WHOLE_MAP = 2


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


def map_to_input(pr: Tuple[str, PlayResult, ExampleType]) -> List[AntVision1DExample]:
    bot_name, play_result, type = pr
    print('Parsing game ' + play_result.game_id)
    generator = GameStateGenerator()
    translator = GameStateTranslater()
    input = translator.convert_to_1d_ant_vision(bot_name, [generator.generate(play_result)]) if (
            type == ExampleType.ANT_VISION) else \
        translator.convert_to_antmap(bot_name, [generator.generate(play_result)])
    return input.examples


def create_test_play_results(game_count: int, bot_name: str) -> List[PlayResult]:
    files = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    play_results = seq([get_play_result(file) for file in files]) \
        .filter(lambda pr: bot_name in pr.playernames) \
        .take(game_count) \
        .to_list()
    print(f'Got {len(play_results)} play results!')
    return play_results


def create_test_examples(game_count: int, bot_name: str, type: ExampleType) -> List[AntVision1DExample]:
    play_results = [(bot_name, pr, type) for pr in create_test_play_results(game_count, bot_name)]
    pool = mp.Pool(mp.cpu_count() - 1)
    results = pool.map(map_to_input, play_results)
    pool.close()
    examples = [item for sublist in results for item in sublist]
    print(f'Generated {len(examples)} examples')
    return examples
