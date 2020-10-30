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


def play_game(save_path: str, model_path: str):
    gateway = JavaGateway()
    runner = tr.TournamentRunner(gateway)
    map_path = os.path.abspath('./ants_ai/engine/maps/training/small.map')
    for bot_name in runner.all_bots:
        game_id = str(uuid4())

        pr = runner.play_game_with_bots(JavaBot(game_id, gateway, BotName(bot_name)),
                                        NNBot(game_id, BotName('neural_network_bot'), model_path), game_id, map_path)
        play_time = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
        play_dir = f'{save_path}\\{play_time}'
        os.makedirs(play_dir)
        path = f'{play_dir}\\{pr.game_id}.json'
        tr.save_play_result(pr, path)
        tr.generate_visualization(path, path.replace('.json', '.html'))


def main():
    default_save_path = os.path.abspath('./../ants_ai_data/neural_network_replays')

    default_model_path = os.path.abspath('./neural_network_bot/config/conv_2d_20201019-150435/model')
    parser = argparse.ArgumentParser(description='Runs a game using the neural network bot')
    parser.add_argument('-sp', '--save-path', help='The folder to save the replay results to',
                        default=default_save_path)
    parser.add_argument('-mp', '--model-path', help='The path to the model weights',
                        default=default_model_path)
    args = parser.parse_args()
    play_game(args.save_path, args.model_path)


if __name__ == "__main__":
    main()
