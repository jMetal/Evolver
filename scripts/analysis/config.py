"""Shared configuration for convergence analysis figures."""

from __future__ import annotations

import os
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

RESULTS_ROOT = Path(
    os.environ.get("EVOLVER_RESULTS_ROOT", "C:/Users/nicor/Desktop/Results_Evolver")
)
ANALYSIS_DIR = Path(__file__).resolve().parent
FIGURES_DIR = ANALYSIS_DIR / "figures"
TABLES_DIR = ANALYSIS_DIR / "tables"
FIGURES_DIR.mkdir(parents=True, exist_ok=True)
TABLES_DIR.mkdir(parents=True, exist_ok=True)

FAMILIES = ["RE3D", "RWA3D"]
FRONT_TYPES = ["referenceFronts", "estimatedReferenceFronts"]
BUDGETS = [1000, 3000, 5000, 7000]

FRONT_TYPE_LABELS = {
    "referenceFronts": "Reference fronts",
    "estimatedReferenceFronts": "Estimated fronts",
}

FRONT_TYPE_COLORS = {
    "referenceFronts": "#1b9e77",
    "estimatedReferenceFronts": "#d95f02",
}

FRONT_TYPE_LINESTYLES = {
    "referenceFronts": "-",
    "estimatedReferenceFronts": "--",
}

INDICATOR_SPECS = [
    ("EP", "EP (lower is better)"),
    ("HVMinus", "HV$^-$ (lower is better)"),
]


def setup_style() -> None:
    """Apply a compact, publication-friendly Matplotlib style."""
    plt.rcParams.update(
        {
            "figure.dpi": 300,
            "savefig.dpi": 300,
            "savefig.bbox": "tight",
            "savefig.facecolor": "white",
            "figure.facecolor": "white",
            "font.size": 11,
            "axes.titlesize": 12,
            "axes.labelsize": 11,
            "xtick.labelsize": 9,
            "ytick.labelsize": 9,
            "legend.fontsize": 9,
            "axes.grid": True,
            "grid.alpha": 0.25,
            "grid.linestyle": "--",
            "axes.spines.top": False,
            "axes.spines.right": False,
        }
    )


def save_figure(fig: plt.Figure, filename: str) -> Path:
    """Save a figure under the local figures directory."""
    output_path = FIGURES_DIR / filename
    fig.savefig(output_path)
    plt.close(fig)
    return output_path
