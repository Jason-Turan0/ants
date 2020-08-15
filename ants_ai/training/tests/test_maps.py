import os
import unittest
from functional import seq
from training.game_state.game_map import TerrainType, Position, Direction, GameMap
from training.tests.test_utils import get_test_play_result
from training_data_gen.engine.map_data import MapData


class TestMaps(unittest.TestCase):
    def get_map(self, path) -> GameMap:
        with open(path, 'r') as f:
            data = f.readlines()
            rows = int(seq(data).find(lambda line: line.startswith('rows')).replace('rows ', ''))
            cols = int(seq(data).find(lambda line: line.startswith('cols')).replace('cols ', ''))
            map_data = seq(data) \
                .filter(lambda line: line.startswith('m')) \
                .map(lambda line: line.replace('m ', '')) \
                .to_list()
            return GameMap(MapData(cols, map_data, rows))

    def get_test_map(self) -> GameMap:
        return self.get_map(f'{os.getcwd()}\\training_data_gen\\engine\\maps\\training\\forage_0.map')

    def test_construct_map(self):
        play_result = get_test_play_result()
        map = GameMap(play_result.replaydata.map)
        self.assertEqual(39, map.columnCount)
        self.assertEqual(43, map.rowCount)
        self.assertEqual(TerrainType.WATER, map.getTerrain(Position(0, 0)))
        self.assertEqual(TerrainType.LAND, map.getTerrain(Position(1, 5)))
        self.assertEqual(TerrainType.LAND, map.getTerrain(Position(21, 18)))

    def test_calculate_adjacent_position_south(self):
        map = self.get_test_map()
        self.assertEqual(TerrainType.LAND, map.getTerrain(Position(1, 5)))
        self.assertEqual(Position(2, 5), map.adjacent_movement_position(Position(1, 5), Direction.SOUTH))

    def test_calculate_adjacent_position_north(self):
        map = self.get_test_map()
        self.assertEqual(Position(0, 5), map.adjacent_movement_position(Position(1, 5), Direction.NORTH))

    def test_calculate_adjacent_position_east(self):
        map = self.get_test_map()
        self.assertEqual(Position(1, 6), map.adjacent_movement_position(Position(1, 5), Direction.EAST))

    def test_calculate_adjacent_position_west(self):
        map = self.get_test_map()
        self.assertEqual(Position(1, 4), map.adjacent_movement_position(Position(1, 5), Direction.WEST))

    def test_calculate_adjacent_position_south_wrap(self):
        map = self.get_test_map()
        self.assertEqual(Position(0, 5), map.adjacent_movement_position(Position(79, 5), Direction.SOUTH))

    def test_calculate_adjacent_position_north_wrap(self):
        map = self.get_test_map()
        self.assertEqual(Position(79, 5), map.adjacent_movement_position(Position(0, 5), Direction.NORTH))

    def test_calculate_adjacent_position_east_wrap(self):
        map = self.get_test_map()
        self.assertEqual(Position(1, 0), map.adjacent_movement_position(Position(1, 79), Direction.EAST))

    def test_calculate_adjacent_position_west_wrap(self):
        map = self.get_test_map()
        self.assertEqual(Position(1, 79), map.adjacent_movement_position(Position(1, 0), Direction.WEST))
