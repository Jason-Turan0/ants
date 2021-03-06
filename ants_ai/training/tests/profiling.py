import cProfile
import glob

from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from ants_ai.training.tests.test_utils import create_test_game_state
from ants_ai.training.game_state.generator import GameStateGenerator
from functional import seq
import jsonpickle
import os
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.encoders import encoders as enc

game_state = create_test_game_state()
translator = GameStateTranslator()


def load_game_state(path: str, gsg: GameStateGenerator) -> GameState:
    with open(path, "r") as f:
        json_data = f.read()
        pr = jsonpickle.decode(json_data)
        return gsg.generate(pr)


def convert_game_state():
    bot_to_emulate = 'memetix_1'
    game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
    test_games = game_paths[2:3]
    gst = GameStateTranslator()
    gsg = GameStateGenerator()

    pr = cProfile.Profile()
    pr.enable()
    game_states = seq(test_games).map(lambda path: load_game_state(path, gsg)).to_list()
    mv_features, mv_labels = enc.encode_map_examples(gst.convert_to_global_antmap(bot_to_emulate, game_states), 7)
    pr.disable()
    # pr.print_stats(sort='cumtime')
    pr.dump_stats('ants_example.profile')


# print('RAW')
# print('=' * 80)
convert_game_state()
