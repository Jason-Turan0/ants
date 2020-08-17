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
        self.assertEqual(39, map.column_count)
        self.assertEqual(43, map.row_count)
        self.assertEqual(TerrainType.WATER, map.get_terrain(Position(0, 0)))
        self.assertEqual(TerrainType.LAND, map.get_terrain(Position(1, 5)))
        self.assertEqual(TerrainType.LAND, map.get_terrain(Position(21, 18)))

    def test_calculate_adjacent_position_south(self):
        map = self.get_test_map()
        self.assertEqual(TerrainType.LAND, map.get_terrain(Position(1, 5)))
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

    def test_calculate_vision(self):
        map = self.get_test_map()
        expected = [Position(row=8, column=26), Position(row=8, column=27), Position(row=8, column=28), Position(row=8, column=29), Position(row=8, column=30), Position(row=8, column=31), Position(row=8, column=32), Position(row=9, column=24), Position(row=9, column=25), Position(row=9, column=26), Position(row=9, column=27), Position(row=9, column=28), Position(row=9, column=29), Position(row=9, column=30), Position(row=9, column=31), Position(row=9, column=32), Position(row=9, column=33), Position(row=9, column=34), Position(row=10, column=23), Position(row=10, column=24), Position(row=10, column=25), Position(row=10, column=26), Position(row=10, column=27), Position(row=10, column=28), Position(row=10, column=29), Position(row=10, column=30), Position(row=10, column=31), Position(row=10, column=32), Position(row=10, column=33), Position(row=10, column=34), Position(row=10, column=35), Position(row=11, column=22), Position(row=11, column=23), Position(row=11, column=24), Position(row=11, column=25), Position(row=11, column=26), Position(row=11, column=27), Position(row=11, column=28), Position(row=11, column=29), Position(row=11, column=30), Position(row=11, column=31), Position(row=11, column=32), Position(row=11, column=33), Position(row=11, column=34), Position(row=11, column=35), Position(row=11, column=36), Position(row=12, column=22), Position(row=12, column=23), Position(row=12, column=24), Position(row=12, column=25), Position(row=12, column=26), Position(row=12, column=27), Position(row=12, column=28), Position(row=12, column=29), Position(row=12, column=30), Position(row=12, column=31), Position(row=12, column=32), Position(row=12, column=33), Position(row=12, column=34), Position(row=12, column=35), Position(row=12, column=36), Position(row=13, column=21), Position(row=13, column=22), Position(row=13, column=23), Position(row=13, column=24), Position(row=13, column=25), Position(row=13, column=26), Position(row=13, column=27), Position(row=13, column=28), Position(row=13, column=29), Position(row=13, column=30), Position(row=13, column=31), Position(row=13, column=32), Position(row=13, column=33), Position(row=13, column=34), Position(row=13, column=35), Position(row=13, column=36), Position(row=13, column=37), Position(row=14, column=21), Position(row=14, column=22), Position(row=14, column=23), Position(row=14, column=24), Position(row=14, column=25), Position(row=14, column=26), Position(row=14, column=27), Position(row=14, column=28), Position(row=14, column=29), Position(row=14, column=30), Position(row=14, column=31), Position(row=14, column=32), Position(row=14, column=33), Position(row=14, column=34), Position(row=14, column=35), Position(row=14, column=36), Position(row=14, column=37), Position(row=15, column=21), Position(row=15, column=22), Position(row=15, column=23), Position(row=15, column=24), Position(row=15, column=25), Position(row=15, column=26), Position(row=15, column=27), Position(row=15, column=28), Position(row=15, column=29), Position(row=15, column=30), Position(row=15, column=31), Position(row=15, column=32), Position(row=15, column=33), Position(row=15, column=34), Position(row=15, column=35), Position(row=15, column=36), Position(row=15, column=37), Position(row=16, column=21), Position(row=16, column=22), Position(row=16, column=23), Position(row=16, column=24), Position(row=16, column=25), Position(row=16, column=26), Position(row=16, column=27), Position(row=16, column=28), Position(row=16, column=29), Position(row=16, column=30), Position(row=16, column=31), Position(row=16, column=32), Position(row=16, column=33), Position(row=16, column=34), Position(row=16, column=35), Position(row=16, column=36), Position(row=16, column=37), Position(row=17, column=21), Position(row=17, column=22), Position(row=17, column=23), Position(row=17, column=24), Position(row=17, column=25), Position(row=17, column=26), Position(row=17, column=27), Position(row=17, column=28), Position(row=17, column=29), Position(row=17, column=30), Position(row=17, column=31), Position(row=17, column=32), Position(row=17, column=33), Position(row=17, column=34), Position(row=17, column=35), Position(row=17, column=36), Position(row=17, column=37), Position(row=18, column=21), Position(row=18, column=22), Position(row=18, column=23), Position(row=18, column=24), Position(row=18, column=25), Position(row=18, column=26), Position(row=18, column=27), Position(row=18, column=28), Position(row=18, column=29), Position(row=18, column=30), Position(row=18, column=31), Position(row=18, column=32), Position(row=18, column=33), Position(row=18, column=34), Position(row=18, column=35), Position(row=18, column=36), Position(row=18, column=37), Position(row=19, column=21), Position(row=19, column=22), Position(row=19, column=23), Position(row=19, column=24), Position(row=19, column=25), Position(row=19, column=26), Position(row=19, column=27), Position(row=19, column=28), Position(row=19, column=29), Position(row=19, column=30), Position(row=19, column=31), Position(row=19, column=32), Position(row=19, column=33), Position(row=19, column=34), Position(row=19, column=35), Position(row=19, column=36), Position(row=19, column=37), Position(row=20, column=22), Position(row=20, column=23), Position(row=20, column=24), Position(row=20, column=25), Position(row=20, column=26), Position(row=20, column=27), Position(row=20, column=28), Position(row=20, column=29), Position(row=20, column=30), Position(row=20, column=31), Position(row=20, column=32), Position(row=20, column=33), Position(row=20, column=34), Position(row=20, column=35), Position(row=20, column=36), Position(row=21, column=22), Position(row=21, column=23), Position(row=21, column=24), Position(row=21, column=25), Position(row=21, column=26), Position(row=21, column=27), Position(row=21, column=28), Position(row=21, column=29), Position(row=21, column=30), Position(row=21, column=31), Position(row=21, column=32), Position(row=21, column=33), Position(row=21, column=34), Position(row=21, column=35), Position(row=21, column=36), Position(row=22, column=23), Position(row=22, column=24), Position(row=22, column=25), Position(row=22, column=26), Position(row=22, column=27), Position(row=22, column=28), Position(row=22, column=29), Position(row=22, column=30), Position(row=22, column=31), Position(row=22, column=32), Position(row=22, column=33), Position(row=22, column=34), Position(row=22, column=35), Position(row=23, column=24), Position(row=23, column=25), Position(row=23, column=26), Position(row=23, column=27), Position(row=23, column=28), Position(row=23, column=29), Position(row=23, column=30), Position(row=23, column=31), Position(row=23, column=32), Position(row=23, column=33), Position(row=23, column=34), Position(row=24, column=26), Position(row=24, column=27), Position(row=24, column=28), Position(row=24, column=29), Position(row=24, column=30), Position(row=24, column=31), Position(row=24, column=32)]
        within_dist = map.get_positions_within_distance(Position(16, 29), 77)
        self.assertIsNotNone(within_dist)
        for pe in expected:
            seq(within_dist).find(lambda pa: pa == pe)


