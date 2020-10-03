from typing import Union


class ModelHyperParameter:
    def __init__(self, name: str, default_value: Union[float, int], is_active: bool):
        self.name = name
        self.default_value = default_value
        self.is_active = is_active
