from pprint import pprint
from typing import List, Tuple

import numpy
from functional import seq
from training.game_state.game_map import Direction, Position
from training.neural_network.game_state_translator import GameStateTranslator
from training.neural_network.neural_network_example import AntVision1DExample, AntMapExample, AntVision2DExample
from training.neural_network.position_state import PositionState
from numpy import ndarray
import pandas as pd
from sklearn.preprocessing import OneHotEncoder


class TrainingDataset:
    def __init__(self, features: ndarray, labels: ndarray):
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


def encode_2d_examples(examples: List[AntVision2DExample]) -> TrainingDataset:
    gst = GameStateTranslator()
    e = examples[0]
    rows = seq(e.features.keys()).group_by(lambda p: p.row).map(lambda t: t[0]).order_by(lambda r: r).to_list()
    columns = seq(e.features.keys()).group_by(lambda p: p.column).map(lambda t: t[0]).order_by(lambda c: c).to_list()
    channel_count = len(PositionState.__members__)
    features = numpy.zeros([len(examples), len(rows), len(columns), channel_count], dtype=int)
    # pprint(rows)
    # pprint(columns)

    for e_index, e in enumerate(examples):
        for r_index, r in enumerate(rows):
            for c_index, c in enumerate(columns):
                key = Position(r, c)
                if key not in e.features.keys():
                    print(key)
                    pprint(e.features)
                    raise ValueError('invalid example')
                features[e_index, r_index, c_index] = gst.convert_enum_to_array(e.features[key],
                                                                                PositionState)

    labels = [gst.convert_enum_to_array(ex.label, Direction) for ex in examples]
    return TrainingDataset(features, numpy.array(labels))


def encode_map_examples(row_count: int, column_count: int, examples: List[AntMapExample]) -> TrainingDataset:
    t = GameStateTranslator()
    features = []
    for e in examples:
        encoded_example = [[t.convert_enum_to_array(e.features[Position(row, col)], PositionState) for col in
                            range(column_count)] for row in range(row_count)]
        features.append(encoded_example)
    labels = [t.convert_enum_to_array(ex.label, Direction) for ex in examples]
    return TrainingDataset(numpy.array(features), numpy.array(labels))
