#!/usr/bin/env python3
"""
Plot a parameter-space YAML as a compact slide-ready table.

Each row shows one parameter: name (indented by depth), a colour-coded type
badge, and the list of allowed values or numeric range.  Sub-parameters are
indented below their parent; conditional ones (active only for a specific
parent value) are marked with *.

Usage:
    python scripts/plot_parameter_space.py <yaml_file>
        [--title TITLE] [--depth N] [--output FILE]

Arguments:
    yaml_file   Path to a parameter-space YAML file
                (e.g. src/main/resources/parameterSpaces/NSGAIIDouble.yaml)
    --title     Figure title  (default: YAML stem)
    --depth     Maximum nesting depth to show  (default: 1)
    --output    Output path (.pdf / .png / .svg); omit to open an interactive window

Examples:
    python scripts/plot_parameter_space.py \\
        src/main/resources/parameterSpaces/NSGAIIDouble.yaml

    python scripts/plot_parameter_space.py \\
        src/main/resources/parameterSpaces/PAESDouble.yaml \\
        --depth 3 --output figures/paes_params.pdf
"""

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
import yaml


# ── visual constants ──────────────────────────────────────────────────────────

_TYPE_COLOR = {
    "categorical": "#4C72B0",
    "integer":     "#DD8452",
    "double":      "#55A868",
    "binary":      "#C44E52",
}

# Normalised x positions for the three columns (0 = left edge, 1 = right edge)
_X_NAME = 0.01
_X_TYPE = 0.46
_X_VALS = 0.60

_ROW_H     = 0.34   # figure-inches per data row
_MAX_VALS  = 7      # truncate categorical value lists beyond this
_INDENT    = "    "  # per-level indent string


# ── data model ────────────────────────────────────────────────────────────────

@dataclass
class Row:
    indent: str        # leading whitespace + connector (e.g. "    ↳ ")
    name: str
    ptype: str
    summary: str
    conditional: bool


# ── YAML → flat list of Row ───────────────────────────────────────────────────

def _summarise(ptype: str, spec: dict) -> str:
    if ptype == "categorical":
        raw   = spec.get("values", {})
        names = list(raw.keys()) if isinstance(raw, dict) else [str(v) for v in raw]
        shown = names[:_MAX_VALS]
        tail  = f"  (+{len(names) - len(shown)})" if len(names) > _MAX_VALS else ""
        return "  ·  ".join(shown) + tail
    if ptype in ("integer", "double"):
        r = spec.get("range", [0, 1])
        return f"[{r[0]},  {r[1]}]"
    return ""


def _flatten(name: str, spec: dict, depth: int, max_depth: int,
             conditional: bool) -> list[Row]:
    ptype   = spec.get("type", "categorical")
    indent  = _INDENT * (depth - 1) + ("↳  " if depth > 0 else "")
    rows    = [Row(indent=indent, name=name, ptype=ptype,
                   summary=_summarise(ptype, spec), conditional=conditional)]

    if depth >= max_depth or ptype != "categorical":
        return rows

    raw = spec.get("values", {})
    if not isinstance(raw, dict):
        return rows

    # global sub-parameters first (solid edge equivalent), then conditional
    for sub, sub_spec in spec.get("globalSubParameters", {}).items():
        rows.extend(_flatten(sub, sub_spec, depth + 1, max_depth, conditional=False))

    seen: set[str] = set()
    for val_spec in raw.values():
        if not isinstance(val_spec, dict):
            continue
        for sub, sub_spec in val_spec.get("conditionalParameters", {}).items():
            if sub not in seen:
                rows.extend(_flatten(sub, sub_spec, depth + 1, max_depth, conditional=True))
                seen.add(sub)

    return rows


def parse(path: Path, max_depth: int) -> list[Row]:
    with open(path) as f:
        data = yaml.safe_load(f)
    rows: list[Row] = []
    for name, spec in data.items():
        rows.extend(_flatten(name, spec, depth=0, max_depth=max_depth, conditional=False))
    return rows


# ── rendering ─────────────────────────────────────────────────────────────────

