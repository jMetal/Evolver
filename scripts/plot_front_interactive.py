#!/usr/bin/env python3
"""
Interactive Pareto-front viewer (Plotly companion to plot_front.py).

Renders a single front (a FUN.csv produced by a base-level Evolver run) interactively — rotate a 3D
front, zoom, hover for values — optionally overlaying its reference front. The number of objectives
is auto-detected from the column count: 2 -> 2D scatter, 3 -> 3D scatter, more than 3 -> parallel
coordinates.

Use this for manual exploration. For static, headless, report-embeddable figures (the default used
by the skills and the validation reports) use plot_front.py instead.

Usage:
    python scripts/plot_front_interactive.py <FUN.csv> [<referenceFront.csv>] [--output figure.html] [--title TITLE]

With --output the interactive figure is written as a self-contained HTML file; otherwise it is shown
in the browser.
"""

import argparse
import sys
from pathlib import Path

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

FRONT_COLOR = "#1f77b4"
REF_COLOR = "#cccccc"


def load_front(path):
    if path is None or not Path(path).exists():
        return None
    return pd.read_csv(path, header=None)


def figure_2d(front, ref, title):
    fig = go.Figure()
    if ref is not None and len(ref):
        fig.add_trace(go.Scatter(x=ref[0], y=ref[1], mode="markers", name="reference",
                                 marker=dict(size=5, color=REF_COLOR)))
    fig.add_trace(go.Scatter(x=front[0], y=front[1], mode="markers", name="front",
                             marker=dict(size=7, color=FRONT_COLOR)))
    fig.update_layout(title=title, xaxis_title="f1", yaxis_title="f2")
    return fig


def figure_3d(front, ref, title):
    fig = go.Figure()
    if ref is not None and len(ref):
        fig.add_trace(go.Scatter3d(x=ref[0], y=ref[1], z=ref[2], mode="markers", name="reference",
                                   marker=dict(size=3, color=REF_COLOR)))
    fig.add_trace(go.Scatter3d(x=front[0], y=front[1], z=front[2], mode="markers", name="front",
                               marker=dict(size=4, color=FRONT_COLOR)))
    fig.update_layout(title=title,
                      scene=dict(xaxis_title="f1", yaxis_title="f2", zaxis_title="f3"))
    return fig


def figure_parallel(front, title):
    front = front.copy()
    front.columns = [f"f{i + 1}" for i in range(front.shape[1])]
    return px.parallel_coordinates(front, title=title)


def main():
    parser = argparse.ArgumentParser(
        description="Interactive Pareto-front viewer (Plotly companion to plot_front.py)."
    )
    parser.add_argument("front", type=Path, help="FUN.csv with the objective values")
    parser.add_argument("reference", type=Path, nargs="?", default=None,
                        help="reference front CSV (optional)")
    parser.add_argument("--output", type=Path, default=None,
                        help="write a self-contained interactive HTML here instead of showing it")
    parser.add_argument("--title", default=None, help="figure title")

    if len(sys.argv) == 1:
        parser.print_help()
        sys.exit(1)
    args = parser.parse_args()

    if not args.front.exists():
        print(f"ERROR: front file not found: {args.front}", file=sys.stderr)
        sys.exit(1)

    front = load_front(args.front)
    ref = load_front(args.reference)
    title = args.title or args.front.stem
    n_obj = front.shape[1]

    if n_obj == 2:
        fig = figure_2d(front, ref, title)
    elif n_obj == 3:
        fig = figure_3d(front, ref, title)
    else:
        if ref is not None:
            print("note: reference ignored for >3 objectives (parallel coordinates)",
                  file=sys.stderr)
        fig = figure_parallel(front, title)

    if args.output:
        fig.write_html(str(args.output))
        print(f"Saved: {args.output}  ({len(front)} solutions, {n_obj} objectives)")
    else:
        fig.show()


if __name__ == "__main__":
    main()
