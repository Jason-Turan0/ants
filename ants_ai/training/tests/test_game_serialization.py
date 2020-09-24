import os
import unittest

import jsonpickle
from ants_ai.engine.bot import BotName
from ants_ai.engine.play_result import PlayResult


class SerializationTests(unittest.TestCase):

    def test_DeserializeFromFile(self):
        data_path = f'{os.getcwd()}\\training\\tests\\test_data\\PlayResult.json'
        f = open(data_path, "r")
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
