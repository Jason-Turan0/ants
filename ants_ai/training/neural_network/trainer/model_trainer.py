from datetime import datetime
from typing import List, Dict, Union, Tuple

import os
import jsonpickle
import kerastuner as kt
import tensorflow as tf
from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.sequences.data_structs import DatasetType
from ants_ai.training.neural_network.trainer.run_stats import RunStats
from ants_ai.training.neural_network.factories.model_factory import ModelFactory
from tensorflow.python.keras.callbacks import LambdaCallback
from tensorflow_core.python.keras import Model


class ModelTrainer:

    def __init__(self, data_path: str):
        self.batch_size = 50
        self.data_path = data_path

    def discover_model(self, game_state_paths: List[str], mf: ModelFactory):
        discovery_path = os.path.join(self.data_path, 'model_discovery',
                                      f'{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}')
        max_epochs = 5
        callback = tf.keras.callbacks.EarlyStopping(monitor='val_categorical_accuracy', patience=3)
        tuner = kt.Hyperband(mf.construct_discover_model,
                             objective='val_categorical_accuracy',
                             max_epochs=max_epochs,
                             directory=discovery_path,
                             project_name='ants_ai')

    def train_model(self, game_state_paths: List[str], mf: ModelFactory) -> Tuple[Model, RunStats]:
        seq = mf.create_sequence(game_state_paths, self.batch_size)
        seq.build_indexes(False)
        return self.perform_training(mf, {}, seq, '')

    def perform_training(self, mf: ModelFactory,
                         tuned_model_params: Dict[str, Union[int, float]],
                         seq: FileSystemSequence,
                         discovery_path: str) -> Tuple[Model, RunStats]:
        log_dir = os.path.join(self.data_path, f'logs{os.sep}fit{os.sep}',
                               f'{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}')
        os.makedirs(log_dir)
        run_stats_path = os.path.join(log_dir, 'run_stats.json')
        model_weights_path = os.path.join(log_dir, f'{mf.model_name}_weights')
        model_path = os.path.join(log_dir, 'model')
        file_writer = tf.summary.create_file_writer(os.path.join(log_dir, 'validation'))
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
                print(f'Saving model weights on epoch {epoch} to {model_path}')
                model.save_weights(model_weights_path)
                model.save(model_path.replace('/', '\\'), save_format='h5')
            val_cat_acc.append(results[1])

        callbacks = [
            LambdaCallback(on_epoch_end=record_validation),
            tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1),
        ]
        print(f'Started training {mf.model_name} with {seq.get_training_range()[1]} examples')
        fit = model.fit(seq,
                        epochs=epochs,
                        callbacks=callbacks
                        )
        stats = RunStats(mf.model_name,
                         model,
                         seq,
                         epochs,
                         self.batch_size,
                         tuned_model_params,
                         mf.get_model_params(tuned_model_params),
                         fit.history,
                         discovery_path,
                         model_weights_path)
        with open(run_stats_path, 'w') as stream:
            stream.write(jsonpickle.encode(stats))
        print(run_stats_path)
        print('Finished')
        return model, stats
