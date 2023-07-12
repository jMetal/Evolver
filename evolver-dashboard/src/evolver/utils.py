import base64
import importlib
import io
import os
import warnings
from zipfile import ZipFile

try:
    import openpyxl  # noqa: F401 # needed for pd.to_excel
    import pandas as pd

    HAS_PANDAS = True
except ImportError:
    HAS_PANDAS = False


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

def zip_directory(path: str, zip_handler: ZipFile) -> None:
    """Zip a directory recursively."""
    for root, _, files in os.walk(path):
        for file in files:
            zip_handler.write(
                os.path.join(root, file), 
                os.path.relpath(os.path.join(root, file), os.path.join(path, '..'))
            )

def download_link(
    label: str,
    data: str|bytes,
    file_name: str|None = None,
    mime: str|None = None,
) -> str:
    """Generates a link to download the given data, suport file-like object and pd.DataFrame.

    Args:
        button_text: text show on page.
        data: file-like object or pd.DataFrame.
        download_filename: filename and extension of file. e.g. mydata.csv,

    Raises:
        RuntimeError: when data type is not supported

    Returns:
        the html text for the download link.

    Examples:
        download_button('Click to download data!', your_df, 'YOUR_DF.xlsx')
        download_button('Click to download text!', your_str.encode(), 'YOUR_STRING.txt')
    """

    # inspired by https://gist.github.com/chad-m/6be98ed6cf1c4f17d09b7f6e5ca2978f

    data_as_bytes: bytes
    mimetype = mime
    if isinstance(data, str):
        data_as_bytes = data.encode()
        mimetype = mimetype or "text/plain"
    elif isinstance(data, io.TextIOWrapper):
        string_data = data.read()
        data_as_bytes = string_data.encode()
        mimetype = mimetype or "text/plain"
    # Assume bytes; try methods until we run out.
    elif isinstance(data, bytes):
        data_as_bytes = data
        mimetype = mimetype or "application/octet-stream"
    elif isinstance(data, io.BytesIO):
        data.seek(0)
        data_as_bytes = data.getvalue()
        mimetype = mimetype or "application/octet-stream"
    elif isinstance(data, io.BufferedReader):
        data.seek(0)
        data_as_bytes = data.read()
        mimetype = mimetype or "application/octet-stream"
    elif isinstance(data, io.RawIOBase):
        data.seek(0)
        data_as_bytes = data.read() or b""
        mimetype = mimetype or "application/octet-stream"
    elif HAS_PANDAS and hasattr(data, "to_excel"):
        bio = io.BytesIO()
        data.to_excel(bio)
        bio.seek(0)
        data_as_bytes = bio.read()
        mimetype = mimetype or "application/octet-stream"
    else:
        raise RuntimeError("Invalid binary data format: %s" % type(data))

    b64 = base64.b64encode(data_as_bytes).decode()

    dl_link = (
        f'<a class="stDownloadButton" download="{file_name}" '
        f'href="data:{mimetype};base64,{b64}">{label}'
        '</a>'
    )

    return dl_link