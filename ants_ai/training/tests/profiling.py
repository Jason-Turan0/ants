import cProfile
from functional import seq
from training.tests.test_utils import create_test_game_state
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator

game_state = create_test_game_state()
translator = GameStateTranslator()


def convert_game_state():
    pr = cProfile.Profile()
    pr.enable()
    training_data = translator.convert_to_1d_ant_vision('pkmiec_1', [game_state])
    pr.disable()
    print(len(training_data))
    # pr.print_stats(sort='cumtime')
    pr.dump_stats('ants_example.profile')


# print('RAW')
# print('=' * 80)
convert_game_state()
