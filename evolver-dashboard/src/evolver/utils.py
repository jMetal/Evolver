import base64
import importlib
import io
import json
import os
import re
import warnings
from zipfile import ZipFile


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
                os.path.relpath(os.path.join(root, file), os.path.join(path, "..")),
            )


def download_link(
    label: str,
    data: str | bytes,
    file_name: str | None = None,
    mime: str | None = None,
) -> str:
    """Generates a link to download the given data, suport file-like object.

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
    else:
        raise RuntimeError("Invalid binary data format: %s" % type(data))

    b64 = base64.b64encode(data_as_bytes).decode()

    dl_link = (
        f'<a class="stDownloadButton" download="{file_name}" '
        f'href="data:{mimetype};base64,{b64}">{label}'
        "</a>"
    )

    return dl_link


def extract_plot(text: str) -> dict:
    """Extract JSON from a string extracted from Evolver logs and generate a vega plot.

    Args:
        text (str): Text to extract JSON from.

    Raises:
        ValueError: If no JSON is found in the text.

    Returns:
        dict: Extracted Vega plot.
    """
    pattern = r"{(.+?)}"
    matches = re.findall(pattern, text)

    if matches:
        json_str = "{" + matches[-1] + "}"
        try:
            plot_data = json.loads(json_str)
        except json.JSONDecodeError:
            raise ValueError("Invalid JSON found in text", text)

        data_values = []
        x_label = plot_data["xAxis"]
        y_label = plot_data["yAxis"]
        evaluations = plot_data["evaluations"]

        for obj1, obj2 in zip(plot_data["xValues"], plot_data["yValues"]):
            data_values.append({x_label: obj1, y_label: obj2})

        # Prepare Vega lite plot
        plot = {
            "data": {"values": data_values},
            "title": {
                "text": f"Front progress of meta-optimizer in {evaluations} evaluations"
            },
            "autosize": {"type": "fit", "resize": True},
            "mark": "point",
            "encoding": {
                "x": {"field": x_label, "type": "quantitative"},
                "y": {"field": y_label, "type": "quantitative"},
            },
        }
        return plot, evaluations
    else:
        raise ValueError("No JSON found in text", text)

def github_logo() -> str:
    """Return the GitHub logo as an SVG.
    
    Returns:
        str: GitHub logo as an SVG.
    """
    return """<svg height="32" aria-hidden="true" viewBox="0 0 16 16" version="1.1" width="32" data-view-component="true">
    <path d="M8 0c4.42 0 8 3.58 8 8a8.013 8.013 0 0 1-5.45 7.59c-.4.08-.55-.17-.55-.38 0-.27.01-1.13.01-2.2 0-.75-.25-1.23-.54-1.48 1.78-.2 3.65-.88 3.65-3.95 0-.88-.31-1.59-.82-2.15.08-.2.36-1.02-.08-2.12 0 0-.67-.22-2.2.82-.64-.18-1.32-.27-2-.27-.68 0-1.36.09-2 .27-1.53-1.03-2.2-.82-2.2-.82-.44 1.1-.16 1.92-.08 2.12-.51.56-.82 1.28-.82 2.15 0 3.06 1.86 3.75 3.64 3.95-.23.2-.44.55-.51 1.07-.46.21-1.61.55-2.33-.66-.15-.24-.6-.83-1.23-.82-.67.01-.27.38.01.53.34.19.73.9.82 1.13.16.45.68 1.31 2.69.94 0 .67.01 1.3.01 1.49 0 .21-.15.45-.55.38A7.995 7.995 0 0 1 0 8c0-4.42 3.58-8 8-8Z"></path>
</svg>"""