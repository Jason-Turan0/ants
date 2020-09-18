class TestTraining(unittest.TestCase):

    @unittest.skip('Skipped')
    def test_training(self):
        bot_to_emulate = 'memetix_1'
        # examples = create_test_examples(2, bot_to_emulate, ExampleType.ANT_VISION)

        # training_set_sizes = [1, 2, 10, 20, 50, 100, 150, 180]

        # for set_size in training_set_sizes:
        game_states = create_test_game_states(400, bot_to_emulate)
        ms = mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 7)
        self.train_model(game_states, ms)

        models = [
            # mf.create_hybrid_model(0.0005, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.001, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.002, 1, bot_to_emulate, 3),
            # mf.create_hybrid_model(0.0005, 1, bot_to_emulate, 7),
            # mf.create_hybrid_model(0.001, 1, bot_to_emulate, 7),
            # mf.create_hybrid_model(0.002, 1, bot_to_emulate, 7),
            # mf.create_conv_2d_model(0.0005, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.002, 1, bot_to_emulate, 3),
            # mf.create_conv_2d_model(0.0005, 1, bot_to_emulate, 7),

            # mf.create_conv_2d_model(0.002, 1, bot_to_emulate, 7),
        ]

        # models = [
        #    mf.create_dense_2d_model(0.0005, bot_to_emulate),
        #    mf.create_dense_2d_model(0.001, bot_to_emulate),
        #    mf.create_dense_2d_model(0.002, bot_to_emulate),
        #    mf.create_dense_2d_model(0.01, bot_to_emulate),

        #    mf.create_conv_2d_model(0.0005, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.001, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.002, 1, bot_to_emulate),
        #    mf.create_conv_2d_model(0.01, 1, bot_to_emulate),

        #    mf.create_conv_2d_model(0.0005, 2, bot_to_emulate),
        #    mf.create_conv_2d_model(0.001, 2, bot_to_emulate),
        #    mf.create_conv_2d_model(0.002, 2, bot_to_emulate),
        # for model in models:

        #    mf.create_conv_2d_model(0.01, 2, bot_to_emulate),
        #        ]
        # default learning rate 0.001
        # model.evaluate(cv_features, cv_labels, verbose=2)

        # prediction0 = model.predict(numpy.array([train_features[0]]))
        # print(prediction0)
        # print(train_labels[0])

        # prediction1 = model.predict(numpy.array([test_features[0]]))
        # print(prediction1)
        # print(test_labels[0])

    # @unittest.skip('Skipped')
    def test_load_weights(self):
        bot_to_emulate = 'memetix_1'
        weight_path = f'logs/fit/conv_2d_20200913-114036/conv_2d_weights_741e221d-0fa7-4a29-9884-390f771a3007_09'
        save_path = f'neural_network_bot/config/conv_2d_20200913-114036'
        ms = mf.create_conv_2d_model(0.001, 1, bot_to_emulate, 7)
        ms.model.load_weights(weight_path)
        ms.model.save(save_path)
        # game_states = create_test_game_states(1, bot_to_emulate)
        # (train, cross_val) = ms.encode_game_states(game_states)
        # predictions = ms.model.predict(cross_val.features)

        # print(seq(blah).sum(lambda t: 1 if t[2] == True else 0) / len(predictions))
 