from tempfile import mkdtemp
from uuid import uuid4

import ants_ai.training.data_gen.tournament_runner as tr
from ants_ai.engine.bot_name import BotName
from ants_ai.engine.java_bot import JavaBot
from ants_ai.neural_network_bot.NNBot import NNBot
from py4j.java_gateway import JavaGateway
import os
import cProfile

os.environ['CUDA_VISIBLE_DEVICES'] = '-1'


def main():
    gateway = JavaGateway()
    runner = tr.TournamentRunner(gateway)
    map_path = f'{os.getcwd()}\\ants_ai\\engine\\maps\\training\\small.map'
    model_path = f'{os.getcwd()}\\ants_ai\\neural_network_bot\\config\\conv_2d_20200913-114036'
    game_id = str(uuid4())
    profile = cProfile.Profile()
    profile.enable()
    pr = runner.play_game_with_bots(JavaBot(game_id, gateway, BotName('memetix')),
                                    NNBot(game_id, BotName('neural_network_bot'), model_path), game_id, map_path)
    profile.disable()
    profile.dump_stats('nn_game.profile')
    dir_path = mkdtemp()
    path = f'{dir_path}\\{pr.game_id}.json'
    tr.save_play_result(pr, path)
    print(path)
    tr.generate_visualization(path, path.replace('.json', '.html'))


if __name__ == "__main__":
    main()
