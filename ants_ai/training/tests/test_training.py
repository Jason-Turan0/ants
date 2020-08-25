from math import floor

import tensorflow as tf
from tensorflow.keras.layers import Dense, InputLayer
from tensorflow.keras import Model
import unittest

from training.game_state.game_map import Direction
from training.neural_network.game_state_translator import PositionState, TRAINING_VIEW_SIZE, GameStateTranslater
from training.tests.test_utils import create_test_game_state

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


class TestTraining(unittest.TestCase):
    def test_training(self):
        model = AntsModel()
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
