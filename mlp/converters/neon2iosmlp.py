import numpy as np
import struct
import cPickle as pkl


def extract_weights(file_name):
    """Load weights from the file_name. Data in the file should be stored neon model."""

    vec = []
    # Load a stored model file from disk (should have extension prm)
    params = pkl.load(open(file_name, 'r'))

    num_linear_layers = len(params["layer_params_states"])

    # Neon layers are LinearLayer_0, BiasLayer_1, LinearLayer_4, ...
    # A linear layer is followed by a bias layer. We need to concat their weights

    for layer_idx in range(0, num_linear_layers, 2):
        # Make sure our model has biases activated, otherwise add zeros here
        b = params["layer_params_states"][layer_idx + 1]['params']
        w = params["layer_params_states"][layer_idx]['params']

        layer_vector = np.ravel(np.hstack((b, w)))
        [vec.append(nv) for nv in layer_vector]
    return vec


def convert(neon_model_path, output_filename):
    """Convert the serialization of a neon model into an iOS MLP model."""

    weights = extract_weights(neon_model_path)
    f = open(output_filename, "wb")

    #  You can use 'd' for double and < or > to force endinness
    bin_data = struct.pack('d' * len(weights), *weights)

    f.write(bin_data)
    f.close()
