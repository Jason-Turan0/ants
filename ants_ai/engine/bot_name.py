from re import sub


class BotName:
    def __init__(self, bot_type: str, bot_name: str = None):
        self.bot_type = bot_type
        self.bot_name = bot_name if (bot_name) else f'{bot_type}_1'

    def __eq__(self, other):
        if not isinstance(other, BotName):
            return NotImplemented
        elif self is other:
            return True
        else:
            return self.__dict__ == other.__dict__


def from_name(botName) -> BotName:
    return BotName(sub('_\d', '', botName))
