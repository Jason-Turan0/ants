from typing import List
from ants_ai.training_data_gen.engine.replay_data import ReplayData
class PlayResult:
    def __init__(self,
                 challenge:str,
                 location: str,
                 game_id: str,
                 game_length: int,
                 playerturns: List[int],
                 rank: List[int],
                 replaydata: ReplayData,
                 score: List[int],
                 status: List[str],
                 playernames: List[str],
                 ant_counts: List[List[int]]
                 ):

        self.challenge= challenge
        self.location= location
        self.game_id= game_id
        self.status = status
        self.playerturns = playerturns
        self.score = score
        self.rank = rank
        self.replayformat ='json'
        self.replaydata = replaydata
        self.game_length = game_length
        self.ant_counts = ant_counts
        self.replaydata=replaydata
        self.score=score
        self.status=status
        self.playernames=playernames