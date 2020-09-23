import os
import unittest
from datetime import datetime
from tempfile import mkdtemp
from uuid import uuid4

from ants_ai.engine.bot import BotName
from ants_ai.engine.play_result import PlayResult
from ants_ai.training.data_gen.tournament_runner import TournamentRunner, save_play_result, generate_visualization
from ants_ai.training.tests.test_utils import get_test_play_result
from py4j.java_gateway import JavaGateway


class TestTournament(unittest.TestCase):
    def runGame(self) -> PlayResult:
        tr = TournamentRunner(JavaGateway())
        map_path = f'{os.getcwd()}\\engine\\maps\\training\\small.map'
        return tr.play_game(BotName('hippo'), BotName('lazarant'), str(uuid4()), map_path)

    @unittest.skip('Integration test')
    def test_rungame(self):
        self.assertIsNotNone(self.runGame())

    def test_savegame(self):
        pr = get_test_play_result()
        dir_path = mkdtemp()
        path = f'{dir_path}\\{pr.game_id}.json'
        save_play_result(pr, path)

    def test_generate_visualization(self):
        pr = get_test_play_result()
        dir_path = mkdtemp()
        path = f'{dir_path}\\{pr.game_id}.json'
        save_play_result(pr, path)
        print(f'Play results saved to {path}')
        generate_visualization(path, path.replace('.json', '.html'))

    # C:\Projects\ants\training_data_gen\tournaments\2020-08-01-01-58-18
    # [('memetix_1', 52), ('lazarant_1', 45), ('pkmiec_1', 38), ('hippo_1', 33), ('xathis_1', 12), ('speedyBot_1', 0)]
    @unittest.skip('Integration test')
    def test_run_tournament(self):
        tr = TournamentRunner(JavaGateway())
        map_path = f'{os.getcwd()}\\engine\\maps\\training\\small.map'
        tournament_time = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        tournament_path = f'{os.getcwd()}\\tournaments\\{tournament_time}'
        tr.run_tournament(tournament_path, map_path)

    @unittest.skip('Integration test')
    def test_generate_data(self):
        tr = TournamentRunner(JavaGateway())
        map_path = f'{os.getcwd()}\\training_data_gen\\engine\\maps\\training\\small.map'
        tournament_time = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        tournament_path = f'{os.getcwd()}\\generated_data\\{tournament_time}'
        tr.generate_game_data(tournament_path, map_path, 'memetix', 200)


if __name__ == '__main__':
    unittest.main()
