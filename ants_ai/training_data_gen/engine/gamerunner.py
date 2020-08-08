from generator import GameRunner, BotDescription
from generator import RunGameResult
from optparse import OptionParser, OptionGroup
from ants import GpBot
from engine import run_game

class gamerunner(GameRunner):
    
    def runGame(self, mapFilePath, botDescriptions):
        opts = self.generateOptions();
        game_options = {            
            "attack": opts.attack,
            "kill_points": opts.kill_points,
            "food": opts.food,
            "viewradius2": opts.viewradius2,
            "attackradius2": opts.attackradius2,
            "spawnradius2": opts.spawnradius2,
            "loadtime": opts.loadtime,
            "turntime": opts.turntime,
            "turns": opts.turns,
            "food_rate": opts.food_rate,
            "food_turn": opts.food_turn,
            "food_start": opts.food_start,
            "food_visible": opts.food_visible,
            "cutoff_turn": opts.cutoff_turn,
            "cutoff_percent": opts.cutoff_percent,
            "scenario": opts.scenario 
        }
        engine_options = {
            "loadtime": opts.loadtime,
            "turntime": opts.turntime,
            "map_file": mapFilePath,
            "turns": opts.turns,
            "log_replay": opts.log_replay,
            "log_stream": opts.log_stream,
            "log_input": opts.log_input,
            "log_output": opts.log_output,
            "log_error": opts.log_error,
            "serial": opts.serial,
            "strict": opts.strict,
            "capture_errors": opts.capture_errors,
            "secure_jail": opts.secure_jail,
            "end_wait": opts.end_wait 
        }
        
        with open(mapFilePath, 'r') as map_file:
            game_options['map'] = map_file.read()
        bots = [[bot.folderPath, bot.botCommand] for bot in botDescriptions ]
        
        game = GpBot(game_options)
        result = run_game(game, bots, engine_options)
        runGameResult = RunGameResult()
        runGameResult.playerOneScore =result['score'][0];
        runGameResult.playerTwoScore =result['score'][1];
        runGameResult.playerOneStatus =result['status'][0];
        runGameResult.playerTwoStatus =result['status'][1];
        runGameResult.gameLength = result['game_length'];
        return runGameResult
        
    def generateOptions(self):
        parser = OptionParser()
        # map to be played
        # number of players is determined by the map file
        

        # maximum number of turns that the game will be played
        parser.add_option("-t", "--turns", dest="turns",
                      default=1000, type="int",
                      help="Number of turns in the game")

        parser.add_option("--serial", dest="serial",
                      action="store_true",
                      help="Run bots in serial, instead of parallel.")

        parser.add_option("--turntime", dest="turntime",
                      default=1000, type="int",
                      help="Amount of time to give each bot, in milliseconds")
        parser.add_option("--loadtime", dest="loadtime",
                      default=3000, type="int",
                      help="Amount of time to give for load, in milliseconds")
        parser.add_option("-r", "--rounds", dest="rounds",
                      default=1, type="int",
                      help="Number of rounds to play")
        parser.add_option("--player_seed", dest="player_seed",
                      default=None, type="int",
                      help="Player seed for the random number generator")
        parser.add_option("--engine_seed", dest="engine_seed",
                      default=None, type="int",
                      help="Engine seed for the random number generator")
    
        parser.add_option('--strict', dest='strict',
                      action='store_true', default=False,
                      help='Strict mode enforces valid moves for bots')
        parser.add_option('--capture_errors', dest='capture_errors',
                      action='store_true', default=False,
                      help='Capture errors and stderr in game result')
        parser.add_option('--end_wait', dest='end_wait',
                      default=0, type="float",
                      help='Seconds to wait at end for bots to process end')
        parser.add_option('--secure_jail', dest='secure_jail',
                      action='store_true', default=False,
                      help='Use the secure jail for each bot (*nix only)')
        parser.add_option('--fill', dest='fill',
                      action='store_true', default=False,
                      help='Fill up extra player starts with last bot specified')
        parser.add_option('-p', '--position', dest='position',
                      default=0, type='int',
                      help='Player position for first bot specified')

        # ants specific game options
        game_group = OptionGroup(parser, "Game Options", "Options that affect the game mechanics for ants")
        game_group.add_option("--attack", dest="attack",
                              default="focus",
                              help="Attack method to use for engine. (closest, focus, support, damage)")
        game_group.add_option("--kill_points", dest="kill_points",
                              default=2, type="int",
                              help="Points awarded for killing a hill")
        game_group.add_option("--food", dest="food",
                              default="symmetric",
                              help="Food spawning method. (none, random, sections, symmetric)")
        game_group.add_option("--viewradius2", dest="viewradius2",
                              default=77, type="int",
                              help="Vision radius of ants squared")
        game_group.add_option("--spawnradius2", dest="spawnradius2",
                              default=1, type="int",
                              help="Spawn radius of ants squared")
        game_group.add_option("--attackradius2", dest="attackradius2",
                              default=5, type="int",
                              help="Attack radius of ants squared")
        game_group.add_option("--food_rate", dest="food_rate", nargs=2, type="int", default=(5,11),
                              help="Numerator of food per turn per player rate")
        game_group.add_option("--food_turn", dest="food_turn", nargs=2, type="int", default=(19,37),
                              help="Denominator of food per turn per player rate")
        game_group.add_option("--food_start", dest="food_start", nargs=2, type="int", default=(75,175),
                              help="One over percentage of land area filled with food at start")
        game_group.add_option("--food_visible", dest="food_visible", nargs=2, type="int", default=(3,5),
                              help="Amount of food guaranteed to be visible to starting ants")
        game_group.add_option("--cutoff_turn", dest="cutoff_turn", type="int", default=150,
                              help="Number of turns cutoff percentage is maintained to end game early")
        game_group.add_option("--cutoff_percent", dest="cutoff_percent", type="float", default=0.85,
                              help="Number of turns cutoff percentage is maintained to end game early")
        game_group.add_option("--scenario", dest="scenario",
                              action='store_true', default=False)
        parser.add_option_group(game_group)

        # the log directory must be specified for any logging to occur, except:
        #    bot errors to stderr
        #    verbose levels 1 & 2 to stdout and stderr
        #    profiling to stderr
        # the log directory will contain
        #    the replay or stream file used by the visualizer, if requested
        #    the bot input/output/error logs, if requested
        log_group = OptionGroup(parser, "Logging Options", "Options that control the logging")
        log_group.add_option("-g", "--game", dest="game_id", default=0, type='int',
                             help="game id to start at when numbering log files")
        log_group.add_option("-l", "--log_dir", dest="log_dir", default=None,
                             help="Directory to dump replay files to.")
        log_group.add_option('-R', '--log_replay', dest='log_replay',
                             action='store_true', default=False),
        log_group.add_option('-S', '--log_stream', dest='log_stream',
                             action='store_true', default=False),
        log_group.add_option("-I", "--log_input", dest="log_input",
                             action="store_true", default=False,
                             help="Log input streams sent to bots")
        log_group.add_option("-O", "--log_output", dest="log_output",
                             action="store_true", default=False,
                             help="Log output streams from bots")
        log_group.add_option("-E", "--log_error", dest="log_error",
                             action="store_true", default=False,
                             help="log error streams from bots")
        log_group.add_option('-e', '--log_stderr', dest='log_stderr',
                             action='store_true', default=False,
                             help='additionally log bot errors to stderr')
        log_group.add_option('-o', '--log_stdout', dest='log_stdout',
                             action='store_true', default=False,
                             help='additionally log replay/stream to stdout')
        # verbose will not print bot input/output/errors
        # only info+debug will print bot error output
        log_group.add_option("-v", "--verbose", dest="verbose",
                             action='store_true', default=False,
                             help="Print out status as game goes.")
        log_group.add_option("--profile", dest="profile",
                             action="store_true", default=False,
                             help="Run under the python profiler")
        parser.add_option("--nolaunch", dest="nolaunch",
                          action='store_true', default=False,
                          help="Prevent visualizer from launching")
        log_group.add_option("--html", dest="html_file",
                             default=None,
                             help="Output file name for an html replay")
        parser.add_option_group(log_group)
        (opts, args) = parser.parse_args([])
        return opts;