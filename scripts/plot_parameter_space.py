#!/usr/bin/env python3
"""
Print or plot a parameter-space YAML.

Default mode outputs a text tree to stdout (ready to paste into a slide or
document).  Pass --figure to generate a compact matplotlib table instead.

Usage:
    python scripts/plot_parameter_space.py <yaml_file>
        [--title TITLE] [--depth N] [--output FILE] [--figure]

Arguments:
    yaml_file   Path to a parameter-space YAML file
                (e.g. src/main/resources/parameterSpaces/NSGAIIDouble.yaml)
    --title     Title shown above the tree or figure  (default: YAML stem)
    --depth     Maximum nesting depth to show  (default: 1)
    --output    Output path; .pdf/.png/.svg imply --figure, otherwise plain text
    --figure    Render a matplotlib table instead of printing text

Examples:
    python scripts/plot_parameter_space.py \\
        src/main/resources/parameterSpaces/NSGAIIDouble.yaml

    python scripts/plot_parameter_space.py \\
        src/main/resources/parameterSpaces/NSGAIIDouble.yaml --depth 2

    python scripts/plot_parameter_space.py \\
        src/main/resources/parameterSpaces/PAESDouble.yaml \\
        --figure --depth 3 --output figures/paes_params.pdf
"""

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

import yaml


# ── shared helpers ────────────────────────────────────────────────────────────

_MAX_VALS = 7


def _summarise(ptype: str, spec: dict) -> str:
    if ptype == "categorical":
        raw = spec.get("values", {})
        names = list(raw.keys()) if isinstance(raw, dict) else [str(v) for v in raw]
        shown = names[:_MAX_VALS]
        tail = f"  (+{len(names) - len(shown)})" if len(names) > _MAX_VALS else ""
        return "  |  ".join(shown) + tail
    if ptype in ("integer", "double"):
        r = spec.get("range", [0, 1])
        return f"[{r[0]}, {r[1]}]"
    return ""


def _load(path: Path) -> dict:
    with open(path) as f:
        return yaml.safe_load(f)


# ── text tree ─────────────────────────────────────────────────────────────────

def _tree_lines(name: str, spec: dict, depth: int, max_depth: int,
                conditional: bool, prefix: str, child_prefix: str) -> list[str]:
    ptype = spec.get("type", "categorical")
    summary = _summarise(ptype, spec)
    cond_mark = " *" if conditional else ""
    type_tag = f"[{ptype}]"
    pad = "  " if summary else ""
    line = f"{prefix}{name}{cond_mark}  {type_tag}{pad}{summary}"
    lines = [line]

    if depth >= max_depth or ptype != "categorical":
        return lines

    raw = spec.get("values", {})
    if not isinstance(raw, dict):
        return lines

    global_children = list(spec.get("globalSubParameters", {}).items())

    seen: set[str] = set()
    cond_children: list[tuple[str, dict]] = []
    for val_spec in raw.values():
        if isinstance(val_spec, dict):
            for sub, sub_spec in val_spec.get("conditionalParameters", {}).items():
                if sub not in seen:
                    cond_children.append((sub, sub_spec))
                    seen.add(sub)

    all_children = (
        [(n, s, False) for n, s in global_children]
        + [(n, s, True) for n, s in cond_children]
    )

    for i, (child_name, child_spec, is_cond) in enumerate(all_children):
        is_last = i == len(all_children) - 1
        conn = "└── " if is_last else "├── "
        grand = "    " if is_last else "│   "
        lines.extend(_tree_lines(
            child_name, child_spec,
            depth + 1, max_depth, is_cond,
            child_prefix + conn,
            child_prefix + grand,
        ))

    return lines


def render_text(data: dict, title: str, max_depth: int, output: Path | None) -> None:
    top = list(data.items())
    all_lines: list[str] = [title, ""]

    for i, (name, spec) in enumerate(top):
        is_last = i == len(top) - 1
        conn = "└── " if is_last else "├── "
        cont = "    " if is_last else "│   "
        all_lines.extend(_tree_lines(name, spec, 0, max_depth, False, conn, cont))

    text = "\n".join(all_lines)
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text)
        print(f"Saved → {output}")
    else:
        print(text)


# ── matplotlib table ──────────────────────────────────────────────────────────

_TYPE_COLOR = {
    "categorical": "#4C72B0",
    "integer":     "#DD8452",
    "double":      "#55A868",
    "binary":      "#C44E52",
}

