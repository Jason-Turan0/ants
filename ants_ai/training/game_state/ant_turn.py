from ants_ai.training.game_state.game_map import Position, Direction
from engine.bot import BotName


class AntTurn:
    def __init__(self, turn_number: int, bot: BotName, position: Position, row, next_direction: Direction):
        self.turn_number = turn_number
        self.bot = bot
        self.position = position
        self.row = row
        self.next_direction = next_direction
