from ants_ai.training.game_state.game_map import Position
from engine.bot import BotName


class HillTurn:
    def __init__(self, turn_number: int, owner_bot: BotName, position: Position, is_alive: bool):
        self.turn_number = turn_number
        self.owner_bot = owner_bot
        self.position = position
        self.is_alive = is_alive
