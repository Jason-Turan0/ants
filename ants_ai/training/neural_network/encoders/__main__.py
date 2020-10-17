import cProfile
import glob
import os

import jsonpickle
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.neural_network.encoders import encoders as enc
from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from functional import seq


# import matplotlib.pyplot as plt

def load_game_state(path: str, gsg: GameStateGenerator) -> GameState:
    with open(path, "r") as f:
        json_data = f.read()
        pr = jsonpickle.decode(json_data)
        return gsg.generate(pr)


def profile_encoding():
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
    pr.dump_stats('ants_example_python.profile')

    # av_features, av_labels = enc.encode_2d_examples(gst.convert_to_2d_ant_vision(bot_to_emulate, game_states), 7)


if __name__ == "__main__":
    profile_encoding()
