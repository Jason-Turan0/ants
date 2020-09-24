from datetime import datetime
from typing import List, Dict, Union

from encoders import TrainingDataset
from functional import seq
from numpy.core.multiarray import ndarray
from tensorflow.python.keras import Model
from ants_ai.training.neural_network.layer_stats import LayerStats


class RunStats:
    def __init__(self, model_name: str, model: Model, ds: TrainingDataset, epochs: int, batch_size: int,
                 tuned_model_params: Dict[str, float], model_params: Dict[str, float], history: Dict,
                 discovery_path: str):
        self.model_name = model_name
        self.layer_shapes: List[LayerStats] = seq(model.layers).map(
            lambda layer: LayerStats(layer.name, layer.input_shape, layer.output_shape)) \
            .to_list()
        test_eval = model.evaluate(ds.test.features, ds.test.labels)
        self.test_loss = test_eval[0]
        self.test_categorical_accuracy = test_eval[1]
        self.train_shape = self.extract_size(ds.train.features)
        self.cross_val_shape = self.extract_size(ds.cross_val.features)
        self.test_shape = self.extract_size(ds.test.features)
        self.epochs = epochs
        self.batch_size = batch_size
        self.tuned_model_params = tuned_model_params
        self.model_params = model_params
        self.history = history
        self.discovery_path = discovery_path
        self.timestamp = datetime.now().timestamp()

    def extract_size(self, item: Union[ndarray, List[ndarray]]) -> List[tuple]:
        if isinstance(item, list):
            return list(map(lambda a: a.shape, item))
        else:
            return [item.shape]
