from py4j.java_gateway import JavaGateway


class House:
    def __init__(self, gameIdentifier, playerName,playerType, gateway):         
        self.turnCommands =''
        self.gateway = gateway
        self.gameIdentifier = gameIdentifier
        self.playerName = playerName
        self.playerType =playerType
        
    @property
    def is_alive(self):
        return True

    def start(self, startData):
        self.gateway.createPlayer(self.gameIdentifier, self.playerName, self.playerType, startData)
                
    def playTurn(self, playTurnData):
        data = self.gateway.playTurn(self.gameIdentifier, self.playerName, playTurnData)
        self.turnCommands = data 
        
    def read_lines(self):
        return self.turnCommands.split('\n')

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

def get_sandbox(gameIdentifier:str, playerName:str, playerType:str, gateway: JavaGateway):
    return House(gameIdentifier, playerName,playerType, gateway)
