"""Proof of concept: SMAC3 as the configurator of an Evolver algorithm, in the way of irace.

Three pieces, as in the irace integration (docs/tutorials/tuning_with_irace.rst):

1. The parameter space: Evolver's ConfigSpaceGenerator writes a YAML parameter space as a ConfigSpace
   configuration space (JSON), with the default configuration of the algorithm as its default.
2. The target function: a configuration of SMAC3 becomes a configuration string of Evolver
   (``--name value`` for the active parameters), which is run with Evolver's ``SolveRunnerMain`` on a
   problem (an instance of SMAC3), with a seed; the cost is a quality indicator of the front found.
3. The scenario: the instances (training problems), the budget (trials or wall-clock time) and the
   facade of SMAC3 for algorithm configuration.

Run from the root of the Evolver repository, with a jar built by ``mvn -DskipTests package``:

    python scripts/smac/evolver_smac.py target/Evolver-2.4-SNAPSHOT-jar-with-dependencies.jar \\
        --problems ZDT1,ZDT4 --evaluations 5000 --trials 20 --output-dir results/smac
"""

import argparse
import subprocess
import tempfile
from pathlib import Path

import yaml
from ConfigSpace import ConfigurationSpace

SOLVE_RUNNER_MAIN = "org.uma.evolver.cli.solving.SolveRunnerMain"


# ---- 1. the parameter space ------------------------------------------------------------------
CONFIG_SPACE_GENERATOR = "org.uma.evolver.smac.ConfigSpaceGenerator"


def config_space(jar: Path, space_file: str, encoding: str, default_configuration: str | None,
                 json_file: Path, seed: int = 0) -> ConfigurationSpace:
    """The ConfigSpace configuration space of an Evolver parameter space, written by Evolver's
    ConfigSpaceGenerator (the twin of the generator of irace's parameter file), so that the
    relations of the parameters are read by Evolver itself and not translated twice."""
    command = ["java", "-cp", str(jar), CONFIG_SPACE_GENERATOR, space_file, encoding]
    if default_configuration:
        command.append(default_configuration)
    json_file.write_text(subprocess.run(command, capture_output=True, text=True, check=True).stdout)
    space = ConfigurationSpace.from_json(json_file)
    space.seed(seed)
    return space


def configuration_to_arguments(configuration) -> str:
    """The Evolver configuration string of a configuration: only the active parameters."""
    return " ".join(f"--{name} {value}" for name, value in dict(configuration).items())


# ---- 2. the target function --------------------------------------------------------------------
class EvolverTarget:
    """Runs a configuration on a problem with Evolver's SolveRunnerMain, and returns an indicator."""

    def __init__(self, jar: Path, space_file: str, evaluations: int, indicator: str,
                 work_dir: Path, algorithm: str = "NSGA-II", encoding: str = "Double",
                 population_size: int = 100):
        self.jar, self.space_file, self.evaluations = jar, space_file, evaluations
        self.indicator, self.work_dir = indicator, work_dir
        self.algorithm, self.encoding, self.population_size = algorithm, encoding, population_size
        self.indicator_names = {"NHV": "NormalizedHypervolume", "EP": "Epsilon",
                                "IGD+": "InvertedGenerationalDistancePlus"}

    def train(self, config, instance: str, seed: int = 0) -> float:
        work_dir = Path(tempfile.mkdtemp(dir=self.work_dir))
        request = {
            "algorithmName": self.algorithm,
            "encoding": self.encoding,
            "populationSize": self.population_size,
            "yamlParameterSpaceFile": self.space_file,
            "configuration": configuration_to_arguments(config),
            "problem": instance,
            "referenceFrontFileName": f"resources/referenceFronts/{instance}.csv",
            "maxEvaluations": self.evaluations,
            "numberOfIndependentRuns": 1,
            "seed": seed,
            "indicatorNames": [self.indicator_names[self.indicator]],
            "outputDirectory": str(work_dir / "output"),
        }
        request_file = work_dir / "request.yaml"
        request_file.write_text(yaml.safe_dump(request, sort_keys=False))
        subprocess.run(
            ["java", "-cp", str(self.jar), SOLVE_RUNNER_MAIN, str(request_file),
             str(work_dir / "status.yaml")],
            capture_output=True, check=True)
        indicators = (work_dir / "output" / "INDICATORS.csv").read_text().splitlines()
        header, row = indicators[0].split(","), indicators[1].split(",")
        return float(row[header.index(self.indicator)])


# ---- 3. the scenario ----------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("jar", type=Path)
    parser.add_argument("--space", default="NSGAIIDouble.yaml", help="bundled name or path")
    parser.add_argument("--encoding", default="Double")
    parser.add_argument("--default-configuration",
                        default="defaultConfigurations/NSGAIIDoubleDefault.txt",
                        help="the first configuration that SMAC3 tries; empty for none")
    parser.add_argument("--problems", default="ZDT1,ZDT4")
    parser.add_argument("--evaluations", type=int, default=5000)
    parser.add_argument("--indicator", default="NHV", choices=["NHV", "EP", "IGD+"])
    parser.add_argument("--trials", type=int, default=20)
    parser.add_argument("--walltime", type=float, default=float("inf"), help="seconds")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--output-dir", type=Path, default=Path("results/smac"))
    args = parser.parse_args()

    from smac import AlgorithmConfigurationFacade, Scenario

    problems = args.problems.split(",")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    space = config_space(args.jar, args.space, args.encoding, args.default_configuration,
                         args.output_dir / "configspace.json", seed=args.seed)
    target = EvolverTarget(args.jar, Path(args.space).name, args.evaluations, args.indicator,
                           args.output_dir)
    scenario = Scenario(
        space,
        name="evolver",
        output_directory=args.output_dir / "smac3_output",
        deterministic=False,
        n_trials=args.trials,
        walltime_limit=args.walltime,
        instances=problems,
        instance_features={p: [float(i == j) for j in range(len(problems))]
                           for i, p in enumerate(problems)},
        seed=args.seed,
        use_default_config=bool(args.default_configuration),
    )
    smac = AlgorithmConfigurationFacade(scenario, target.train, overwrite=True)
    incumbent = smac.optimize()
    print("Incumbent:", configuration_to_arguments(incumbent))
    print("Average cost of the incumbent:", smac.runhistory.average_cost(incumbent))


if __name__ == "__main__":
    main()
