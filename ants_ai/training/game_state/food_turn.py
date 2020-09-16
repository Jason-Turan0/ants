from ants_ai.training.game_state.game_map import Position


class FoodTurn:
    def __init__(self, turn_number: int, position: Position):
        self.turn_number = turn_number
        self.position = position
