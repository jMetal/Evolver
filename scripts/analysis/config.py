"""Shared configuration for convergence analysis figures."""

from __future__ import annotations

import os
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

REFERENCE_RESULTS_ROOT = Path(
    os.environ.get(
        "EVOLVER_REFERENCE_RESULTS_ROOT",
        "C:/Users/nicor/Desktop/Results_Evolver",
    )
)
EXTREME_POINTS_RESULTS_ROOT = Path(
    os.environ.get(
        "EVOLVER_EXTREME_POINTS_RESULTS_ROOT",
        "C:/Users/nicor/Desktop/2026.03.11.results",
    )
)
RESULTS_ROOT: Path | None = None
ANALYSIS_DIR = Path(__file__).resolve().parent
FIGURES_DIR = ANALYSIS_DIR / "figures"
TABLES_DIR = ANALYSIS_DIR / "tables"
FIGURES_DIR.mkdir(parents=True, exist_ok=True)
TABLES_DIR.mkdir(parents=True, exist_ok=True)

FAMILIES = ["RE3D", "RWA3D"]
FRONT_TYPES = ["referenceFronts", "extremePointsFronts"]
BUDGETS = [1000, 3000, 5000, 7000]

FRONT_TYPE_RESULTS_ROOTS = {
    "referenceFronts": REFERENCE_RESULTS_ROOT,
    "extremePointsFronts": EXTREME_POINTS_RESULTS_ROOT,
}

FRONT_TYPE_LABELS = {
    "referenceFronts": "Reference fronts",
    "extremePointsFronts": "Extreme-point fronts",
}

FRONT_TYPE_COLORS = {
    "referenceFronts": "#1b9e77",
    "extremePointsFronts": "#d95f02",
}

FRONT_TYPE_LINESTYLES = {
    "referenceFronts": "-",
    "extremePointsFronts": "--",
}

INDICATOR_SPECS = [
    ("EP", "EP (lower is better)"),
    ("HVMinus", "HV$^-$ (lower is better)"),
]

CATEGORICAL_DECODINGS = {
    "algorithmResult": {
        0.0: "population",
        1.0: "externalArchive",
    },
    "archiveType": {
        0.0: "crowdingDistanceArchive",
        1.0: "unboundedArchive",
    },
    "createInitialSolutions": {
        0.0: "default",
        1.0: "latinHypercubeSampling",
        2.0: "scatterSearch",
        3.0: "sobol",
        4.0: "cauchy",
        5.0: "oppositionBased",
    },
    "offspringPopulationSize": {
        0.0: "1",
        1.0: "5",
        2.0: "10",
        3.0: "20",
        4.0: "50",
        5.0: "100",
        6.0: "200",
        7.0: "400",
    },
    "variation": {
        0.0: "crossoverAndMutationVariation",
    },
    "crossover": {
        0.0: "SBX",
        1.0: "BLX-alpha",
        2.0: "wholeArithmetic",
        3.0: "BLX-alpha-beta",
        4.0: "arithmetic",
        5.0: "laplace",
        6.0: "fuzzyRecombination",
        7.0: "PCX",
        8.0: "UNDC",
    },
    "crossoverRepairStrategy": {
        0.0: "random",
        1.0: "round",
        2.0: "bounds",
    },
    "mutation": {
        0.0: "uniform",
        1.0: "polynomial",
        2.0: "linkedPolynomial",
        3.0: "nonUniform",
        4.0: "levyFlight",
        5.0: "powerLaw",
    },
    "mutationRepairStrategy": {
        0.0: "random",
        1.0: "round",
        2.0: "bounds",
    },
    "selection": {
        0.0: "tournament",
        1.0: "random",
        2.0: "boltzmann",
        3.0: "ranking",
        4.0: "stochasticUniversalSampling",
    },
}

CATEGORICAL_COLUMNS = list(CATEGORICAL_DECODINGS.keys())

TOP_PARAMETERS = [
    "populationSizeWithArchive",
    "powerLawMutationDelta",
    "mutationProbabilityFactor",
    "crossoverProbability",
]

NUMERICAL_COLUMNS = [
    "populationSizeWithArchive",
    "crossoverProbability",
    "sbxDistributionIndex",
    "blxAlphaCrossoverAlpha",
    "blxAlphaBetaCrossoverBeta",
    "blxAlphaBetaCrossoverAlpha",
    "laplaceCrossoverScale",
    "fuzzyRecombinationCrossoverAlpha",
    "pcxCrossoverZeta",
    "pcxCrossoverEta",
    "undcCrossoverZeta",
    "undcCrossoverEta",
    "mutationProbabilityFactor",
    "uniformMutationPerturbation",
    "polynomialMutationDistributionIndex",
    "linkedPolynomialMutationDistributionIndex",
    "nonUniformMutationPerturbation",
    "levyFlightMutationBeta",
    "levyFlightMutationStepSize",
    "powerLawMutationDelta",
    "selectionTournamentSize",
    "boltzmannTemperature",
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
    output_path.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(output_path)
    plt.close(fig)
    return output_path


def resolve_results_root(front_type: str, results_root: Path | None = None) -> Path:
    """Resolve the results root for a front type, honoring an optional override."""
    if results_root is not None:
        return Path(results_root)
    return FRONT_TYPE_RESULTS_ROOTS[front_type]
