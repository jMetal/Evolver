#!/usr/bin/env python3
"""
Plot a parameter-space YAML file as a left-to-right tree for presentation slides.

Nodes are colour-coded by parameter type:
  blue   → categorical   orange → integer   green → double

Each node shows the parameter name and a compact summary of its allowed values
or numeric range.  Edges connect parameters to their sub-parameters:
  solid line  → global sub-parameter (applies for every value of the parent)
  dashed line → conditional sub-parameter (applies only for a specific value)

Usage:
    python scripts/plot_parameter_space.py <yaml_file>
        [--title TITLE] [--depth N] [--output FILE]

Arguments:
    yaml_file   Path to a parameter-space YAML file
                (e.g. src/main/resources/parameterSpaces/NSGAIIDouble.yaml)
    --title     Figure title  (default: YAML stem)
    --depth     Maximum tree depth to render  (default: 2)
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
from dataclasses import dataclass, field
from pathlib import Path

import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
from matplotlib.patches import FancyBboxPatch
import yaml


# ── visual constants ──────────────────────────────────────────────────────────

_TYPE_COLOR = {
    "categorical": "#4C72B0",
    "integer":     "#DD8452",
    "double":      "#55A868",
    "binary":      "#C44E52",
    "root":        "#7B4F9E",
}
_FG          = "white"
_EDGE_GLOBAL = "#444444"   # solid: global sub-parameter
_EDGE_COND   = "#BBBBBB"   # dashed: conditional sub-parameter

_NODE_W    = 1.0    # node width  (data units)
_NODE_H    = 0.80   # node height (data units)
_COL_GAP   = 0.30   # horizontal gap between depth levels (data units)
_COL_SCALE = 5.0    # figure-inches per depth column
_ROW_SCALE = 1.05   # figure-inches per leaf row
_MAX_VALS  = 6      # truncate categorical value lists beyond this
_WRAP_AT   = 32     # wrap summary text at this many characters


# ── data model ────────────────────────────────────────────────────────────────

@dataclass
class Node:
    name: str
    ptype: str
    summary: str = ""
    children: list = field(default_factory=list)
    conditional: bool = False
    col: int   = 0
    row: float = 0.0


# ── YAML → Node tree ──────────────────────────────────────────────────────────

def _summarise(ptype: str, spec: dict) -> str:
    if ptype == "categorical":
        raw   = spec.get("values", {})
        names = list(raw.keys()) if isinstance(raw, dict) else [str(v) for v in raw]
        shown = names[:_MAX_VALS]
        tail  = f"  (+{len(names) - len(shown)})" if len(names) > _MAX_VALS else ""
        return " · ".join(shown) + tail
    if ptype in ("integer", "double"):
        r = spec.get("range", [0, 1])
        return f"[{r[0]}, {r[1]}]"
    return ""


def _build(name: str, spec: dict, col: int, max_col: int) -> Node:
    ptype = spec.get("type", "categorical")
    node  = Node(name=name, ptype=ptype, summary=_summarise(ptype, spec), col=col)

    if col >= max_col or ptype != "categorical":
        return node

    raw = spec.get("values", {})
    if not isinstance(raw, dict):
        return node

    # Global sub-parameters: solid edge, apply regardless of value chosen
    for sub, sub_spec in spec.get("globalSubParameters", {}).items():
        child = _build(sub, sub_spec, col + 1, max_col)
        node.children.append(child)

    # Conditional sub-parameters: dashed edge, deduplicated across values
    seen: set[str] = set()
    for val_spec in raw.values():
        if not isinstance(val_spec, dict):
            continue
        for sub, sub_spec in val_spec.get("conditionalParameters", {}).items():
            if sub not in seen:
                child = _build(sub, sub_spec, col + 1, max_col)
                child.conditional = True
                node.children.append(child)
                seen.add(sub)

    return node


def parse(path: Path, max_depth: int, title: str) -> Node:
    with open(path) as f:
        data = yaml.safe_load(f)
    root = Node(name=title, ptype="root", col=0)
    for name, spec in data.items():
        root.children.append(_build(name, spec, col=1, max_col=max_depth))
    return root


# ── layout ────────────────────────────────────────────────────────────────────

def _assign_rows(node: Node, counter: list) -> float:
    """Post-order: leaves get consecutive integers; parents centre over children."""
    if not node.children:
        node.row = float(counter[0])
        counter[0] += 1
    else:
        rows = [_assign_rows(c, counter) for c in node.children]
        node.row = (rows[0] + rows[-1]) / 2
    return node.row


def _max_col_used(node: Node) -> int:
    return node.col if not node.children else max(node.col, *(_max_col_used(c) for c in node.children))


def layout(root: Node) -> tuple[int, int]:
    """Assign row/col to every node. Returns (max_col, n_leaves)."""
    counter = [0]
    _assign_rows(root, counter)
    return _max_col_used(root), counter[0]


# ── rendering ─────────────────────────────────────────────────────────────────

def _col_x(col: int) -> float:
    return col * (_NODE_W + _COL_GAP)


def _textwrap(s: str, width: int) -> str:
    """Break a ' · '-separated list into lines of at most `width` characters."""
    parts = s.split(" · ")
    lines, cur = [], ""
    for part in parts:
        trial = (cur + " · " + part) if cur else part
        if len(trial) > width and cur:
            lines.append(cur)
            cur = part
        else:
            cur = trial
    if cur:
        lines.append(cur)
    return "\n".join(lines)


def _draw_node(ax, node: Node) -> None:
    color = _TYPE_COLOR.get(node.ptype, "#888888")
    cx, cy = _col_x(node.col), node.row
    nw, nh = _NODE_W, _NODE_H

    ax.add_patch(FancyBboxPatch(
        (cx - nw / 2, cy - nh / 2), nw, nh,
        boxstyle="round,pad=0.04",
        linewidth=0.8, edgecolor="white",
        facecolor=color, zorder=3,
    ))

    # y-axis is inverted: lower data-y = visually higher (top of box)
    # name at top, summary at bottom
    name_offset = -nh * 0.15 if node.summary else 0.0
    ax.text(cx, cy + name_offset, node.name,
            ha="center", va="center",
            fontsize=7, fontweight="bold", color=_FG, zorder=4)

    if node.summary:
        ax.text(cx, cy + nh * 0.23, _textwrap(node.summary, _WRAP_AT),
                ha="center", va="center",
                fontsize=5.5, color=_FG, zorder=4)


def _draw_edges(ax, node: Node) -> None:
    for child in node.children:
        x0 = _col_x(node.col)  + _NODE_W / 2
        x1 = _col_x(child.col) - _NODE_W / 2
        xm = (x0 + x1) / 2
        color = _EDGE_COND if child.conditional else _EDGE_GLOBAL
        ls    = (0, (5, 3)) if child.conditional else "solid"
        ax.plot([x0, xm, xm, x1],
                [node.row, node.row, child.row, child.row],
                color=color, linestyle=ls, linewidth=1.0, zorder=2)
        _draw_edges(ax, child)


def _draw_all(ax, node: Node) -> None:
    _draw_node(ax, node)
    for c in node.children:
        _draw_all(ax, c)


def _collect_ptypes(node: Node, out: set) -> None:
    out.add(node.ptype)
    for c in node.children:
        _collect_ptypes(c, out)


def render(root: Node, title: str, output: Path | None) -> None:
    max_col, n_leaves = layout(root)

    fig_w = (max_col + 0.5) * _COL_SCALE
    fig_h = max(4.0, n_leaves * _ROW_SCALE + 0.8)

    fig, ax = plt.subplots(figsize=(fig_w, fig_h))
    fig.patch.set_facecolor("white")
    ax.set_facecolor("white")
    ax.axis("off")
    ax.set_xlim(_col_x(0) - _NODE_W / 2 - 0.05,
                _col_x(max_col) + _NODE_W / 2 + 0.05)
    ax.set_ylim(-_NODE_H / 2 - 0.4,
                (n_leaves - 1) + _NODE_H / 2 + 0.4)
    ax.invert_yaxis()  # first YAML parameter at top, last at bottom

    _draw_edges(ax, root)
    _draw_all(ax, root)

    # Legend below the axes (avoids overlap with nodes)
    present: set = set()
    _collect_ptypes(root, present)
    type_handles = [
        mpatches.Patch(color=c, label=t)
        for t, c in _TYPE_COLOR.items()
        if t in present and t != "root"
    ]
    edge_handles = [
        Line2D([0], [0], color=_EDGE_GLOBAL, lw=1.2,
               label="global sub-parameter"),
        Line2D([0], [0], color=_EDGE_COND, lw=1.2,
               linestyle=(0, (5, 3)), label="conditional sub-parameter"),
    ]
    all_handles = type_handles + edge_handles
    fig.legend(handles=all_handles,
               loc="lower center", bbox_to_anchor=(0.5, 0.0),
               ncol=len(all_handles), fontsize=7,
               framealpha=0.9, edgecolor="#cccccc")

    ax.set_title(title, fontsize=12, fontweight="bold", pad=10)
    plt.tight_layout(rect=[0, 0.06, 1, 1])

    if output:
        plt.savefig(output, dpi=180, bbox_inches="tight")
        print(f"Saved → {output}")
    else:
        plt.show()


# ── CLI ───────────────────────────────────────────────────────────────────────

def main() -> None:
    p = argparse.ArgumentParser(
        description="Plot an Evolver parameter-space YAML as a slide-ready tree.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("yaml_file", type=Path, help="Parameter-space YAML file")
    p.add_argument("--title",  default=None, help="Figure title (default: YAML stem)")
    p.add_argument("--depth",  type=int, default=2, help="Max depth (default: 2)")
    p.add_argument("--output", type=Path, default=None,
                   help="Output file (.pdf / .png / .svg)")
    args = p.parse_args()

    path = args.yaml_file.expanduser().resolve()
    if not path.exists():
        print(f"Error: {path} not found", file=sys.stderr)
        sys.exit(1)

    title = args.title or path.stem
    root  = parse(path, max_depth=args.depth, title=title)
    render(root, title=title, output=args.output)


if __name__ == "__main__":
    main()
