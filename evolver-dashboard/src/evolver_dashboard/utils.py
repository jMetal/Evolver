import importlib
import warnings


def is_installed(module: str) -> bool:
    """Check if a module is installed.

    Args:
        module (str): Name of the module to check.

    Returns:
        bool: True if the module is installed, False otherwise.
    """
    if "." in module:
        warnings.warn(f"Parent module will be imported: {module}", RuntimeWarning)
    return importlib.util.find_spec(module) is not None
