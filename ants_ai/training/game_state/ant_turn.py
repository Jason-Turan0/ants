from training.game_state.position import Position
from training_data_gen.engine.bot import Bot


class AntTurn:
    def __init__(self, turn_number: int, bot: Bot, position: Position):
        self.turn_number = turn_number
        self.bot = bot
        self.position = position