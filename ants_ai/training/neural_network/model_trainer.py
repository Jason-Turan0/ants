from datetime import datetime
from typing import List, Dict, Union

import jsonpickle
import kerastuner as kt
import tensorflow as tf
from ants_ai.training.neural_network.encoders import TrainingDataset
from ants_ai.training.neural_network.run_stats import RunStats
from ants_ai.training.neural_network.model_factory import ModelFactory


class ModelTrainer:

    def discover_model(self, game_state_paths: List[str], mf: ModelFactory):
        discovery_path = f'model_discovery/{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
        max_epochs = 5
        callback = tf.keras.callbacks.EarlyStopping(monitor='val_categorical_accuracy', patience=3)
        tuner = kt.Hyperband(mf.construct_discover_model,
                             objective='val_categorical_accuracy',
                             max_epochs=max_epochs,
                             directory=discovery_path,
                             project_name='ants_ai')
        tds: TrainingDataset = mf.encode_games(game_state_paths)
        print(
            f'Started discovery of {mf.model_name} with {tds.get_training_length()} examples')
        tuner.search(tds.train.features, tds.train.labels, epochs=max_epochs, batch_size=50,
                     validation_data=(tds.cross_val.features, tds.cross_val.labels), callbacks=[callback])
        self.perform_training(mf, {}, tds, discovery_path)
        for best_hps in tuner.get_best_hyperparameters(3):
            self.perform_training(mf, best_hps.values, tds, discovery_path)

    def train_model(self, game_state_paths: List[str], mf: ModelFactory):
        tds: TrainingDataset = mf.encode_games(game_state_paths)
        self.perform_training(mf, {}, tds, '')

    def perform_training(self, mf: ModelFactory,
                         tuned_model_params: Dict[str, Union[int, float]],
                         tds: TrainingDataset,
                         discovery_path: str):
        log_dir = f'logs/fit/{mf.model_name}_{datetime.now().strftime("%Y%m%d-%H%M%S")}'
        run_stats_path = f'{log_dir}/run_stats.json'
        model_weights_path = f'{log_dir}/{mf.model_name}_weights'
        model_checkpoint_callback = tf.keras.callbacks.ModelCheckpoint(
            filepath=model_weights_path,
            save_weights_only=True,
            monitor='val_categorical_accuracy',
            mode='max',
            save_best_only=True)
        callbacks = [
            tf.keras.callbacks.TensorBoard(log_dir=log_dir, histogram_freq=1),
            model_checkpoint_callback]
        epochs = 10
        batch_size = 50
        model = mf.construct_model(tuned_model_params)
        print(f'Started training {mf.model_name} with {tds.get_training_length()} examples')
        fit = model.fit(tds.train.features, tds.train.labels,
                        validation_data=(tds.cross_val.features, tds.cross_val.labels),
                        epochs=epochs,
                        callbacks=callbacks,
                        batch_size=batch_size)
        stats = RunStats(mf.model_name,
                         model,
                         tds,
                         epochs,
                         batch_size,
                         tuned_model_params,
                         mf.get_model_params(tuned_model_params),
                         fit.history,
                         discovery_path)
        with open(run_stats_path, 'w') as stream:
            stream.write(jsonpickle.encode(stats))
        print(run_stats_path)
        print('Finished')
