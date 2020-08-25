import math
from math import sqrt, ceil, floor
from training_data_gen.engine.map_data import MapData
from functional import seq
from enum import Enum
from typing import Dict, List
from dataclasses import dataclass


class TerrainType(Enum):
    WATER = 1
    LAND = 2


class Direction(Enum):
    NORTH = 'n'
    EAST = 'e'
    SOUTH = 's'
    WEST = 'w'
    NONE = '-'


@dataclass(frozen=True)
class Position:
    row: int
    column: int

    def calculate_distance(self, other) -> float:
        return math.sqrt(((self.row - other.row) ** 2) + ((self.column - other.column) ** 2))

    def __lt__(self, other):
        return (self.row, self.column) < (other.row, other.column)


class GameMap:
    def __init__(self, map_data: MapData):
        self.column_count = map_data.cols
        self.row_count = map_data.rows
        self.direction_change = {
            Direction.NORTH: (-1, 0),
            Direction.EAST: (0, 1),
            Direction.SOUTH: (1, 0),
            Direction.WEST: (0, -1),
            Direction.NONE: (0, 0),
        }

        def charToTerrainType(char: str):
            if char == '%': return TerrainType.WATER
            return TerrainType.LAND

        self.terrain: Dict[Position, TerrainType] = seq(range(0, self.row_count)) \
            .flat_map(lambda row: map(lambda column: Position(row, column), range(0, self.column_count))) \
            .map(lambda position: (position, charToTerrainType(map_data.data[position.row][position.column]))) \
            .to_dict()

    def get_terrain(self, position: Position) -> TerrainType:
        return self.terrain.get(position)

    def wrap_position(self, new_row, new_col) -> Position:
        if new_row >= self.row_count:
            wrapped_row = new_row % self.row_count
        elif new_row < 0:
            wrapped_row = self.row_count + (-1 * (abs(new_row) % self.row_count))
        else:
            wrapped_row = new_row

        if new_col >= self.column_count:
            wrapped_col = new_col % self.column_count
        elif new_col < 0:
            wrapped_col = self.column_count + (-1 * (abs(new_col) % self.column_count))
        else:
            wrapped_col = new_col
        return Position(wrapped_row, wrapped_col)

    def adjacent_movement_position(self, position: Position, direction: Direction) -> Position:
        (row_change, col_change) = self.direction_change[direction]
        return self.wrap_position(position.row + row_change, position.column + col_change)

    def get_positions_within_distance(self, pos: Position, radius_squared: int) -> List[Position]:
        distance = sqrt(radius_squared)
        d_ceil = ceil(distance)
        positions = [self.wrap_position(pos.row+x,pos.column+y) \
                     for x in range(-d_ceil, d_ceil) for y in range(-d_ceil, d_ceil) \
                     if Position(pos.row + x,pos.column + y).calculate_distance(pos) < distance]
        return positions


        #return seq(distances) \
        #    .map(lambda d: Position(pos.row + d[0], pos.column + d[1])) \
        #    .filter(lambda p: p.calculate_distance(pos) < distance) \
        #    .map(lambda p: self.wrap_position(p.row, p.column)) \
        #    .order_by(lambda p: p) \
        #    .list()
