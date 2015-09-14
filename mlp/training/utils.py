import os
import errno


def get_bytes_from_file(filename):
    """Read a file into a byte array"""
    return open(filename, "rb").read()


def remove_if_exists(filename):
    """Makes sure a file at a location is writable.

    Checks if file at the location exists. Deletes it if it is there and ensures all parent directories are present."""
    if os.path.exists(filename):
        os.remove(filename)
    try:
        os.makedirs(os.path.dirname(filename))
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(os.path.dirname(filename)):
            pass
        else:
            raise
