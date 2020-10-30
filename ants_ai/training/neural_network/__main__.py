import argparse
import glob
import os
from typing import List

import jsonpickle
from ants_ai.training.game_state.game_state import GameState
from ants_ai.training.game_state.generator import GameStateGenerator
from ants_ai.training.neural_network.factories.combined_model_factory import CombinedModelFactory
from ants_ai.training.neural_network.factories.conv2d_model_factory import Conv2DModelFactory
from ants_ai.training.neural_network.factories.map_view_model_factory import MapViewModelFactory
from ants_ai.training.neural_network.trainer.model_trainer import ModelTrainer
from ants_ai.training.neural_network.trainer.run_stats import RunStats
from functional import seq
from matplotlib import pyplot as plt
from prettytable import PrettyTable


def read_file_contents(path):
    with open(path, "r") as f:
        return f.read()


def plot_learning_curve(data, model_name: str):
    # summarize history for accuracy
    plt.plot([d[0] for d in data], [d[1] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[2] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[3] for d in data], linestyle='--', marker='.')
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Loss')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation', 'test'], loc='upper left')
    plt.show()

    plt.plot([d[0] for d in data], [d[4] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[5] for d in data], linestyle='--', marker='.')
    plt.plot([d[0] for d in data], [d[6] for d in data], linestyle='--', marker='.')
    plt.title('Learning curve ' + model_name)
    plt.ylabel('Categorical Accuracy')
    plt.xlabel('Training Size')
    plt.legend(['train', 'cross_validation', 'test'], loc='upper left')
    plt.show()


def show_learning_curve(data_path: str, model_name: str):
    run_stats: List[RunStats] = [jsonpickle.decode(read_file_contents(path)) for path in
                                 glob.glob(f'{data_path}\\logs\\fit\\**\\run_stats.json')]

    def get_data_points(rs: RunStats):
        val_loss = seq(rs.history["val_loss"]).last()
        val_cat_acc = seq(rs.history["val_categorical_accuracy"]).last()
        return (
            rs.train_shape[0][0],
            seq(rs.history["loss"]).last(),
            val_loss,
            rs.test_loss if hasattr(rs, 'test_loss') else val_loss,

            seq(rs.history["categorical_accuracy"]).last(),
            val_cat_acc,
            rs.test_categorical_accuracy if hasattr(rs, 'test_categorical_accuracy') else val_cat_acc
        )

    data_points = seq([get_data_points(rs) for
                       rs in run_stats if
                       rs.model_name == model_name]) \
        .filter(lambda t: t[0] > 4000) \
        .order_by(lambda t: t[0]) \
        .to_list()
    pt = PrettyTable()
    pt.field_names = ['train_size', 'train_loss', 'val_loss', 'test_loss', 'train_acc', 'val_acc', 'test_acc']
    for row in data_points:
        pt.add_row(row)
    print(pt)

    plot_learning_curve(data_points, model_name)


def load_game_state(path: str, gsg: GameStateGenerator) -> GameState:
    with open(path, "r") as f:
        json_data = f.read()
        pr = jsonpickle.decode(json_data)
        return gsg.generate(pr)


def get_game_paths(data_path) -> List[str]:
    return glob.glob(os.path.join(data_path, 'training', '**', '*.json'))

