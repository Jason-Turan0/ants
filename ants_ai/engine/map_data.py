from typing import List


class MapData:
    def __init__(self, cols: int, data: List[str], rows: int):
        self.rows = rows
        self.cols = cols
        self.data = data
