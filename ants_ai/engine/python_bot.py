from engine.bot import Bot
from engine.bot_name import BotName


class PythonBot(Bot):
    def __init__(self, gameIdentifier: str, game_identifier: str, name: BotName):
        super().__init__(game_identifier, name)
        self.turnCommands = ''
        self.gameIdentifier = gameIdentifier

    def start(self, start_data):
        pass

    def play_turn(self, play_turn_data):
        pass

    def read_lines(self):
        return self.turnCommands.split('\n')
