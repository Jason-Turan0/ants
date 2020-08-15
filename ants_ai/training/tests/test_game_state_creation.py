import unittest

from training.game_state.game_map import Position
from training.tests.test_utils import get_test_play_result, create_test_game_state


class TestGameStateCreation(unittest.TestCase):
    def test_DeserializeFromFile(self):
        self.assertEqual(159, get_test_play_result().game_length)

    def test_hills(self):
        game_state = create_test_game_state()
        self.assertEqual(8, len(game_state.game_turns[0].hills))
        self.assertEqual(Position(1,19), game_state.game_turns[0].hills[0].position)
        self.assertTrue(game_state.game_turns[151].hills[0].is_alive)
        self.assertFalse(game_state.game_turns[152].hills[0].is_alive)

    def test_foods(self):
        game_state = create_test_game_state()
        self.assertEqual(28, len(game_state.game_turns[0].foods))
        self.assertEqual(5, len(game_state.game_turns[101].foods))
        self.assertEqual(Position(7,5), game_state.game_turns[0].foods[0].position)

if __name__ == '__main__':
    unittest.main()
