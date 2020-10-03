import glob
import os
import unittest

from ants_ai.training.neural_network.factories.model_factory import map_to_input, EncodingType
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.encoders.encoders import decode_ant_vision_2d_examples, decode_map_examples
 

class TestModelFactory(unittest.TestCase):

    def setUp(self) -> None:
        self.data_file_name = '0acf0270-1f31-4015-aa2c-0f3a52cc80fb'
        self.data_folder = f'{os.getcwd()}\\training\\tests\\test_data\\tournament 2020-08-07-23-59-20'
        self.data_path = f'{self.data_folder}\\{self.data_file_name}.json'

        cached_file_paths = [f for f in glob.glob(f'{self.data_folder}\\{self.data_file_name}*.npy')]
        for path in cached_file_paths:
            os.remove(path)

    def test_encode_2d_ant_vision(self):
        bot_to_emulate = 'pkmiec_1'

        gsg = GameStateGenerator()
        gst = GameStateTranslator()
        test_game_state = gsg.generate_from_file(self.data_path)
        expected_ant_vision = gst.convert_to_2d_ant_vision(bot_to_emulate, [test_game_state])

        actual_encoded_ant_vision = map_to_input((bot_to_emulate, EncodingType.ANT_VISION_2D, self.data_path))
        actual_decoded_ant_vision = decode_ant_vision_2d_examples(actual_encoded_ant_vision)

        for index, expected in enumerate(expected_ant_vision):
            self.assertEqual(expected.label, actual_decoded_ant_vision[index].label)
            for expected_pos in expected.features.keys():
                self.assertEqual(expected.features[expected_pos],
                                 actual_decoded_ant_vision[index].features[expected_pos])

    def test_encode_2d_map(self):
        bot_to_emulate = 'pkmiec_1'

        gsg = GameStateGenerator()
        gst = GameStateTranslator()
        test_game_state = gsg.generate_from_file(self.data_path)
        expected_map = gst.convert_to_antmap(bot_to_emulate, [test_game_state])

        actual_encoded_map = map_to_input((bot_to_emulate, EncodingType.MAP_2D, self.data_path))
        actual_decoded_map = decode_map_examples(actual_encoded_map)

        for index, expected in enumerate(expected_map):
            self.assertEqual(expected.label, actual_decoded_map[index].label)
            for expected_pos in expected.features.keys():
                self.assertEqual(expected.features[expected_pos],
                                 actual_decoded_map[index].features[expected_pos])
