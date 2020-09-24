class LayerStats:
    def __init__(self, layer_name: str, input_shape: tuple, output_shape: tuple):
        self.layer_name = layer_name
        self.input_shape = input_shape
        self.output_shape = output_shape
