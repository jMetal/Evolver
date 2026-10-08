"""Summary of the independent replications of a training, and the input to compare two studies.

A study is a directory with one training output directory per replication (``run01``, ``run02``,
..., as written by ``TrainingRunnerMain --output-dir``, tutorial E18). For each replication, the
script reads the front of the last checkpoint of its VAR_CONF.txt (the configurations the training
returns, with their meta-objectives) and keeps:

* the best (lowest: every meta-objective is minimized) value of each meta-objective on that front;
* the configuration with the best value of the primary meta-objective, the one to validate.

It writes, in the output directory:

* ``replicas.csv``: a row per replication (study, replication, meta-evaluations, minutes, size of
  the front and the best value of each meta-objective); the median and interquartile range of each
  study are printed;
* ``configurations/<study>/runNN.txt``: the configuration chosen in each replication, in the format
  of ``ConfigurationFileReader`` (``ValidationTutorial`` takes such a file, tutorial E8);
* ``QualityIndicatorSummary.csv``: the same values in the format of jMetal's validation studies
  (Algorithm = study, Problem = ``Training``, ExecutionId = replication), so that two studies (flat
  and tree encodings, two meta-optimizers, ...) are compared with ``wilcoxon_pivot_tables.py`` and
  ``boxplots.py`` over their replications.

    python scripts/training_replicas.py results/tutorial-replications/flat --primary NHV
    python scripts/training_replicas.py results/tutorial-replications/flat \\
        results/tutorial-replications/tree --primary NHV \\
        --output-dir results/tutorial-replications/analysis
"""

import argparse
import re
from pathlib import Path

import pandas as pd

from training_time import checkpoint_minutes

_EVALUATION = re.compile(r"^# Evaluation: (\d+)\s*$")
_SOLUTION = re.compile(r"^(?P<values>(?:\S+=\S+\s+)+)\|\s*(?P<configuration>.*)$")


def last_front(var_conf_file: Path) -> tuple[int, list[tuple[dict[str, float], str]]]:
    """The meta-evaluations and the front (meta-objective values and configuration of each
    solution) of the last checkpoint of a VAR_CONF.txt file. The final checkpoint may be written
    twice; the last block wins."""
    evaluation, front = None, []
    for line in var_conf_file.read_text().splitlines():
        match = _EVALUATION.match(line)
        if match:
            evaluation, front = int(match.group(1)), []
            continue
        match = _SOLUTION.match(line.strip())
        if match and evaluation is not None:
            values = dict(pair.split("=", 1) for pair in match.group("values").split())
            front.append(({name: float(value) for name, value in values.items()},
                          match.group("configuration").strip()))
    if evaluation is None or not front:
        raise SystemExit(f"{var_conf_file} has no checkpoint with configurations")
    return evaluation, front


def replications(study: Path) -> list[Path]:
    """The training output directories of a study: its subdirectories with a VAR_CONF.txt."""
    runs = sorted(p for p in study.iterdir() if p.is_dir() and (p / "VAR_CONF.txt").exists())
    if not runs:
        raise SystemExit(f"No replication (a subdirectory with VAR_CONF.txt) in {study}")
    return runs


def summarize(study: Path, label: str, primary: str) -> tuple[pd.DataFrame, dict[str, str]]:
    """A row per replication of a study, and the configuration chosen in each one."""
    rows, chosen = [], {}
    for run in replications(study):
        evaluation, front = last_front(run / "VAR_CONF.txt")
        names = list(front[0][0])
        if primary not in names:
            raise SystemExit(f"{run}: no meta-objective {primary} (there are {', '.join(names)})")
        best = {name: min(values[name] for values, _ in front) for name in names}
        chosen[run.name] = min(front, key=lambda solution: solution[0][primary])[1]
        rows.append({
            "Study": label,
            "Replication": run.name,
            "Evaluations": evaluation,
            "Minutes": checkpoint_minutes(run / "VAR_CONF.txt").get(evaluation, float("nan")),
            "FrontSize": len(front),
            **best,
        })
    return pd.DataFrame(rows), chosen


def quality_indicator_summary(table: pd.DataFrame) -> pd.DataFrame:
    """The best values of the replications in the format of jMetal's QualityIndicatorSummary.csv."""
    indicators = [c for c in table.columns
                  if c not in ("Study", "Replication", "Evaluations", "Minutes", "FrontSize")]
    long = table.melt(id_vars=["Study", "Replication"], value_vars=indicators,
                      var_name="IndicatorName", value_name="IndicatorValue")
    long["ExecutionId"] = long.groupby(["Study", "IndicatorName"]).cumcount()
    long["Problem"] = "Training"
    return long.rename(columns={"Study": "Algorithm"})[
        ["Algorithm", "Problem", "IndicatorName", "ExecutionId", "IndicatorValue"]]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("studies", type=Path, nargs="+",
                        help="study directories, each with one directory per replication")
    parser.add_argument("--primary", required=True,
                        help="meta-objective that chooses the configuration of each replication")
    parser.add_argument("--labels", default=None,
                        help="comma-separated names of the studies (default: directory names)")
    parser.add_argument("--output-dir", type=Path, default=None,
                        help="default: the study directory (with one study) or required")
    args = parser.parse_args()

    labels = args.labels.split(",") if args.labels else [s.name for s in args.studies]
    if len(labels) != len(args.studies) or len(set(labels)) != len(labels):
        raise SystemExit("--labels needs one distinct name per study")
    if args.output_dir is None:
        if len(args.studies) > 1:
            raise SystemExit("--output-dir is required with more than one study")
        args.output_dir = args.studies[0]
    args.output_dir.mkdir(parents=True, exist_ok=True)

    tables = []
    for study, label in zip(args.studies, labels):
        table, chosen = summarize(study, label, args.primary)
        tables.append(table)
        directory = args.output_dir / "configurations" / label
        directory.mkdir(parents=True, exist_ok=True)
        for run, configuration in chosen.items():
            (directory / f"{run}.txt").write_text(configuration + "\n")
    table = pd.concat(tables, ignore_index=True)
    table.to_csv(args.output_dir / "replicas.csv", index=False)
    quality_indicator_summary(table).to_csv(
        args.output_dir / "QualityIndicatorSummary.csv", index=False)

    indicators = [c for c in table.columns
                  if c not in ("Study", "Replication", "Evaluations", "Minutes", "FrontSize")]
    grouped = table.groupby("Study", sort=False)[indicators]
    summary = pd.concat({"median": grouped.median(),
                         "IQR": grouped.quantile(0.75) - grouped.quantile(0.25)}, axis=1)
    print(f"{len(table)} replications; best value of each meta-objective on the final front")
    print(summary.to_string(float_format=lambda v: f"{v:.4g}"))


if __name__ == "__main__":
    main()
