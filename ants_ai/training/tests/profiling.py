import cProfile
from functional import seq
from training.tests.test_utils import create_test_game_state
from ants_ai.training.neural_network.game_state_translator import GameStateTranslater
game_state = create_test_game_state()
translator = GameStateTranslater()
def convert_game_state():
    ant_turns = seq(game_state.game_turns) \
        .flat_map(lambda gt : gt.ants.values()) \
        .filter(lambda at: at.bot.bot_name == game_state.winning_bot.bot_name)
    pr = cProfile.Profile()
    pr.enable()
    training_data = ant_turns\
        .map(lambda at: translator.convert_to_example(at, game_state)) \
        .to_list()
    pr.disable()
    pr.print_stats(sort='cumtime')

print('RAW')
print('=' * 80)
convert_game_state()