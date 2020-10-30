import argparse
from tempfile import mkdtemp
from uuid import uuid4

import ants_ai.training.data_gen.tournament_runner as tr
from ants_ai.engine.bot_name import BotName
from ants_ai.engine.java_bot import JavaBot
from ants_ai.neural_network_bot.NNBot import NNBot
from py4j.java_gateway import JavaGateway
import os
from datetime import datetime


# os.environ['CUDA_VISIBLE_DEVICES'] = '-1'

def play_game(save_path):
    gateway = JavaGateway()
    runner = tr.TournamentRunner(gateway)
    map_path = f'{os.getcwd()}\\ants_ai\\engine\\maps\\training\\small.map'
    model_path = f'{os.getcwd()}\\ants_ai\\neural_network_bot\\config\\conv_2d_20201019-150435\\model'
    # model_path = rf'E:\ants_ai_data\logs\fit\conv_2d_20201019-110623\conv_2d\model'
    # model_path = rf'E:\ants_ai_data\logs\fit\conv_2d_20201019-112133\model'
    for bot_name in runner.all_bots:
        game_id = str(uuid4())
        # profile = cProfile.Profile()
        # profile.enable()

        pr = runner.play_game_with_bots(JavaBot(game_id, gateway, BotName(bot_name)),
                                        NNBot(game_id, BotName('neural_network_bot'), model_path), game_id, map_path)
        # profile.disable()
        # profile.dump_stats('nn_game.profile')
        play_time = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        play_dir = f'{save_path}\\{play_time}'
        os.makedirs(play_dir)
        path = f'{play_dir}\\{pr.game_id}.json'
        tr.save_play_result(pr, path)
        print(path)
        tr.generate_visualization(path, path.replace('.json', '.html'))


def main():
    default_data_path = f'{os.getcwd()}\\ants_ai_data\\neural_network_replays'
    parser = argparse.ArgumentParser(description='Runs a game using the neural network bot')
    parser.add_argument('-sp', '--save-path', help='The folder to save the replay results to',
                        default=default_data_path)
    args = parser.parse_args()
    play_game(args.save_path)


if __name__ == "__main__":
    main()
