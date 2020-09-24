import sys
from itertools import combinations
from uuid import uuid4
import jsonpickle
from ants_ai.engine.bot import Bot
from functional import seq
from ants_ai.engine.visualizer import visualize_locally
from ants_ai.engine.ants import Ants
from ants_ai.engine.bot_name import BotName
from ants_ai.engine.engine import run_game
from ants_ai.engine.play_result import PlayResult
from typing import List, Tuple
import os

from ants_ai.engine.java_bot import JavaBot


class TournamentRunner:
    def __init__(self, gateway):
        self.gateway = gateway
        self.all_bots = ['hippo', 'lazarant', 'xathis', 'speedyBot', 'memetix', 'pkmiec']

    def create_options(self, map_path: str, game_id: str) -> Tuple[dict, dict]:
        turns = 200
        loadtime = 3000
        turntime = 1000
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
        engine_options = {
            "loadtime": loadtime,
            "turntime": turntime,
            "map_file": map_path,
            "turns": turns,
            "verbose_log": sys.stdout,
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
        with open(map_path, 'r') as map_file:
            game_options['map'] = map_file.read()
        return game_options, engine_options

    def play_game(self, bot0: BotName, bot1: BotName, game_id: str, map_path: str) -> PlayResult:
        game_options, engine_options = self.create_options(map_path, game_id)
        game = Ants(game_options)
        return run_game(game, [JavaBot(game_id, self.gateway, bot0), JavaBot(game_id, self.gateway, bot1)],
                        engine_options)

    def play_game_with_bots(self, bot0: Bot, bot1: Bot, game_id: str, map_path: str) -> PlayResult:
        game_options, engine_options = self.create_options(map_path, game_id)
        game = Ants(game_options)
        return run_game(game, [bot0, bot1], engine_options)

    def play_games(self, save_path: str, map_path: str,
                   game_settings: List[Tuple[str, str, int]]):
        print(f'Playing {len(game_settings)} games')

        play_results: List[PlayResult] = seq(game_settings).map(
            lambda tuple: self.play_game(BotName(tuple[0]), BotName(tuple[1]), str(uuid4()), map_path)).list()

        os.makedirs(save_path)
        for pr in play_results:
            replay_path = f'{save_path}/{pr.game_id}.json'
            html_path = f'{save_path}/{pr.game_id}.html'
            save_play_result(pr, replay_path)
            generate_visualization(replay_path, html_path)

        def sum_tournament_score(botType: str):
            bot = BotName(botType)

            def determine_score(bot: BotName, pr: PlayResult):
                bot_index = pr.playernames.index(bot.bot_name)
                other_bot_index = 1 if bot_index == 0 else 0
                if pr.score[bot_index] == pr.score[other_bot_index]: return 1
                if pr.score[bot_index] > pr.score[other_bot_index]: return 2
                if pr.score[bot_index] < pr.score[other_bot_index]: return 0

            def determine_winner(bot: BotName, pr: PlayResult):
                bot_index = pr.playernames.index(bot.bot_name)
                other_bot_index = 1 if bot_index == 0 else 0
                return 1 if pr.score[bot_index] > pr.score[other_bot_index] else 0

            games_played = seq(play_results) \
                .count(lambda pr: bot.bot_name in pr.playernames)
            bot_score = seq(play_results) \
                .filter(lambda pr: bot.bot_name in pr.playernames) \
                .map(lambda pr: determine_score(bot, pr)) \
                .sum()
            games_won = seq(play_results) \
                .filter(lambda pr: bot.bot_name in pr.playernames) \
                .map(lambda pr: determine_winner(bot, pr)) \
                .sum()
            return bot.bot_name, games_played, 0 if games_played == 0 else (games_won / games_played) * 100, bot_score

        final_score = seq(self.all_bots) \
            .map(sum_tournament_score) \
            .sorted(key=lambda tuple: tuple[1], reverse=True) \
            .list()
        print(final_score)

    def generate_game_data(self, save_path: str, map_path: str, playing_bot: str, game_count: int):
        other_bots = [b for b in self.all_bots if b != playing_bot]

        def create_tuple(game_index):
            playing_bot_first = (playing_bot, other_bots[game_index % len(other_bots)], game_index)
            playing_bot_second = (other_bots[game_index % len(other_bots)], playing_bot, game_index)
            return playing_bot_first if (game_index < game_count / 2) else playing_bot_second

        game_settings = [create_tuple(game_index) for game_index in range(game_count)]
        self.play_games(save_path, map_path, game_settings)

    def run_tournament(self, save_path, map_path, game_count):
        allBots = ['hippo', 'lazarant', 'xathis', 'speedyBot', 'memetix', 'pkmiec']
        game_settings = seq(combinations(allBots, 2)) \
            .flat_map(lambda combo: seq(range(0, game_count)) \
                      .map(lambda game_index: (combo[0], combo[1], game_index) if (game_index < game_count / 2) else (
            combo[1], combo[0], game_index))) \
            .list()
        self.play_games(save_path, map_path, game_settings)


def save_play_result(result: PlayResult, replay_path: str):
    stream = open(replay_path, 'w')
    stream.write(jsonpickle.encode(result))
    stream.close()


def generate_visualization(replay_path: str, html_path):
    visualize_locally.launch(replay_path, False, html_path)
