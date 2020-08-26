from math import floor
from typing import List, Dict

import tensorflow as tf
from functional import seq
from tensorflow.keras.layers import Dense, InputLayer
from tensorflow.keras import Model
import unittest
from tensorflow import keras
from tensorflow.keras.layers.experimental.preprocessing import Normalization
from training.game_state.game_map import Direction
from training.neural_network.game_state_translator import PositionState, TRAINING_VIEW_SIZE, GameStateTranslater
from training.neural_network.neural_network_example import NeuralNetworkExample
from training.tests.test_utils import create_test_game_state
from tensorflow.keras import layers
from tensorflow.keras.layers.experimental.preprocessing import CategoryEncoding

#https://keras.io/examples/structured_data/structured_data_classification_from_scratch/
class AntsModel(Model):
    def __init__(self):
        super(AntsModel, self).__init__()
        input_size =  len(PositionState.__members__) * TRAINING_VIEW_SIZE


        self.hidden_layer = Dense(128, activation='relu')
        self.output_layer = Dense(len(Direction.__members__))

    def call(self, x):
        print('hidden')
        print(x)
        x = self.hidden_layer(x)
        print('output')
        return self.output_layer(x)

def encode_numerical_feature(feature, name, dataset):
    # Create a Normalization layer for our feature
    normalizer = Normalization()

    # Prepare a Dataset that only yields our feature
    feature_ds = dataset.map(lambda x, y: x[name])
    feature_ds = feature_ds.map(lambda x: tf.expand_dims(x, -1))

    # Learn the statistics of the data
    normalizer.adapt(feature_ds)

    # Normalize the input feature
    encoded_feature = normalizer(feature)
    return encoded_feature

class TestTraining(unittest.TestCase):
    @unittest.skip('WIP')
    def test_training(self):
        model = AntsModel()
        keras.utils.plot_model(model, show_shapes=True, rankdir="LR")
        game_state = create_test_game_state()
        translator = GameStateTranslater()
        ds = translator.convert_to_nn_input('pkmiec_1', [game_state])
        loss_object = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
        optimizer = tf.keras.optimizers.Adam()
        train_loss = tf.keras.metrics.Mean(name='train_loss')
        train_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name='train_accuracy')

        test_loss = tf.keras.metrics.Mean(name='test_loss')
        test_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name='test_accuracy')

        @tf.function
        def train_step(examples, labels):
            with tf.GradientTape() as tape:
                # training=True is only needed if there are layers with different
                # behavior during training versus inference (e.g. Dropout).
                predictions = model(examples, training=True)
                loss = loss_object(labels, predictions)
            gradients = tape.gradient(loss, model.trainable_variables)
            optimizer.apply_gradients(zip(gradients, model.trainable_variables))

            train_loss(loss)
            train_accuracy(labels, predictions)

        @tf.function
        def test_step(examples, labels):
            # training=False is only needed if there are layers with different
            # behavior during training versus inference (e.g. Dropout).
            predictions = model(examples, training=False)
            t_loss = loss_object(labels, predictions)

            test_loss(t_loss)
            test_accuracy(labels, predictions)

        EPOCHS = 5

        for epoch in range(EPOCHS):
            # Reset the metrics at the start of the next epoch
            train_loss.reset_states()
            train_accuracy.reset_states()
            test_loss.reset_states()
            test_accuracy.reset_states()

            for features, labels  in ds.train:
                train_step(features, labels)

            for test_features, test_labels in ds.test:
                test_step(test_features, test_labels)

            template = 'Epoch {}, Loss: {}, Accuracy: {}, Test Loss: {}, Test Accuracy: {}'
            print(template.format(epoch + 1,
                                  train_loss.result(),
                                  train_accuracy.result() * 100,
                                  test_loss.result(),
                                  test_accuracy.result() * 100))

    def test_training_2(self):
        game_state = create_test_game_state()
        translator = GameStateTranslater()
        input = translator.convert_to_nn_input('pkmiec_1', [game_state])

        cutoff = floor(len(input.examples) * .60)

        def to_dataset(examples: List[NeuralNetworkExample], dir: Direction):
            features = seq(range(len(examples[0].features)))\
                        .map(lambda colNum: (f'pos_{colNum}', [e.features[colNum] for e in examples]))\
                        .to_dict()
            #features : List[Dict[str,int]] = seq(examples)\
            #            .map(lambda ex: \
            #                seq(enumerate(ex.features)).map(lambda t: (f'pos_{t[0]}', t[1].value)).to_dict())\
            #            .to_list()

            labels = [ex.label == dir for ex in examples]
            ds = tf.data.Dataset.from_tensor_slices((features, labels))
            return features, labels, ds

        def encode_integer_categorical_feature(feature, name, dataset):
            print('create '+ name)
            # Create a CategoryEncoding for our integer indices
            encoder = CategoryEncoding(output_mode="binary")

            # Prepare a Dataset that only yields our feature
            feature_ds = dataset.map(lambda x, y: x[name])
            feature_ds = feature_ds.map(lambda x: tf.expand_dims(x, -1))

            # Learn the space of possible indices
            encoder.adapt(feature_ds)

            # Apply one-hot encoding to our indices
            encoded_feature = encoder(feature)
            return encoded_feature

        #for d in Direction.__members__:
        d = Direction.SOUTH
        training_features, training_labels, train_ds = to_dataset(input.examples[0: cutoff], d)
        test_features, test_labels, test_ds = to_dataset(input.examples[cutoff:], d)

        train_ds = train_ds.batch(32)
        test_ds = test_ds.batch(32)

        all_inputs = [(key, keras.Input(shape=(1,), name=key, dtype="int64")) for key in training_features.keys()]
        print('all_features_before')
        all_features = layers.concatenate([encode_integer_categorical_feature(feature, key, train_ds) for key, feature in all_inputs])
        print('all_features_after')
        x = layers.Dense(32, activation="relu")(all_features)
        x = layers.Dropout(0.5)(x)
        output = layers.Dense(1, activation="sigmoid")(x)
        model = keras.Model([feature for key, feature in all_inputs], output)
        model.compile("adam", "binary_crossentropy", metrics=["accuracy"])
        keras.utils.plot_model(model, show_shapes=True, rankdir="LR")
        #model.fit(train_ds, epochs=50, validation_data=val_ds)
