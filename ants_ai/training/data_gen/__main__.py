from ants_ai.training.data_gen.tournament_runner import TournamentRunner
from py4j.java_gateway import JavaGateway

import os
import argparse


def main(save_path: str, map_path: str, type: str, bot_name: str, game_count: int):
    gateway = JavaGateway()
    gr = TournamentRunner(gateway)
    if type.lower() == 'all':
        gr.run_tournament(save_path, map_path, game_count)
    elif type.lower() == 'single':
        gr.generate_game_data(save_path, map_path, bot_name, game_count)
    else:
        raise ValueError(f'Unknown generation type {type}')


if __name__ == "__main__":
    map_path = f'{os.getcwd()}\\ants_ai\\engine\\maps\\training\\small.map'
    save_path = f'{os.getcwd()}\\ants_ai_data\\training'
    parser = argparse.ArgumentParser(description='Runs games and saves the results to the file system')
    parser.add_argument('-mp', '--map-path', help='The map to use for the data generation', default=map_path)
    parser.add_argument('-sp', '--save-path', help='The directory to save the play results to', default=save_path)
    parser.add_argument('-t', '--type', help='The type of data generation. Options are All or Single', default='Single')
    parser.add_argument('-b', '--bot', help='The name of the bot to run games for if single', default='memetix')
    parser.add_argument('-gc', '--game-count', help='The number of games to run', type=int, default=100)
    args = parser.parse_args()
    main(args.save_path, args.map_path, args.type, args.bot, args.game_count)
