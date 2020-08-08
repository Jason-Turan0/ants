import unittest
from ants_ai.training_data_gen.engine.play_result import PlayResult
import os
import jsonpickle


class GameStateCreationTests(unittest.TestCase):

    def test_DeserializeFromFile(self):
        dataPath = f'{os.getcwd()}\\training\\tests\\test_data\\PlayResult.json'
        f = open(dataPath, "r")
        json_data = f.read()
        f.close()
        playResult: (PlayResult) = jsonpickle.decode(json_data)
        self.assertEqual(playResult.game_length, 500)


if __name__ == '__main__':
    unittest.main()
