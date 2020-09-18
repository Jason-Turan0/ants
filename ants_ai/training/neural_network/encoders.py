from pprint import pprint
from typing import List, Union, Dict

import numpy
from functional import seq
from ants_ai.training.game_state.game_map import Direction, Position
from ants_ai.training.neural_network.game_state_translator import GameStateTranslator
from ants_ai.training.neural_network.neural_network_example import AntVision1DExample, AntMapExample, AntVision2DExample
from ants_ai.training.neural_network.position_state import PositionState
from numpy import ndarray
import pandas as pd
from sklearn.preprocessing import OneHotEncoder


class TrainingDataset:
    def __init__(self, features: Union[ndarray, List[ndarray]], labels: ndarray):
        self.features = features
        self.labels = labels


def encode_flat_examples(examples: List[AntVision1DExample]) -> TrainingDataset:
    stringData = [[f.name for f in ex.features] for ex in examples]
    categories = [[m for m in PositionState.__members__] for i in range(len(examples[0].features))]
    columns = [f'pos_{num}' for num, ex in enumerate(examples[0].features)]
    in_df = pd.DataFrame(data=stringData, columns=columns)
    one_hot_encoder = OneHotEncoder(sparse=False, categories=categories)
    one_hot_encoder.fit(in_df)
    encoded_features = one_hot_encoder.transform(in_df)
    labels = [[e.label.name] for e in examples]
    label_df = pd.DataFrame(data=labels, columns=['direction'])
    label_categories = [d for d in Direction.__members__]
    one_hot_label_encoder = OneHotEncoder(sparse=False, categories=[label_categories])
    one_hot_label_encoder.fit(label_df)
    encoded_labels = one_hot_label_encoder.transform(label_df)
    return TrainingDataset(encoded_features, encoded_labels)


def encode_1d_examples(examples: List[AntVision1DExample]) -> TrainingDataset:
    t = GameStateTranslator()
    features = numpy.array(
        [[t.convert_enum_to_array(f, PositionState) for f in ex.features] for ex in examples])
    labels = numpy.array([t.convert_enum_to_array(ex.label, Direction) for ex in examples])
    return TrainingDataset(features, labels)


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


def encode_2d_examples(examples: List[AntVision2DExample], channel_count: int) -> TrainingDataset:
    gst = GameStateTranslator()
    features = encode_2d_features(seq(examples).map(lambda e: e.features).to_list(), gst, channel_count)
    labels = [gst.convert_enum_to_array(ex.label, Direction) for ex in examples]
    return TrainingDataset(features, numpy.array(labels))


def encode_map_examples(examples: List[AntMapExample], channel_count: int) -> TrainingDataset:
    gst = GameStateTranslator()
    assert (len(examples) > 0)
    ex = examples[0]
    features = numpy.zeros([len(examples), ex.row_count, ex.column_count, channel_count], dtype=int)
    for e_index, e in enumerate(examples):
        for r in range(ex.row_count):
            for c in range(ex.column_count):
                key = Position(r, c)
                features[e_index, r, c] = down_sample(gst, e.features[key], channel_count)
    labels = [gst.convert_enum_to_array(ex.label, Direction) for ex in examples]
    return TrainingDataset(numpy.array(features), numpy.array(labels))
