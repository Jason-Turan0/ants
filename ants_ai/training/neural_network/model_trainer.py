from datetime import datetime
from typing import List, Dict, Union

import jsonpickle
import kerastuner as kt
import tensorflow as tf
from sequences.ant_vision_sequence import AntVisionSequence
from sequences.data_structs import DatasetType
from ants_ai.training.neural_network.run_stats import RunStats
from ants_ai.training.neural_network.model_factory import ModelFactory
from tensorflow.python.keras.callbacks import LambdaCallback


class ModelTrainer:

    def __init__(self):
        self.batch_size = 50

    def discover_model(self, game_state_paths: List[str], mf: ModelFactory):
        discovery_path = f'model_discovery/{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
        max_epochs = 5
        callback = tf.keras.callbacks.EarlyStopping(monitor='val_categorical_accuracy', patience=3)
        tuner = kt.Hyperband(mf.construct_discover_model,
                             objective='val_categorical_accuracy',
                             max_epochs=max_epochs,
                             directory=discovery_path,
                             project_name='ants_ai')
        # tds: TrainingDataset = mf.encode_games(game_state_paths)
        # print(
        #    f'Started discovery of {mf.model_name} with {tds.get_training_length()} examples')
        # tuner.search(tds.train.features, tds.train.labels, epochs=max_epochs, batch_size=50,
        #             validation_data=(tds.cross_val.features, tds.cross_val.labels), callbacks=[callback])
        # self.perform_training(mf, {}, tds, discovery_path)
        # for best_hps in tuner.get_best_hyperparameters(3):
        #    self.perform_training(mf, best_hps.values, tds, discovery_path)

    def train_model(self, game_state_paths: List[str], mf: ModelFactory):
        seq = mf.create_sequence(game_state_paths, self.batch_size)
        seq.build_indexes()
        self.perform_training(mf, {}, seq, '')

    def perform_training(self, mf: ModelFactory,
                         tuned_model_params: Dict[str, Union[int, float]],
                         seq: AntVisionSequence,
                         discovery_path: str):
        log_dir = f'logs/fit/{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
        run_stats_path = f'{log_dir}/run_stats.json'
        model_weights_path = f'{log_dir}/{mf.model_name}_weights'
        file_writer = tf.summary.create_file_writer(log_dir + "/validation")
        file_writer.set_as_default()
        epochs = 10
        model = mf.construct_model(tuned_model_params)
        val_cat_acc: List[float] = []

        def record_validation(epoch, logs):
            seq.set_dataset_type(DatasetType.CROSS_VAL)
            results = model.evaluate(seq)
            seq.set_dataset_type(DatasetType.TRAINING)
            logs['val_loss'] = results[0]
            logs['val_categorical_accuracy'] = results[1]
            tf.summary.scalar('val_loss', data=results[0], step=epoch)
            tf.summary.scalar('val_categorical_accuracy', data=results[1], step=epoch)

            current_best = max(val_cat_acc) if len(val_cat_acc) > 0 else 0
            if current_best < results[1]:
                print(f'Saving model weights on epoch {epoch}')
                model.save_weights(model_weights_path)
            val_cat_acc.append(results[1])

        callbacks = [
            LambdaCallback(on_epoch_end=record_validation),
            tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1),
        ]
        print(f'Started training {mf.model_name} with {seq.get_training_range()[1]} examples')
        fit = model.fit(seq,
                        epochs=epochs,
                        callbacks=callbacks)
        stats = RunStats(mf.model_name,
                         model,
                         seq,
                         epochs,
                         self.batch_size,
                         tuned_model_params,
                         mf.get_model_params(tuned_model_params),
                         fit.history,
                         discovery_path)
        with open(run_stats_path, 'w') as stream:
            stream.write(jsonpickle.encode(stats))
        print(run_stats_path)
        print('Finished')
