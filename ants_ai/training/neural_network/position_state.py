from enum import IntEnum


class PositionState(IntEnum):
    FRIENDLY_ANT = 0,
    HOSTILE_ANT = 1,
    FRIENDLY_HILL = 2,
    HOSTILE_HILL = 3,
    FOOD = 4,
    WATER = 5,
    LAND = 6
