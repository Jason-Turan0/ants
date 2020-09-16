import unittest
from pprint import pprint

from ants_ai.training.neural_network.game_state_translator import GameStateTranslator, PositionState
from ants_ai.training.game_state.game_map import Position
from training.tests.test_utils import create_test_game_state
import tensorflow as tf


class TestGameStateTranslator(unittest.TestCase):

    def test_create_nn_input(self):
        game_state = create_test_game_state()
        test_ant = game_state.game_turns[0].ants.get(Position(1, 19))
        ant_vision = game_state.game_map.get_positions_within_distance(test_ant.position,
                                                                       game_state.view_radius_squared)
        translator = GameStateTranslator()

        nn_input = translator.convert_to_1d_example(test_ant, game_state)
        self.assertIsNotNone(nn_input)
        self.assertEqual((len(ant_vision) - 1), len(nn_input.features))

    def test_convert_enums_to_array(self):
        translator = GameStateTranslator()
        array = translator.convert_enum_to_array(PositionState.LAND, PositionState)
        self.assertEqual([0, 0, 0, 0, 0, 0, 1], array)
        self.assertEqual(PositionState.LAND, translator.convert_array_to_enum(array, PositionState))
        self.assertIsNone(
            translator.convert_array_to_enum([0, 0, 0, 0, 0, 0, 0], PositionState))

    def test_create_1d_ant_vision(self):
        game_state = create_test_game_state()
        translator = GameStateTranslator()
        translated = translator.convert_to_1d_ant_vision('pkmiec_1', [game_state])
        self.assertIsNotNone(translated)

    def test_create_2d_ant_vision(self):
        game_state = create_test_game_state()
        translator = GameStateTranslator()
        translated = translator.convert_to_2d_ant_vision('pkmiec_1', [game_state])
        self.assertIsNotNone(translated)
        pprint(translated[0].features)
        print(translated[0].features.keys())


if __name__ == '__main__':
    unittest.main()
