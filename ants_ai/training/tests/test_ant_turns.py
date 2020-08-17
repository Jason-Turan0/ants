import unittest
from training.game_state.ant_turn import AntTurn
from training.game_state.game_map import Position, Direction
from functional import seq
from training.tests.test_utils import get_test_play_result, create_test_game_state


class TestAntTurns(unittest.TestCase):
    def get_ant_at_pos (self,game_state, turn, position) -> AntTurn:
        return seq(game_state.game_turns[turn].ants).find(lambda a: a.position == position)

    def test_create_ants_turn0(self):
        game_state = create_test_game_state()
        turn0 = game_state.game_turns[0]
        self.assertEqual(8, len(turn0.ants))
        self.assertEqual(Position(1, 19), turn0.ants[0].position)
        self.assertEqual('lazarant', turn0.ants[0].bot.bot_type)
        self.assertEqual('pkmiec', turn0.ants[7].bot.bot_type)


    def test_create_ants_turn1(self):
        game_state = create_test_game_state()
        turn1 = game_state.game_turns[1]
        self.assertEqual(8, len(turn1.ants))
        self.assertIsNotNone(self.get_ant_at_pos(game_state, 1, Position(2, 19)))

    def test_next_direction(self):
        game_state = create_test_game_state()
        self.assertEqual(Direction.WEST, self.get_ant_at_pos(game_state, 0, Position(28,19)).next_direction)
        self.assertEqual(Direction.NORTH, self.get_ant_at_pos(game_state, 1, Position(28,18)).next_direction)
        self.assertEqual(Direction.NORTH, self.get_ant_at_pos(game_state, 2, Position(27,18)).next_direction)
        self.assertEqual(Direction.SOUTH, self.get_ant_at_pos(game_state, 12, Position(19,24)).next_direction)

    def test_create_ants_turn2(self):
        game_state = create_test_game_state()
        turn2 = game_state.game_turns[2]
        self.assertEqual(13, len(turn2.ants))
        self.assertEqual(Position(3, 19), turn2.ants[0].position)

    def test_ant_position(self):
        game_state = create_test_game_state()
        test_ant :AntTurn = self.get_ant_at_pos(game_state, 59,  Position(16,14))
        self.assertIsNotNone(test_ant)
        self.assertEqual('lazarant', test_ant.bot.bot_type)


if __name__ == '__main__':
    unittest.main()
