"""SMAC3 as the configurator of an Evolver algorithm, evaluating on Evolver's evaluation server.

SMAC3 (https://github.com/automl/SMAC3) proposes configurations of a base-level algorithm, and the
evaluation server (``org.uma.evolver.cli.evaluation.EvaluationServerMain``) evaluates them as a
training of Evolver does: the problems, budget, runs and quality indicators of a base-level file,
in one Java virtual machine (tutorial "Tuning with SMAC3").

* The configuration space is written by Evolver's ``ConfigSpaceGenerator`` (the twin of the
  generator of irace's parameter file) from the parameter space of the base-level file.
* The instances of SMAC3 are the problems of the server; a trial is one run of a configuration on
  a problem, as an experiment of irace.
* The objectives are the quality indicators of the base-level file (for instance EP and NHV), handled
  by ParEGO; SMAC3 returns the configurations of the Pareto front of what it evaluated.
* It starts from a random design of 50 configurations.

The space, the problems and the indicators are read from the server (``GET /status``), so they are
those of the base-level file it was started with. Run from the root of the Evolver repository, with
a server running (``EvaluationServerMain start <baseLevel.yaml>``):

    python scripts/smac/evolver_smac.py --jar target/Evolver-<version>-jar-with-dependencies.jar \\
        --trials 2000 --workers 8 --output-dir results/tutorial-smac

SMAC3's optimizer is sequential and refits its model on the whole history every ``--retrain-after``
trials (8, SMAC3's default), so the time per trial grows with the trials even when the runs are
cheap; a larger value refits less often, at the cost of a less up-to-date model.
"""

import argparse
import json
import subprocess
import urllib.error
import urllib.request
from pathlib import Path

from ConfigSpace import ConfigurationSpace

CONFIG_SPACE_GENERATOR = "org.uma.evolver.smac.ConfigSpaceGenerator"
DEFAULT_SERVER = "http://127.0.0.1:8765"


# ---- 1. the parameter space ------------------------------------------------------------------
def config_space(jar: Path, space_file: str, encoding: str, json_file: Path,
                 seed: int = 0) -> ConfigurationSpace:
    """The configuration space of an Evolver parameter space, written by Evolver's own generator."""
    command = ["java", "-cp", str(jar), CONFIG_SPACE_GENERATOR, space_file, encoding]
    json_file.write_text(subprocess.run(command, capture_output=True, text=True, check=True).stdout)
    space = ConfigurationSpace.from_json(json_file)
    space.seed(seed)
    return space


def configuration_to_arguments(configuration) -> str:
    """The Evolver configuration string of a configuration: only the active parameters."""
    return " ".join(f"--{name} {value}" for name, value in dict(configuration).items())


def server_status(server: str = DEFAULT_SERVER, timeout: float = 10.0) -> dict:
    """What the server evaluates (``GET /status``): its parameter space, encoding, problems and
    indicators, among others."""
    with urllib.request.urlopen(server.rstrip("/") + "/status", timeout=timeout) as response:
        return json.load(response)


# ---- 2. the target function --------------------------------------------------------------------
class EvaluationServerTarget:
    """Evaluates a configuration on a problem, on Evolver's evaluation server."""

    def __init__(self, server: str = DEFAULT_SERVER, objectives: list[str] = ("EP", "NHV"),
                 tool: str = "smac", timeout: float = 600.0):
        self.server, self.tool, self.timeout = server.rstrip("/"), tool, timeout
        self.objectives = list(objectives)

    def evaluate(self, configuration: str, problem: str, seed: int = 0) -> dict[str, float]:
        """The indicators of a configuration on a problem.

        Raises:
            RuntimeError: if the configuration fails (the server answers 422): SMAC3 records the
                trial as a crash.
        """
        body = json.dumps({"configuration": configuration, "problem": problem,
                           "tool": self.tool, "seed": seed}).encode()
        request = urllib.request.Request(self.server + "/evaluate", data=body, method="POST",
                                         headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                result = json.load(response)
        except urllib.error.HTTPError as error:
            raise RuntimeError(f"{error.code}: {error.read().decode()[:300]}") from error
        return {name: float(result[name]) for name in self.objectives}

    def train(self, config, instance: str, seed: int = 0) -> dict[str, float]:
        """The target function of SMAC3: a configuration, an instance (a problem) and a seed."""
        return self.evaluate(configuration_to_arguments(config), instance, seed)


# ---- 3. the scenario ----------------------------------------------------------------------------
def optimize(space: ConfigurationSpace, target: EvaluationServerTarget, problems: list[str],
             trials: int, workers: int, seed: int, output_dir: Path,
             initial_configurations: int = 50, walltime: float = float("inf"),
             retrain_after: int = 8):
    """Runs SMAC3 and returns it (``smac.runhistory``, ``smac.intensifier.get_incumbents()``)."""
    from smac import AlgorithmConfigurationFacade, Scenario
    from smac.initial_design import RandomInitialDesign
    from smac.multi_objective.parego import ParEGO

    scenario = Scenario(
        space,
        name="evolver",
        output_directory=output_dir / "smac3_output",
        objectives=target.objectives,
        deterministic=False,
        n_trials=trials,
        walltime_limit=walltime,
        instances=problems,
        instance_features={p: [float(i == j) for j in range(len(problems))]
                           for i, p in enumerate(problems)},
        seed=seed,
        n_workers=workers,
    )
    smac = AlgorithmConfigurationFacade(
        scenario,
        target.train,
        # The facade reduces the instance features with a PCA from 4 features on, and SMAC3 2.4.1
        # then predicts with the original ones (an assertion fails once a model is trained): no PCA.
        model=AlgorithmConfigurationFacade.get_model(scenario, pca_components=None),
        initial_design=RandomInitialDesign(scenario, n_configs=initial_configurations),
        multi_objective_algorithm=ParEGO(scenario),
        config_selector=AlgorithmConfigurationFacade.get_config_selector(
            scenario, retrain_after=retrain_after),
        overwrite=True,
        logging_level=30,
    )
    smac.optimize()
    return smac


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--jar", type=Path, required=True, help="Evolver's jar, to write the space")
    parser.add_argument("--server", default=DEFAULT_SERVER)
    parser.add_argument("--trials", type=int, default=2000, help="runs of a configuration on a problem")
    parser.add_argument("--workers", type=int, default=8, help="trials evaluated at the same time")
    parser.add_argument("--initial-configurations", type=int, default=50)
    parser.add_argument("--walltime", type=float, default=float("inf"), help="seconds")
    parser.add_argument("--retrain-after", type=int, default=8,
                        help="trials between two fits of SMAC3's model (8 is SMAC3's default)")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--output-dir", type=Path, default=Path("results/smac"))
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    status = server_status(args.server)
    space = config_space(args.jar, status["parameterSpace"], status["encoding"],
                         args.output_dir / "configspace.json", seed=args.seed)
    target = EvaluationServerTarget(args.server, objectives=status["indicators"])
    smac = optimize(space, target, status["problems"], args.trials, args.workers, args.seed,
                    args.output_dir, args.initial_configurations, args.walltime,
                    args.retrain_after)
    incumbents = smac.intensifier.get_incumbents()
    print(f"{len(incumbents)} configurations on the Pareto front of the trials")
    for configuration in incumbents:
        print(configuration_to_arguments(configuration))


if __name__ == "__main__":
    main()
