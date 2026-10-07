"""Runs a batch of Evolver solve requests in parallel and gathers their indicators in one table.

The batch is every algorithm that ships a default configuration (``defaultConfigurations/``) for the
chosen encoding, on every given problem. For each pair the script writes a ``request.yaml``, runs
``SolveRunnerMain`` on it as a process, follows its ``status.yaml`` and, when all have finished,
writes the median of every indicator per algorithm and problem to ``medians.csv`` and prints it.

What the algorithms and the problems of an encoding are comes from Evolver's own manifest
(``DescribeMain``), so the script needs no list of them. It must be run from the root of the Evolver
repository (the reference fronts and the weight vectors are read from ``resources/``), with a jar
built by ``mvn -DskipTests package``.

Example:
    python scripts/cli_batch.py target/Evolver-<version>-jar-with-dependencies.jar \\
        --problems ZDT1,ZDT4,DTLZ2 --evaluations 25000 --runs 10 --processes 8 \\
        --output-dir results/batch
"""

import argparse
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path

import pandas as pd
import yaml

DESCRIBE_MAIN = "org.uma.evolver.cli.training.DescribeMain"
SOLVE_RUNNER_MAIN = "org.uma.evolver.cli.solving.SolveRunnerMain"
REFERENCE_FRONT_DIRECTORIES = ("referenceFronts", "referenceFrontsTSP")
DEFAULT_INDICATORS = ("Epsilon", "NormalizedHypervolume")
WEIGHT_VECTORS_DIRECTORY = "resources/weightVectors"


@dataclass(frozen=True)
class Job:
    """One solve request of the batch: an algorithm on a problem."""

    algorithm: str
    problem: str
    directory: Path

    @property
    def request_file(self) -> Path:
        return self.directory / "request.yaml"

    @property
    def status_file(self) -> Path:
        return self.directory / "status.yaml"

    @property
    def output_directory(self) -> Path:
        return self.directory / "output"


def read_manifest(jar: Path) -> dict:
    """Asks Evolver what it can run: runs ``DescribeMain`` and parses its manifest."""
    result = subprocess.run(
        ["java", "-cp", str(jar), DESCRIBE_MAIN], capture_output=True, text=True, check=True
    )
    return yaml.safe_load(result.stdout)


def default_configuration_file(algorithm: str, encoding: str) -> str:
    """The default configuration of an algorithm and encoding: ``NSGA-II``, ``Double`` gives
    ``NSGAIIDoubleDefault.txt``."""
    return f"{algorithm.replace('-', '')}{encoding}Default.txt"


def space_file(algorithm: str, encoding: str) -> str:
    """The parameter space of an algorithm and encoding: ``NSGAIIDouble.yaml``."""
    return f"{algorithm.replace('-', '')}{encoding}.yaml"


def algorithms_with_default(manifest: dict, encoding: str) -> list[str]:
    """The algorithms of an encoding that have a default configuration, in the manifest's order."""
    defaults = set(manifest["resourceDirectories"]["defaultConfigurations"])
    return [
        entry["name"]
        for entry in manifest["baseAlgorithms"]
        if entry["encoding"] == encoding
        and default_configuration_file(entry["name"], encoding) in defaults
    ]


def problems_of_encoding(manifest: dict, encoding: str) -> set[str]:
    """The names of the registered problems whose solutions have the given encoding."""
    return {entry["name"] for entry in manifest["problemCatalogue"] if entry["encoding"] == encoding}


def reference_front(problem: str, resources: Path = Path("resources")) -> str | None:
    """The reference front file of a problem, relative to the root of the repository, or None."""
    for directory in REFERENCE_FRONT_DIRECTORIES:
        if (resources / directory / f"{problem}.csv").is_file():
            return f"{resources.name}/{directory}/{problem}.csv"
    return None


