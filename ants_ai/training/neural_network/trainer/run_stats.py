from datetime import datetime
from typing import List, Dict

from ants_ai.training.neural_network.sequences.file_system_sequence import FileSystemSequence
from ants_ai.training.neural_network.sequences.data_structs import DatasetType
from functional import seq

from tensorflow.python.keras import Model
from ants_ai.training.neural_network.trainer.layer_stats import LayerStats


class RunStats:
    def __init__(self, model_name: str, model: Model, fs_seq: FileSystemSequence, epochs: int, batch_size: int,
                 tuned_model_params: Dict[str, float], model_params: Dict[str, float], history: Dict,
                 discovery_path: str, weight_path: str):
        self.model_name = model_name
        self.layer_shapes: List[LayerStats] = seq(model.layers).map(
            lambda layer: LayerStats(layer.name, layer.input_shape, layer.output_shape)) \
            .to_list()
        fs_seq.set_dataset_type(DatasetType.TEST)
        test_eval = model.evaluate(fs_seq)
        fs_seq.set_dataset_type(DatasetType.TRAINING)
        self.test_loss = test_eval[0].item()
        self.test_categorical_accuracy = test_eval[1].item()
        self.train_shape = fs_seq.get_train_feature_shape()
        self.cross_val_shape = fs_seq.get_crossval_feature_shape()
        self.test_shape = fs_seq.get_test_feature_shape()
        self.epochs = epochs
        self.batch_size = batch_size
        self.tuned_model_params = tuned_model_params
        self.model_params = model_params
        self.history = {k: seq(history[k]).map(lambda v: v.item()).to_list() for k in history.keys()}
        self.discovery_path = discovery_path
        self.timestamp = datetime.now().timestamp()
        self.weight_path = weight_path
