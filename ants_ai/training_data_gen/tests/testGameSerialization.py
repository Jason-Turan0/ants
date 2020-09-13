import unittest
from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training_data_gen.engine.bot import BotName
import os
import jsonpickle


class SerializationTests(unittest.TestCase):

    def test_DeserializeFromFile(self):
        dataPath = f'{os.getcwd()}\\training_data_gen\\tests\\test_data\\PlayResult.json'
        f = open(dataPath, "r")
        json_data = f.read()
        f.close()
        playResult: (PlayResult) = jsonpickle.decode(json_data)
        self.assertEqual(playResult.game_length, 500)

    def test_SimpleJsonPickle(self):
        b = BotName('foo', 'bar')
        frozenBot = jsonpickle.encode(b)
        thawedBot: BotName = jsonpickle.decode(frozenBot)
        self.assertEqual(b, thawedBot)


if __name__ == '__main__':
    unittest.main()
