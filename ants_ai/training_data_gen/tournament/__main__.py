from ants_ai.training_data_gen.tournament.tournament_runner import TournamentRunner
from py4j.java_gateway import JavaGateway
from datetime import datetime
import os
def main(args=None):
    gateway = JavaGateway()
    gr = TournamentRunner(gateway)
    mapPath = f'{os.getcwd()}\\ants_ai\\training_data_gen\\engine\\maps\\training\\small.map'
    tournamentTime = datetime.today().strftime('%Y-%m-%d-%H-%M-%S')
    tournamentPath = f'{os.getcwd()}\\tournaments\\{tournamentTime}'
    gr.runTournament(tournamentPath, mapPath)

if __name__ == "__main__":
    main()
