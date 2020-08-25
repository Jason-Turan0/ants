import unittest
from ants_ai.training.neural_network.game_state_translator import GameStateTranslater, PositionState
from training.game_state.game_map import Position
from training.tests.test_utils import create_test_game_state
import tensorflow as tf
class TestGameStateTranslator(unittest.TestCase):

    def test_create_nn_input(self):
        game_state = create_test_game_state()
        test_ant = game_state.game_turns[0].ants.get(Position(1,19))
        ant_vision = game_state.game_map.get_positions_within_distance(test_ant.position,
                                                                       game_state.view_radius_squared)
        translator = GameStateTranslater()

        nn_input = translator.convert_to_example(test_ant, game_state)
        self.assertIsNotNone(nn_input)
        self.assertEqual(7 * (len(ant_vision)-1), len(nn_input.nn_input))

    def test_convert_enums_to_array(self):
        translator = GameStateTranslater()
        array = translator.convert_enum_to_array(PositionState.LAND, PositionState)
        self.assertEqual([False, False, False, False, False, False, True], array)
        self.assertEqual(PositionState.LAND, translator.convert_array_to_enum(array, PositionState))
        self.assertIsNone(
            translator.convert_array_to_enum([False, False, False, False, False, False, False], PositionState))

    def test_create_nn_input2(self):
        game_state = create_test_game_state()
        translator = GameStateTranslater()
        blah = translator.convert_to_nn_input('pkmiec_1', [game_state])
        print(blah.train)
        print(blah.test)

if __name__ == '__main__':
    unittest.main()
