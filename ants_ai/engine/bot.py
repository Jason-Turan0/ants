from abc import abstractmethod

from engine.bot_name import BotName


class Bot:
    def __init__(self, game_identifier: str, name: BotName):
        self.game_identifier = game_identifier
        self.name = name

    @abstractmethod
    def start(self, startData):
        pass

    @abstractmethod
    def play_turn(self, playTurnData):
        pass

    @abstractmethod
    def read_lines(self):
        pass

    @property
    def is_alive(self):
        return True

    def kill(self):
        pass

    def retrieve(self):
        pass

    def release(self):
        pass

    def pause(self):
        pass

    def resume(self):
        pass

    def _child_writer(self):
        pass

    def write(self, str):
        pass

    def write_line(self, line):
        pass

    def read_line(self, timeout=0):
        pass

    def read_error(self, timeout=0):
        pass

    def check_path(self, path, errors):
        pass
