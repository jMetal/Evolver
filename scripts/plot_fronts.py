"""Plot several bi-objective fronts, each with its own label, against a reference front.

Useful to compare the fronts obtained by different configurations of an algorithm on the same
problem (for instance, a default configuration and one found by meta-optimization). By default each
front gets its own panel, side by side with shared axes, so that the differences are easy to see;
--mode overlay draws all of them on a single panel instead:

    python scripts/plot_fronts.py resources/referenceFronts/ZDT4.csv \\
        --front "Default=results/tutorial/E3/validation/default/FUN.csv" \\
        --front "Tuned=results/tutorial/E3/validation/tuned/FUN.csv" \\
        --output fronts.png

For a single front, or for three or more objectives, see plot_front.py.
"""

import argparse
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402

COLORS = ["#0B3C6E", "#14A3B8", "#E07A1F", "#7CB342", "#8E44AD"]
MARKERS = ["o", "s", "^", "D", "v"]


def load_front(path: Path) -> np.ndarray:
    """Load a front: one solution per line, comma-separated objective values."""
    front = np.loadtxt(path, delimiter=",", ndmin=2)
    if front.shape[1] != 2:
        raise SystemExit(f"{path}: expected 2 objectives, found {front.shape[1]}")
    return front


def parse_front_argument(argument: str) -> tuple[str, Path]:
    """Split a LABEL=PATH argument."""
    label, separator, path = argument.partition("=")
    if not separator:
        raise SystemExit(f"--front expects LABEL=PATH, got: {argument}")
    return label, Path(path)


def draw_reference(axes, reference: np.ndarray) -> None:
    """Draw the reference front as a thin line, and set up the axes."""
    axes.plot(reference[:, 0], reference[:, 1], color="#8A949E", linewidth=1, label="Reference front")
    axes.set_xlabel("$f_1$")
    axes.set_ylabel("$f_2$")
    axes.grid(alpha=0.3)


def draw_front(axes, front: np.ndarray, label: str, index: int) -> None:
    """Draw one front as a scatter, with the color and marker of its position."""
    axes.scatter(
        front[:, 0],
        front[:, 1],
        s=18,
        color=COLORS[index % len(COLORS)],
        marker=MARKERS[index % len(MARKERS)],
        label=label,
        zorder=3,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("reference", type=Path, help="reference front CSV")
    parser.add_argument(
        "--front", action="append", required=True, help="LABEL=PATH of a FUN.csv (repeatable)"
    )
    parser.add_argument(
        "--mode", choices=["panels", "overlay"], default="panels",
        help="one panel per front (default) or all fronts on a single panel",
    )
    parser.add_argument("--output", type=Path, default=Path("fronts.png"), help="output image")
    parser.add_argument("--title", default=None, help="figure title")
    arguments = parser.parse_args()

    reference = load_front(arguments.reference)
    fronts = [parse_front_argument(argument) for argument in arguments.front]
    if arguments.mode == "overlay":
        figure, axes = plt.subplots(figsize=(7, 5))
        draw_reference(axes, reference)
        for index, (label, path) in enumerate(fronts):
            draw_front(axes, load_front(path), label, index)
        axes.legend()
    else:
        figure, all_axes = plt.subplots(
            1, len(fronts), figsize=(5 * len(fronts), 4.5), sharex=True, sharey=True, squeeze=False
        )
        for index, ((label, path), axes) in enumerate(zip(fronts, all_axes[0], strict=True)):
            draw_reference(axes, reference)
            draw_front(axes, load_front(path), label, index)
            axes.set_title(label)
            axes.legend(loc="upper right")
    if arguments.title:
        figure.suptitle(arguments.title)
    figure.tight_layout()
    figure.savefig(arguments.output, dpi=150)
    print(f"Saved {arguments.output}")


if __name__ == "__main__":
    main()
