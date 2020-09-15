from tempfile import mkdtemp
from uuid import uuid4

import ants_ai.training_data_gen.tournament.tournament_runner as tr
from engine.bot_name import BotName
from engine.java_bot import JavaBot
from neural_network_bot.NNBot import NNBot
from py4j.java_gateway import JavaGateway
from datetime import datetime
import os

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'


def main(args=None):
    gateway = JavaGateway()
    runner = tr.TournamentRunner(gateway)
    map_path = f'{os.getcwd()}\\ants_ai\\engine\\maps\\training\\small.map'
    weight_path = f'{os.getcwd()}\\ants_ai\\neural_network_bot\\bot_weights\\conv_2d_weights_741e221d-0fa7-4a29-9884-390f771a3007_09'
    game_id = str(uuid4())
    pr = runner.play_game_with_bots(JavaBot(game_id, gateway, BotName('speedyBot')),
                                    NNBot(game_id, BotName('neural_network_bot'), weight_path), game_id, map_path)
    dir_path = mkdtemp()
    path = f'{dir_path}\\{pr.game_id}.json'
    tr.save_play_result(pr, path)
    print(path)
    tr.generate_visualization(path, path.replace('.json', '.html'))


if __name__ == "__main__":
    main()
