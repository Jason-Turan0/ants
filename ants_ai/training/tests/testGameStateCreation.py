import unittest
from ants_ai.training_data_gen.engine.play_result import PlayResult
from ants_ai.training.game_state.generator import GameStateGenerator
import os
import jsonpickle
from training.game_state.game_map import GameMap, TerrainType
from training.game_state.position import Position


class GameStateCreationTests(unittest.TestCase):

    def getPlayResult(self, dataPath) -> PlayResult:
        f = open(dataPath, "r")
        json_data = f.read()
        f.close()
        return jsonpickle.decode(json_data)

    def get_test_play_result (self) -> PlayResult:
        return self.getPlayResult(f'{os.getcwd()}\\training\\tests\\test_data\\tournament 2020-08-07-23-59-20\\0acf0270-1f31-4015-aa2c-0f3a52cc80fb.json')

    def test_DeserializeFromFile(self):
        play_result = self.get_test_play_result()
        self.assertEqual(159, play_result.game_length)

    @unittest.skip('WIP')
    def test_construct_game_state(self):
        play_result = self.get_test_play_result()
        generator = GameStateGenerator(play_result)
        gameState = generator.generate()
        self.assertEqual(len(gameState.gameTurns), 159)

    def test_construct_map(self):
        play_result = self.get_test_play_result()
        map = GameMap(play_result.replaydata.map);
        self.assertEqual(39, map.columnCount)
        self.assertEqual(43, map.rowCount)
        self.assertEqual(TerrainType.WATER, map.getTerrain(Position(0,0)))
        self.assertEqual(TerrainType.LAND, map.getTerrain(Position(1,5)))
        self.assertEqual(TerrainType.LAND, map.getTerrain(Position(21,18)))

if __name__ == '__main__':
    unittest.main()
