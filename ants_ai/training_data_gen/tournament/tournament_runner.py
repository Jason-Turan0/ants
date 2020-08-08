import json
import os
import sys
from io import StringIO
from itertools import combinations
from uuid import uuid4

import jsonpickle
from functional import seq

import ants_ai.training_data_gen.engine.visualizer.visualize_locally
from ants_ai.training_data_gen.engine import visualizer
from ants_ai.training_data_gen.engine.ants import Ants
from ants_ai.training_data_gen.engine.bot import Bot
from ants_ai.training_data_gen.engine.engine import run_game
from ants_ai.training_data_gen.engine.play_result import PlayResult
from typing import List
import os

class TournamentRunner:
    def __init__(self, gateway):
        self.gateway = gateway

    def play_game(self, bot0: Bot, bot1: Bot, mapPath: str) -> PlayResult:
        turns = 500
        loadtime = 3000
        turntime = 1000
        game_id = str(uuid4())
        game_options = {
            "attack": "focus",
            "kill_points": 2,
            "food": "symmetric",
            "viewradius2": 77,
            "attackradius2": 5,
            "spawnradius2": 1,
            "loadtime": loadtime,
            "turntime": turntime,
            "turns": turns,
            "food_rate": (5, 11),
            "food_turn": (19, 37),
            "food_start": (75, 175),
            "food_visible": (3, 5),
            "cutoff_turn": 150,
            "cutoff_percent": 1,
            "scenario": "store_true",
            "game_id": game_id
        }
        # replay_path = f'{tournamentDirectory}\\{game_id}.json'
        # html_path = replay_path.replace(".json", ".html")
        engine_options = {
            "loadtime": loadtime,
            "turntime": turntime,
            "map_file": mapPath,
            "turns": turns,
            "verbose_log": sys.stdout,
            # "replay_log":,
            "log_dir": "game_log",
            "log_stream": False,
            "log_input": False,
            "log_output": False,
            "log_error": False,
            "serial": False,
            "strict": False,
            "capture_errors": False,
            "secure_jail": False,
            "end_wait": 0,
            "game_id": game_options["game_id"]
        }
        with open(mapPath, 'r') as map_file:
            game_options['map'] = map_file.read()

        game = Ants(game_options)
        return run_game(game, [bot0, bot1], engine_options, self.gateway)

        # if engine_options['replay_log']:
        #    intcpt_replay_io = StringIO()
        #    real_replay_io = engine_options['replay_log']
        #    engine_options['replay_log'] = intcpt_replay_io

        # for status in result['status']:
        #    print(status)
        # for turns in result['playerturns']:
        #    print(turns)
        # for score in result['score']:
        #    print(score)
        # if engine_options['replay_log']:
        #    replay_json = json.loads(intcpt_replay_io.getvalue())
        #    replay_json['playernames'] = [bot0.bot_name, bot1.bot_name];
        #    real_replay_io.write(json.dumps(replay_json))
        #    intcpt_replay_io.close()
        #    engine_options['replay_log'] = real_replay_io
        #    engine_options['replay_log'].close()
        #    visualizer.visualize_locally.launch(replay_path, False, html_path)

    def runTournament(self, tournamentDirectory, mapPath):
        allBots = ['hippo', 'lazarant', 'xathis', 'speedyBot', 'memetix', 'pkmiec']
        #allBots = ['hippo', 'lazarant', 'xathis', 'speedyBot',]
        gamesToPlay = 6
        gameSettings = seq(combinations(allBots, 2)) \
            .flat_map(lambda combo: seq(range(0, gamesToPlay)) \
            .map(lambda game_index: (combo[0], combo[1], game_index) if (game_index < 3) else (combo[1], combo[0], game_index)))\
            .list()
        print(f'Playing {len(gameSettings)} games')
        playResults:List[PlayResult] = seq(gameSettings).map(lambda tuple: self.play_game(Bot(tuple[0]), Bot(tuple[1]), mapPath)).list()

        os.makedirs(tournamentDirectory)
        for pr in playResults:
            replayPath = f'{tournamentDirectory}\\{pr.game_id}.json'
            htmlPath = f'{tournamentDirectory}\\{pr.game_id}.html'
            save_play_result(pr, replayPath)
            generate_visualization(replayPath, htmlPath)

        def sumTournamentScore (botType:str):
            bot = Bot(botType)
            def determineScore (bot: Bot, pr: PlayResult):
                botIndex = pr.playernames.index(bot.bot_name)
                otherBotIndex = 1 if botIndex ==0 else 0
                if pr.score[botIndex] == pr.score[otherBotIndex]:return 1
                if pr.score[botIndex] > pr.score[otherBotIndex]:return 2
                if pr.score[botIndex] < pr.score[otherBotIndex]:return 0
            botScore = seq(playResults) \
                .filter(lambda pr: bot.bot_name in pr.playernames) \
                .map(lambda pr: determineScore(bot, pr)) \
                .sum()
            return (bot.bot_name, botScore)

        finalScore = seq(allBots)\
            .map(sumTournamentScore)\
            .sorted(key=lambda tuple: tuple[1], reverse=True)\
            .list()
        print(finalScore)



        #def countScore(botName, gameResults):
        #    seq(gameResults)

        # tournamentScores = seq(allBots)\
        #    .map(lambda bot_name: (bot_name, seq(gameResults).filter(lambda gr: seq(gr.bot_types).filter(lambda bt: bot_name == bt).any())))
        #    .map()


def save_play_result(result: PlayResult, replay_path: str):
    stream = open(replay_path, 'w')
    stream.write(jsonpickle.encode(result))
    stream.close()


def generate_visualization(replay_path: str, html_path):
    visualizer.visualize_locally.launch(replay_path, False, html_path)
