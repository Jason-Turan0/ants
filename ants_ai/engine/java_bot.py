from py4j.java_gateway import JavaGateway
from ants_ai.engine.bot import Bot
from ants_ai.engine.bot_name import BotName


class JavaBot(Bot):
    def __init__(self, game_identifier: str, gateway: JavaGateway, name: BotName):
        super().__init__(game_identifier, name)
        self.turn_commands = ''
        self.gateway = gateway
        self.game_identifier = game_identifier

    def start(self, start_data):
        self.gateway.createPlayer(self.game_identifier, self.name.bot_name, self.name.bot_type, start_data)

    def play_turn(self, play_turn_data):
        data = self.gateway.playTurn(self.game_identifier, self.name.bot_name, play_turn_data)
        self.turn_commands = data

    def read_lines(self):
        return self.turn_commands.split('\n')