# def convert_to_dir(arr: List[float]) -> List[Tuple[float, Direction]]:
#     blah = list(Direction.__members__.items())
#     blahReturn: List[Tuple[float, Direction]] = []
#     for index, prob in enumerate(arr):
#         # print(index, prob)
#         blahReturn.append((prob, blah[index][1]))
#     return blahReturn
#
#
# def main1():
#     bot_to_emulate = 'memetix_1'
#     game_paths = [f for f in glob.glob(f'{os.getcwd()}\\training\\tests\\test_data\\**\\*.json')]
#     train_games = game_paths[:1]
#     test_games = game_paths[2:3]
#     mv_mf = MapViewModelFactory(bot_to_emulate)
#     conv_mf = Conv2DModelFactory(bot_to_emulate)
#     mt = ModelTrainer()
#
#     trained_conv, trained_conv_stats = mt.train_model(train_games, conv_mf)
#     trained_mv, trained_mv_stats = mt.train_model(train_games, mv_mf)
#
#     gst = GameStateTranslator()
#     gsg = GameStateGenerator()
#     game_states = seq(test_games).map(lambda path: load_game_state(path, gsg)).to_list()
#     mv_features, mv_labels = enc.encode_map_examples(gst.convert_to_global_antmap(bot_to_emulate, game_states), 7)
#     av_features, av_labels = enc.encode_2d_examples(gst.convert_to_2d_ant_vision(bot_to_emulate, game_states), 7)
#
#     prediction_conv = trained_conv.predict(av_features)
#     prediction_mv = trained_mv.predict(mv_features)
#     print(prediction_conv.shape)
#     print(prediction_mv.shape)
#     predictions = []
#     actuals = [seq(convert_to_dir(av_labels[index])).max_by(lambda t: t[0])[1] for index in range(av_labels.shape[0])]
#     for row_index in range(prediction_mv.shape[0]):
#         blah = prediction_conv[row_index]
#         blah1 = prediction_mv[row_index]
#         # pprint(blah)
#         # pprint(blah1)
#         dir_probs = convert_to_dir(blah)
#         dir_probs.extend(convert_to_dir(blah1))
#         predictions.append(seq(dir_probs).max_by(lambda t: t[0])[1])
#
#     print(actuals[0])
#     print(predictions[0])
#     acc = seq(range(len(predictions))).filter(lambda i: predictions[i] == actuals[i]).len() / len(actuals)
#     print(acc)

def main(data_path: str):
    bot_to_emulate = 'memetix_1'
    print('FOOBAR')
    game_paths = get_game_paths(data_path)
    print(len(game_paths))
    game_lengths = [1]
    # seq = HybridSequence(game_paths, 50, bot_to_emulate)
    # seq.build_indexes()
    mt = ModelTrainer(data_path)
    map_factory = MapViewModelFactory(bot_to_emulate)
    conv2D_factory = Conv2DModelFactory(bot_to_emulate)

    for gl in game_lengths:
        train_game_paths = game_paths[0:gl]

        conv_model, conv_stats = mt.train_model(train_game_paths, conv2D_factory)
        map_model, map_stats = mt.train_model(train_game_paths, map_factory)

        combined_factory = CombinedModelFactory(bot_to_emulate, conv_stats.weight_path, map_stats.weight_path)
        combined_model, combined_stats = mt.train_model(train_game_paths, combined_factory)
        print(f"Finished training on game length {gl}. Test acc {combined_stats.test_categorical_accuracy}")


# Total data as of 10-19-2020
# 1145 Games
# 1,597,269 Examples
def main1(data_path: str):
    bot_to_emulate = 'memetix_1'
    game_paths = get_game_paths(data_path)
    game_lengths = [2]
    mt = ModelTrainer(data_path)
    conv_factory = Conv2DModelFactory(bot_to_emulate)

    for gl in game_lengths:
        train_game_paths = game_paths[0:gl]
        conv_model, conv_stats = mt.train_model(train_game_paths, conv_factory)
        print(conv_stats.test_categorical_accuracy)


def compare_model_learning_curve(data_path: str):
    show_learning_curve(data_path, 'mapview_2d')
    show_learning_curve(data_path, 'conv_2d')
    show_learning_curve(data_path, 'combined_2d')


if __name__ == "__main__":
    default_data_path = os.path.abspath('../ants_ai_data')
    parser = argparse.ArgumentParser(description='Performs training')
    parser.add_argument('-dp', '--data-path', help='The root folder to look for training data and save training stats',
                        default=default_data_path)
    args = parser.parse_args()
    main1(args.data_path)
    # compare_model_learning_curve(args.data_path)
    #map_factory = CombinedModelFactory('memetix_1',
    #                                   rf'E:\ants_ai_data\logs\fit\conv_2d_20201019-142704\conv_2d_weights',
    #                                   rf'E:\ants_ai_data\logs\fit\mapview_2d_20201020-113608\mapview_2d_weights')
    #model = map_factory.construct_model({})
    #model.summary()
    # conv2d 576997
    # mapview 101861
    # combined total 1,260,665 trainable 581,797 nontrainable 678,858
