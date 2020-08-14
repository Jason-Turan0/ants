from training.game_state.position import Position
from training_data_gen.engine.map_data import MapData
from functional import seq
from enum import Enum
from typing import Dict

class TerrainType(Enum):
    WATER = 1
    LAND = 2

class GameMap:
    def __init__ (self, map_data : MapData):
        self.columnCount = map_data.cols
        self.rowCount = map_data.rows

        def charToTerrainType (char: str):
            if(char == '%'):return TerrainType.WATER
            return TerrainType.LAND

        self.terrain : Dict[Position, TerrainType] = seq(range(0, self.rowCount))\
            .flat_map(lambda row : map(lambda column : Position(row, column), range(0, self.columnCount)))\
            .map(lambda position : (position, charToTerrainType(map_data.data[position.row][position.column])))\
            .to_dict()

    def getTerrain(self, position: Position) -> TerrainType:
        return self.terrain.get(position)