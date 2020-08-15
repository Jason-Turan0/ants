from training_data_gen.engine.map_data import MapData
from functional import seq
from enum import Enum
from typing import Dict
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


class GameMap:
    def __init__(self, map_data: MapData):
        self.columnCount = map_data.cols
        self.rowCount = map_data.rows
        self.direction_change = {
            Direction.NORTH: (-1, 0),
            Direction.EAST: (0, 1),
            Direction.SOUTH: (1, 0),
            Direction.WEST: (0, -1),
            Direction.NONE: (0, 0),
        }

        def charToTerrainType(char: str):
            if (char == '%'): return TerrainType.WATER
            return TerrainType.LAND

        self.terrain: Dict[Position, TerrainType] = seq(range(0, self.rowCount)) \
            .flat_map(lambda row: map(lambda column: Position(row, column), range(0, self.columnCount))) \
            .map(lambda position: (position, charToTerrainType(map_data.data[position.row][position.column]))) \
            .to_dict()

    def getTerrain(self, position: Position) -> TerrainType:
        return self.terrain.get(position)

    def adjacent_movement_position(self, position: Position, direction: Direction) -> Position:
        if self.terrain[position] == TerrainType.WATER: raise ValueError(f'Cannot start at water position row {position.row} column: {position.column}');
        (row_change, col_change) = self.direction_change[direction]
        new_row =position.row +row_change
        new_col = position.column + col_change
        if new_row == self.rowCount:
            new_pos = Position(0, new_col)
        elif new_row < 0:
            new_pos = Position(self.rowCount-1, new_col)
        elif new_col == self.columnCount:
            new_pos = Position(new_row, 0)
        elif new_col < 0:
            new_pos = Position(new_row, self.columnCount -1)
        else:
            new_pos = Position(new_row, new_col)

        if self.terrain[new_pos] == TerrainType.WATER: raise ValueError(f'Cannot move to water position row {new_pos.row} column: {new_pos.column}')
        return new_pos