from typing import Dict, Union, Callable

from kerastuner import HyperParameters, HyperParameter
from ants_ai.training.neural_network.model_hyper_parameter import ModelHyperParameter


class HyperParameterFactory:
    def __init__(self,
                 default_parameters_values: Dict[str, ModelHyperParameter],
                 tuned_model_params: Dict[str, Union[int, float]],
                 hps: HyperParameters = None
                 ):
        self.default_parameters_values = default_parameters_values
        self.tuned_model_params = tuned_model_params
        self.hps = hps

    def get_hyper_param(self,
                        name: str,
                        param_factory: Callable[[Union[int, float]], HyperParameter]) -> \
            Union[int, float, HyperParameter]:
        if self.hps is None:
            return self.tuned_model_params.get(name) \
                if name in self.tuned_model_params.keys() \
                else self.default_parameters_values[name].default_value
        else:
            config = self.default_parameters_values[name]
            if config.is_active:
                return param_factory(config.default_value)
            else:
                return config.default_value
