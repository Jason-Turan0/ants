from ants_ai.training_data_gen.engine.play_result import PlayResult
import unittest
from ants_ai.training_data_gen.tournament.tournament_runner import TournamentRunner, save_play_result, \
    generate_visualization
from ants_ai.training_data_gen.engine.bot import Bot
from tempfile import mkdtemp
from py4j.java_gateway import JavaGateway
import os
from datetime import datetime


class TestTournament(unittest.TestCase):
    def runGame(self) -> PlayResult:
        tr = TournamentRunner(JavaGateway())
        mapPath = f'{os.getcwd()}\\training_data_gen\\engine\\maps\\training\\small.map'
        return tr.play_game(Bot('hippo'), Bot('lazarant'), mapPath)

    @unittest.skip('Integration test')
    def test_rungame(self):
        self.assertIsNotNone(self.runGame())

    @unittest.skip('Integration test')
    def test_savegame(self):
        pr = self.runGame()
        dir_path = mkdtemp()
        path = f'{dir_path}\\{pr.game_id}.json'
        save_play_result(pr, path)

    @unittest.skip('Integration test')
    def test_generate_visualization(self):
        pr = self.runGame()
        dir_path = mkdtemp()
        path = f'{dir_path}\\{pr.game_id}.json'
        save_play_result(pr, path)
        print(path)
        generate_visualization(path, path.replace('.json', '.html'))

    # C:\Projects\ants\training_data_gen\tournaments\2020-08-01-01-58-18
    # [('memetix_1', 52), ('lazarant_1', 45), ('pkmiec_1', 38), ('hippo_1', 33), ('xathis_1', 12), ('speedyBot_1', 0)]
    @unittest.skip('Integration test')
    def test_run_tournament(self):
        tr = TournamentRunner(JavaGateway())
        mapPath = f'{os.getcwd()}\\engine\\maps\\training\\small.map'
        tournamentTime = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        tournamentPath = f'{os.getcwd()}\\tournaments\\{tournamentTime}'
        tr.run_tournament(tournamentPath, mapPath)

    def test_generate_data(self):
        tr = TournamentRunner(JavaGateway())
        mapPath = f'{os.getcwd()}\\training_data_gen\\engine\\maps\\training\\small.map'
        tournamentTime = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        tournamentPath = f'{os.getcwd()}\\generated_data\\{tournamentTime}'
        tr.generate_game_data(tournamentPath, mapPath, 'memetix', 100)


if __name__ == '__main__':
    unittest.main()