def build_request(
    entry: dict,
    problem: str,
    front: str | None,
    output_directory: Path,
    evaluations: int,
    runs: int,
    seed: int,
    indicators: list[str],
    population_size: int = 100,
) -> dict:
    """The content of a ``SolveRunnerMain`` request for an algorithm on a problem.

    Args:
        entry: The algorithm's entry of the manifest (``name``, ``encoding``,
            ``requiredExtraConfigKeys``).
        front: Its reference front, or None for no indicators.
    """
    algorithm, encoding = entry["name"], entry["encoding"]
    request = {
        "algorithmName": algorithm,
        "encoding": encoding,
        "populationSize": population_size,
        "yamlParameterSpaceFile": space_file(algorithm, encoding),
        "configurationFile": f"defaultConfigurations/{default_configuration_file(algorithm, encoding)}",
        "problem": problem,
        "maxEvaluations": evaluations,
        "numberOfIndependentRuns": runs,
        "seed": seed,
        "statusFrequency": max(evaluations // 10, 1),
        "outputDirectory": str(output_directory),
    }
    if "weightVectorFilesDirectory" in entry.get("requiredExtraConfigKeys", []):
        request["extraConfig"] = {"weightVectorFilesDirectory": WEIGHT_VECTORS_DIRECTORY}
    if front is not None:
        request["referenceFrontFileName"] = front
        request["indicatorNames"] = indicators
    return request


def make_jobs(
    manifest: dict,
    encoding: str,
    algorithms: list[str] | None,
    problems: list[str],
    output_dir: Path,
    evaluations: int,
    runs: int,
    seed: int,
    indicators: list[str],
) -> list[Job]:
    """Writes the request of every algorithm on every problem and returns the jobs.

    Raises:
        ValueError: if an algorithm has no default configuration for the encoding, or a problem
            is not registered with that encoding.
    """
    available = algorithms_with_default(manifest, encoding)
    chosen = algorithms or available
    missing = [name for name in chosen if name not in available]
    if missing:
        raise ValueError(
            f"No default configuration for {', '.join(missing)} with the {encoding} encoding; "
            f"the algorithms that have one: {', '.join(available)}"
        )
    known = problems_of_encoding(manifest, encoding)
    wrong = [name for name in problems if name not in known]
    if wrong:
        raise ValueError(f"Not {encoding}-encoded problems of Evolver: {', '.join(wrong)}")
    entries = {
        (entry["name"], entry["encoding"]): entry for entry in manifest["baseAlgorithms"]
    }
    jobs = []
    for algorithm in chosen:
        for problem in problems:
            directory = output_dir / f"{algorithm}.{problem}"
            job = Job(algorithm, problem, directory)
            directory.mkdir(parents=True, exist_ok=True)
            request = build_request(
                entries[(algorithm, encoding)],
                problem,
                reference_front(problem),
                job.output_directory,
                evaluations,
                runs,
                seed,
                indicators,
            )
            job.request_file.write_text(yaml.safe_dump(request, sort_keys=False))
            jobs.append(job)
    return jobs


def read_status(job: Job) -> dict:
    """The job's ``status.yaml``, or an empty dict while it is not written (or being replaced)."""
    try:
        return yaml.safe_load(job.status_file.read_text()) or {}
    except (OSError, yaml.YAMLError):
        return {}


def run_job(jar: Path, job: Job) -> str:
    """Runs a job's request as a process and returns its final state (``FINISHED`` or ``FAILED``)."""
    log = job.directory / "runner.log"
    with log.open("w") as output:
        subprocess.run(
            ["java", "-cp", str(jar), SOLVE_RUNNER_MAIN, str(job.request_file), str(job.status_file)],
            stdout=output,
            stderr=subprocess.STDOUT,
            check=False,
        )
    return read_status(job).get("status", "FAILED")


def run_batch(jar: Path, jobs: list[Job], processes: int, report=print) -> dict[Job, str]:
    """Runs the jobs, at most ``processes`` at a time, and reports each one that ends."""
    states: dict[Job, str] = {}
    started = time.monotonic()
    with ThreadPoolExecutor(max_workers=processes) as pool:
        futures = {pool.submit(run_job, jar, job): job for job in jobs}
        for future in as_completed(futures):
            job = futures[future]
            states[job] = future.result()
            elapsed = time.monotonic() - started
            report(
                f"[{len(states)}/{len(jobs)}] {job.algorithm} on {job.problem}: "
                f"{states[job]} ({elapsed:.0f} s)"
            )
    return states


def collect_medians(jobs: list[Job]) -> pd.DataFrame:
    """The median of each indicator over the runs of every finished job.

    Returns:
        A table with a row per algorithm and problem: ``Algorithm``, ``Problem`` and a column per
        indicator (plus ``TimeMs``, the median computing time of a run).
    """
    rows = []
    for job in jobs:
        indicators_file = job.output_directory / "INDICATORS.csv"
        if not indicators_file.is_file():
            continue
        values = pd.read_csv(indicators_file).drop(columns=["Run", "Seed"], errors="ignore")
        row = {"Algorithm": job.algorithm, "Problem": job.problem}
        row.update(values.median().to_dict())
        rows.append(row)
    return pd.DataFrame(rows)


def parse_arguments(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run the default configuration of every algorithm of an encoding on some "
        "problems, in parallel, and tabulate the medians of their indicators."
    )
    parser.add_argument("jar", type=Path, help="Evolver's jar with dependencies")
    parser.add_argument("--problems", required=True, help="comma-separated problem names")
    parser.add_argument("--algorithms", help="comma-separated; default: all with a default")
    parser.add_argument("--encoding", default="Double", choices=["Double", "Binary", "Permutation"])
    parser.add_argument("--evaluations", type=int, default=25000, help="per run")
    parser.add_argument("--runs", type=int, default=10, help="independent runs per request")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--indicators",
        default=",".join(DEFAULT_INDICATORS),
        help="comma-separated indicator names (default: %(default)s)",
    )
    parser.add_argument(
        "--processes",
        type=int,
        default=max((os.cpu_count() or 2) - 2, 1),
        help="requests running at once, one core each (default: cores minus two)",
    )
    parser.add_argument("--output-dir", type=Path, default=Path("results/batch"))
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_arguments(argv)
    manifest = read_manifest(args.jar)
    try:
        jobs = make_jobs(
            manifest,
            args.encoding,
            args.algorithms.split(",") if args.algorithms else None,
            args.problems.split(","),
            args.output_dir,
            args.evaluations,
            args.runs,
            args.seed,
            args.indicators.split(","),
        )
    except ValueError as error:
        print(error, file=sys.stderr)
        return 2
    states = run_batch(args.jar, jobs, args.processes)
    medians = collect_medians(jobs)
    medians.to_csv(args.output_dir / "medians.csv", index=False)
    print(medians.to_string(index=False))
    failed = [job for job, state in states.items() if state != "FINISHED"]
    for job in failed:
        print(f"FAILED: {job.algorithm} on {job.problem}, see {job.directory / 'runner.log'}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
