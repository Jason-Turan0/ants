import math
from math import sqrt, ceil, floor
from ants_ai.engine.map_data import MapData
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


@dataclass(eq=True, frozen=True)
class Position:
    row: int
    column: int

    def calculate_distance(self, other) -> float:
        return math.sqrt(((self.row - other.row) ** 2) + ((self.column - other.column) ** 2))

    def __lt__(self, other):
        return (self.row, self.column) < (other.row, other.column)

    def __hash__(self):
        return hash((self.row, self.column))


class GameMap:
    def __init__(self, row_count: int, column_count: int, terrain: Dict[Position, TerrainType]):
        self.column_count = column_count
        self.row_count = row_count
        self.terrain = terrain
        self.direction_change = {
            Direction.NORTH: (-1, 0),
            Direction.EAST: (0, 1),
            Direction.SOUTH: (1, 0),
            Direction.WEST: (0, -1),
            Direction.NONE: (0, 0),
        }

    def get_terrain(self, position: Position) -> TerrainType:
        return self.terrain.get(position)

    def update_terrain(self, position: Position, terrain: TerrainType):
        self.terrain[position] = terrain

    def wrap_position(self, new_row: int, new_col: int) -> Position:
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

    def get_positions_within_distance(self, pos: Position, radius_squared: int, use_absolute: bool = True,
                                      crop_to_square: bool = False) -> List[
        Position]:
        radius = sqrt(radius_squared)
        if crop_to_square:
            d_ceil = floor(sqrt((radius ** 2) / 2))
            positions = [self.wrap_position(pos.row + x, pos.column + y) if use_absolute else Position(x, y) \
                         for x in range(-d_ceil, d_ceil) for y in range(-d_ceil, d_ceil)]
            return positions
        else:
            d_ceil = ceil(radius)
            positions = [self.wrap_position(pos.row + x, pos.column + y) if use_absolute else Position(x, y) \
                         for x in range(-d_ceil, d_ceil) for y in range(-d_ceil, d_ceil) \
                         if Position(pos.row + x, pos.column + y).calculate_distance(pos) < radius]
            return positions


def create_from_map_data(map_data: MapData) -> GameMap:
    def charToTerrainType(char: str):
        if char == '%': return TerrainType.WATER
        return TerrainType.LAND

    terrain: Dict[Position, TerrainType] = seq(range(0, map_data.rows)) \
        .flat_map(lambda row: map(lambda column: Position(row, column), range(0, map_data.cols))) \
        .map(lambda position: (position, charToTerrainType(map_data.data[position.row][position.column]))) \
        .to_dict()
    return GameMap(map_data.rows, map_data.cols, terrain)


def create_map(row_count: int, column_count: int) -> GameMap:
    terrain: Dict[Position, TerrainType] = {Position(row, column): TerrainType.LAND
                                            for row in range(row_count) for column in range(column_count)}
    return GameMap(row_count, column_count, terrain)