_X_NAME = 0.01
_X_TYPE = 0.46
_X_VALS = 0.60
_ROW_H  = 0.34
_INDENT = "    "


@dataclass
class Row:
    indent: str
    name: str
    ptype: str
    summary: str
    conditional: bool


def _flatten(name: str, spec: dict, depth: int, max_depth: int,
             conditional: bool) -> list[Row]:
    ptype = spec.get("type", "categorical")
    indent = _INDENT * (depth - 1) + ("↳  " if depth > 0 else "")
    rows = [Row(indent=indent, name=name, ptype=ptype,
                summary=_summarise(ptype, spec).replace("|", "·"),
                conditional=conditional)]

    if depth >= max_depth or ptype != "categorical":
        return rows

    raw = spec.get("values", {})
    if not isinstance(raw, dict):
        return rows

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


def _parse_rows(data: dict, max_depth: int) -> list[Row]:
    rows: list[Row] = []
    for name, spec in data.items():
        rows.extend(_flatten(name, spec, depth=0, max_depth=max_depth, conditional=False))
    return rows


def render_figure(data: dict, title: str, max_depth: int, output: Path | None) -> None:
    import matplotlib.patches as mpatches
    import matplotlib.pyplot as plt
    from matplotlib.patches import FancyBboxPatch

    rows = _parse_rows(data, max_depth)
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

    for text, x, align in [
        ("Parameter",      _X_NAME,        "left"),
        ("Type",           _X_TYPE + 0.05, "center"),
        ("Values / Range", _X_VALS,        "left"),
    ]:
        ax.text(x, 0.55, text, va="center", ha=align,
                fontsize=8.5, fontweight="bold", color="#333333")
    ax.axhline(1.0, color="#AAAAAA", linewidth=1.0, xmin=_X_NAME, xmax=1)

    for i, row in enumerate(rows):
        y = i + 1.5
        if i % 2 == 0:
            ax.add_patch(mpatches.Rectangle(
                (0, y - 0.48), 1, 0.96,
                facecolor="#F6F6F6", edgecolor="none", zorder=1))
        ax.text(_X_NAME, y, row.indent,
                va="center", ha="left", fontsize=8, color="#BBBBBB", zorder=2)
        x_name = _X_NAME + len(row.indent) * 0.0055
        suffix = " *" if row.conditional else ""
        ax.text(x_name, y, row.name + suffix,
                va="center", ha="left", fontsize=8,
                fontstyle="italic" if row.conditional else "normal",
                color="#111111", zorder=2)
        color = _TYPE_COLOR.get(row.ptype, "#888888")
        ax.add_patch(FancyBboxPatch(
            (_X_TYPE, y - 0.30), 0.108, 0.60,
            boxstyle="round,pad=0.01",
            facecolor=color, edgecolor="none", zorder=2, alpha=0.88))
        ax.text(_X_TYPE + 0.054, y, row.ptype,
                va="center", ha="center", fontsize=6.5, color="white", zorder=3)
        ax.text(_X_VALS, y, row.summary,
                va="center", ha="left", fontsize=7.5, color="#333333", zorder=2)

    ax.axhline(n + 1.0, color="#AAAAAA", linewidth=1.0, xmin=_X_NAME, xmax=1)

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

_FIGURE_SUFFIXES = {".pdf", ".png", ".svg", ".jpg", ".jpeg"}


def main() -> None:
    p = argparse.ArgumentParser(
        description="Print or plot an Evolver parameter-space YAML.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("yaml_file", type=Path, help="Parameter-space YAML file")
    p.add_argument("--title",  default=None, help="Title (default: YAML stem)")
    p.add_argument("--depth",  type=int, default=1, help="Max nesting depth (default: 1)")
    p.add_argument("--output", type=Path, default=None,
                   help="Output file (.txt for text tree; .pdf/.png/.svg for figure)")
    p.add_argument("--figure", action="store_true",
                   help="Render a matplotlib table instead of printing text")
    args = p.parse_args()

    path = args.yaml_file.expanduser().resolve()
    if not path.exists():
        print(f"Error: {path} not found", file=sys.stderr)
        sys.exit(1)

    data = _load(path)
    title = args.title or path.stem

    use_figure = args.figure or (
        args.output is not None and args.output.suffix.lower() in _FIGURE_SUFFIXES
    )

    if use_figure:
        render_figure(data, title=title, max_depth=args.depth, output=args.output)
    else:
        render_text(data, title=title, max_depth=args.depth, output=args.output)


if __name__ == "__main__":
    main()
