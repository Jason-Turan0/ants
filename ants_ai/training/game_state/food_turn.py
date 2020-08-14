from training.game_state.position import Position


class FoodTurn:
    def __init__(self, turn_number: int, position: Position):
        self.turn_number = turn_number
        self.position = position
