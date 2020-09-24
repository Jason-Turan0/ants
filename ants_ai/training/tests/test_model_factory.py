import glob
import os
import unittest

from ants_ai.training.neural_network.model_factory import map_to_input, EncodingType
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.encoders import decode_2d_examples


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
        print(actual_encoded_ant_vision[0].shape)
        self.assertEqual(expected_ant_vision, decode_2d_examples(actual_encoded_ant_vision))