def render(rows: list[Row], title: str, output: Path | None) -> None:
    n = len(rows)
    fig_w = 14.0
    fig_h = max(2.5, n * _ROW_H + 1.2)

    fig, ax = plt.subplots(figsize=(fig_w, fig_h))
    fig.patch.set_facecolor("white")
    ax.set_facecolor("white")
    ax.axis("off")
    ax.set_xlim(0, 1)
    ax.set_ylim(0, n + 1)
    ax.invert_yaxis()

    # ── column headers ──
    for text, x, align in [
        ("Parameter",      _X_NAME,        "left"),
        ("Type",           _X_TYPE + 0.05, "center"),
        ("Values / Range", _X_VALS,        "left"),
    ]:
        ax.text(x, 0.55, text, va="center", ha=align,
                fontsize=8.5, fontweight="bold", color="#333333")
    ax.axhline(1.0, color="#AAAAAA", linewidth=1.0, xmin=_X_NAME, xmax=1)

    # ── data rows ──
    for i, row in enumerate(rows):
        y = i + 1.5

        # alternating stripe
        if i % 2 == 0:
            ax.add_patch(mpatches.Rectangle(
                (0, y - 0.48), 1, 0.96,
                facecolor="#F6F6F6", edgecolor="none", zorder=1))

        # name: indent in light gray, name in dark (italic if conditional)
        ax.text(_X_NAME, y, row.indent,
                va="center", ha="left", fontsize=8,
                color="#BBBBBB", zorder=2)
        x_name = _X_NAME + len(row.indent) * 0.0055
        suffix = " *" if row.conditional else ""
        ax.text(x_name, y, row.name + suffix,
                va="center", ha="left", fontsize=8,
                fontstyle="italic" if row.conditional else "normal",
                color="#111111", zorder=2)

        # type badge
        color = _TYPE_COLOR.get(row.ptype, "#888888")
        ax.add_patch(FancyBboxPatch(
            (_X_TYPE, y - 0.30), 0.108, 0.60,
            boxstyle="round,pad=0.01",
            facecolor=color, edgecolor="none", zorder=2, alpha=0.88))
        ax.text(_X_TYPE + 0.054, y, row.ptype,
                va="center", ha="center", fontsize=6.5,
                color="white", zorder=3)

        # values
        ax.text(_X_VALS, y, row.summary,
                va="center", ha="left", fontsize=7.5,
                color="#333333", zorder=2)

    # bottom border
    ax.axhline(n + 1.0, color="#AAAAAA", linewidth=1.0, xmin=_X_NAME, xmax=1)

    # ── legend ──
    type_handles = [
        mpatches.Patch(color=c, label=t, alpha=0.88)
        for t, c in _TYPE_COLOR.items()
    ]
    cond_handle = mpatches.Patch(color="none", label="* = conditional sub-parameter")
    fig.legend(handles=type_handles + [cond_handle],
               loc="lower center", bbox_to_anchor=(0.5, 0.0),
               ncol=len(type_handles) + 1, fontsize=7.5,
               framealpha=0.9, edgecolor="#cccccc")

    ax.set_title(title, fontsize=13, fontweight="bold", pad=10)
    plt.tight_layout(rect=(0, 0.05, 1, 1))

    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(output, dpi=180, bbox_inches="tight")
        print(f"Saved → {output}")
    else:
        plt.show()


# ── CLI ───────────────────────────────────────────────────────────────────────

def main() -> None:
    p = argparse.ArgumentParser(
        description="Plot an Evolver parameter-space YAML as a compact table.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("yaml_file", type=Path, help="Parameter-space YAML file")
    p.add_argument("--title",  default=None, help="Figure title (default: YAML stem)")
    p.add_argument("--depth",  type=int, default=1, help="Max depth (default: 1)")
    p.add_argument("--output", type=Path, default=None,
                   help="Output file (.pdf / .png / .svg)")
    args = p.parse_args()

    path = args.yaml_file.expanduser().resolve()
    if not path.exists():
        print(f"Error: {path} not found", file=sys.stderr)
        sys.exit(1)

    title = args.title or path.stem
    rows  = parse(path, max_depth=args.depth)
    render(rows, title=title, output=args.output)


if __name__ == "__main__":
    main()
