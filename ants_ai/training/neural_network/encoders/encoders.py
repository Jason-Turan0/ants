from math import floor
from pprint import pprint
from typing import List, Union, Dict, Tuple

import numpy
from ants_ai.training.game_state.game_map import Direction, Position
from ants_ai.training.neural_network.encoders.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.encoders.neural_network_example import AntMapExample, AntVision2DExample
from ants_ai.training.neural_network.encoders.position_state import PositionState
from functional import seq
from numpy import ndarray


# from sklearn.preprocessing import OneHotEncoder


class LabeledDataset:
    def __init__(self, features: Union[ndarray, List[ndarray]], labels: ndarray):
        self.features = features
        self.labels = labels


class TrainingDataset:
    def __init__(self, train: LabeledDataset, cross_val: LabeledDataset, test: LabeledDataset):
        self.train = train
        self.cross_val = cross_val
        self.test = test

    def get_training_length(self) -> int:
        return self.train.features.shape[0] \
            if isinstance(self.train.features, ndarray) \
            else self.train.features[0].shape[0]


def down_sample(gst: GameStateTranslator, ps: PositionState, channel_count: int):
    if channel_count == 7:
        return gst.convert_enum_to_array(ps, PositionState)
    elif channel_count == 3:
        if ps == PositionState.FRIENDLY_ANT or ps == PositionState.FRIENDLY_HILL:
            return [1, 0, 0]
        elif ps == PositionState.HOSTILE_ANT or ps == PositionState.HOSTILE_HILL:
            return [0, 1, 0]
        elif ps == PositionState.FOOD:
            return [0, 0, 1]
        else:
            return [0, 0, 0]
    else:
        raise ValueError('Not implemented')


def encode_2d_features(examples: List[Dict[Position, PositionState]], gst: GameStateTranslator,
                       channel_count: int) -> ndarray:
    if len(examples) == 0: return numpy.empty([0, 12, 12, 7], dtype=int)
    first_feature = examples[0]
    rows = seq(first_feature.keys()).group_by(lambda p: p.row).map(lambda t: t[0]).order_by(lambda r: r).to_list()
    columns = seq(first_feature.keys()).group_by(lambda p: p.column).map(lambda t: t[0]).order_by(lambda c: c).to_list()
    encoded_features = numpy.zeros([len(examples), len(rows), len(columns), channel_count], dtype=int)
    for e_index, e in enumerate(examples):
        for r_index, r in enumerate(rows):
            for c_index, c in enumerate(columns):
                key = Position(r, c)
                if key not in e.keys():
                    print(key)
                    pprint(e)
                    raise ValueError(f'Invalid Feature {key}')
                encoded_features[e_index, r_index, c_index] = down_sample(gst, e[key], channel_count)
    return encoded_features


def encode_2d_examples(examples: List[AntVision2DExample], channel_count: int) -> Tuple[ndarray, ndarray]:
    gst = GameStateTranslator()
    features = encode_2d_features(seq(examples).map(lambda e: e.features).to_list(), gst, channel_count)
    labels = numpy.array([gst.convert_enum_to_array(ex.label, Direction) for ex in examples])
    return features, labels


def decode_ant_vision_2d_examples(encoded_examples: Tuple[ndarray, ndarray]) -> List[AntVision2DExample]:
    gst = GameStateTranslator()
    features, labels = encoded_examples
    feature_example_count = features.shape[0]
    feature_row_min = 0 - floor(features.shape[1] / 2)
    feature_row_max = 0 + floor(features.shape[1] / 2)
    feature_col_min = 0 - floor(features.shape[2] / 2)
    feature_col_max = 0 + floor(features.shape[2] / 2)
    row_nums = seq(range(feature_row_min, feature_row_max)).to_list()
    col_nums = seq(range(feature_col_min, feature_col_max)).to_list()
    if features.shape[3] != 7: raise ValueError(
        'Only implemented for 7 channel encoding since down_sampling eliminates information')

    items = []
    for ex_index in range(feature_example_count):
        example_features: Dict[Position, PositionState] = {}
        for row_index, row_num in enumerate(row_nums):
            for col_index, col_num in enumerate(col_nums):
                position = Position(row_num, col_num)
                enum_val = gst.convert_array_to_enum(features[ex_index, row_index, col_index].tolist(), PositionState)
                example_features[position] = enum_val

        direction = gst.convert_array_to_enum(labels[ex_index].tolist(), Direction)
        items.append(AntVision2DExample(example_features, direction))
    return items


def encode_map_examples(examples: List[AntMapExample], channel_count: int) -> Tuple[ndarray, ndarray]:
    gst = GameStateTranslator()
    if len(examples) == 0: return numpy.empty([0, 43, 39, 7], dtype=int), numpy.empty([0, 5], dtype=int)
    ex = examples[0]
    features = numpy.zeros([len(examples), ex.row_count, ex.column_count, channel_count], dtype=int)
    for e_index, e in enumerate(examples):
        for r in range(ex.row_count):
            for c in range(ex.column_count):
                key = Position(r, c)
                features[e_index, r, c] = down_sample(gst, e.features[key], channel_count)
    labels = [gst.convert_enum_to_array(ex.label, Direction) for ex in examples]
    return numpy.array(features), numpy.array(labels)


def decode_map_examples(encoded_examples: Tuple[ndarray, ndarray]) -> List[AntMapExample]:
    features, labels = encoded_examples
    items = []
    gst = GameStateTranslator()
    row_count = features.shape[1]
    col_count = features.shape[2]
    for ex_index in range(features.shape[0]):
        example_features: Dict[Position, PositionState] = {}
        for row_num in range(features.shape[1]):
            for col_num in range(features.shape[2]):
                position = Position(row_num, col_num)
                enum_val = gst.convert_array_to_enum(features[ex_index, row_num, col_num].tolist(), PositionState)
                example_features[position] = enum_val

        direction = gst.convert_array_to_enum(labels[ex_index].tolist(), Direction)
        items.append(AntMapExample(example_features, direction, row_count, col_count))
    return items
