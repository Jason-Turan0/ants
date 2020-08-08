import unittest
from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training.game_state.generator import GameStateGenerator
import os
import jsonpickle


class GameStateCreationTests(unittest.TestCase):

    def getPlayResult(self, dataPath) -> PlayResult:
        f = open(dataPath, "r")
        json_data = f.read()
        f.close()
        return jsonpickle.decode(json_data)

    def test_DeserializeFromFile(self):
        playResult = self.getPlayResult(f'{os.getcwd()}\\training\\tests\\test_data\\PlayResult.json')
        self.assertEqual(playResult.game_length, 500)

    def test_shouldBeAbleToConstructGameState(self):
        playResult = self.getPlayResult(f'{os.getcwd()}\\training\\tests\\test_data\\tournament 2020-08-07-23-59-20\\0acf0270-1f31-4015-aa2c-0f3a52cc80fb.json')
        generator = GameStateGenerator(playResult)
        gameState = generator.generate()
        self.assertEqual(len(gameState.gameTurns), 159)



if __name__ == '__main__':
    unittest.main()
