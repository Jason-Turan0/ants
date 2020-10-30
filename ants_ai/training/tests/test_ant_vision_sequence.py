import unittest
import glob
import os
import re
from math import ceil, floor
from pprint import pprint
from functional import seq
from ants_ai.training.neural_network.sequences.ant_vision_sequence import AntVisionSequence
from ants_ai.training.neural_network.sequences.hybrid_sequence import HybridSequence


class TestAntVisionSequence(unittest.TestCase):
    def setUp(self) -> None:
        self.data_file_names = ['0cbdcc89-692b-46e6-a567-5014942151ed', '0acf0270-1f31-4015-aa2c-0f3a52cc80fb']
        self.data_folder = os.path.abspath('./training/tests/test_data')
        self.data_paths = [os.path.join(self.data_folder, data_file_name + '.json') for data_file_name in self.data_file_names]
        self.expected_example_counts = [1072, 1311]
        self.batch_size = 50
        self.bot_to_emulate = 'pkmiec_1'
        for data_file_name in self.data_file_names:
            cached_file_paths = [f for f in glob.glob(f'{self.data_folder}\\{data_file_name}*')]
            for path in cached_file_paths:
                if re.match('.*\\.txt$|.*\\.npy$', path):
                    print(f'Removing {path}!')
                    os.remove(path)

    def test_batch_length(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)
        self.assertEqual(ceil((sum(self.expected_example_counts) * .6) / self.batch_size), len(s))

    def test_hybrid_sequence(self):
        s = HybridSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)

        for gi in s.game_indexes:
            print(f'{gi.position_start} {gi.position_end} {gi.length}')

        print('Training')
        print(s.get_training_range())
        print(s.get_training_batch_count())
        training_count = seq(range(s.get_training_batch_count())) \
            .map(lambda batch_index: s.get_training_batch(batch_index)[0][0].shape[0]) \
            .sum()
        self.assertEqual(1429, training_count)

        print('Test')
        pprint(s.get_test_range())
        print(s.get_test_batch_count())
        test_count = seq(range(s.get_test_batch_count())) \
            .map(lambda batch_index: s.get_test_batch(batch_index)[0][0].shape[0]) \
            .sum()
        self.assertEqual(477, test_count)

        print('Cross_val')
        pprint(s.get_cross_val_range())
        print(s.get_cross_val_batch_count())
        crossval_count = seq(range(s.get_cross_val_batch_count())) \
            .map(lambda batch_index: s.get_cross_val_batch(batch_index)[0][0].shape[0]) \
            .sum()
        self.assertEqual(477, crossval_count)

    def test_set_sizes(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)

        for gi in s.game_indexes:
            print(f'{gi.position_start} {gi.position_end} {gi.length}')

        print('Training')
        print(s.get_training_range())
        print(s.get_training_batch_count())
        training_count = seq(range(s.get_training_batch_count())) \
            .map(lambda batch_index: s.get_training_batch(batch_index)[0].shape[0]) \
            .sum()
        self.assertEqual(1429, training_count)

        print('Test')
        pprint(s.get_test_range())
        print(s.get_test_batch_count())
        test_count = seq(range(s.get_test_batch_count())) \
            .map(lambda batch_index: s.get_test_batch(batch_index)[0].shape[0]) \
            .sum()
        self.assertEqual(477, test_count)

        print('Cross_val')
        pprint(s.get_cross_val_range())
        print(s.get_cross_val_batch_count())
        crossval_count = seq(range(s.get_cross_val_batch_count())) \
            .map(lambda batch_index: s.get_cross_val_batch(batch_index)[0].shape[0]) \
            .sum()
        self.assertEqual(477, crossval_count)

    def test_create_index(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)
        gi_0 = seq(s.game_indexes).find(lambda gi: gi.game_path == self.data_paths[0])
        gi_1 = seq(s.game_indexes).find(lambda gi: gi.game_path == self.data_paths[1])

        self.assertEqual(gi_0.length, self.expected_example_counts[0])
        self.assertEqual(gi_1.length, self.expected_example_counts[1])

    def test_get_batch(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)
        blah = s[0]
        self.assertEqual((50, 12, 12, 7), blah[0].shape)

    def test_get_last_batch(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)
        last_batch = s[len(s) - 1]
        self.assertEqual((29, 12, 12, 7), last_batch[0].shape)

    def test_range_intersects(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        self.assertTrue(s.ranges_intersect(0, 6, 5, 10))
        self.assertTrue(s.ranges_intersect(0, 6, 4, 10))
        self.assertFalse(s.ranges_intersect(0, 5, 5, 10))
        self.assertFalse(s.ranges_intersect(10, 20, 5, 10))
        self.assertFalse(s.ranges_intersect(0, 0, 0, 0))

    def test_get_batch_across_index(self):
        s = AntVisionSequence(self.data_paths, self.batch_size, self.bot_to_emulate)
        s.build_indexes(True)
        last_batch_index = s[floor(s.game_indexes[0].length % self.batch_size)]
        self.assertEqual((50, 12, 12, 7), last_batch_index[0].shape)
