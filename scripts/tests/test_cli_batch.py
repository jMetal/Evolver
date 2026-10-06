"""Tests of the batch of solve requests (cli_batch.py).

Run from the root of the repository: ``python -m pytest scripts/tests``.
"""

import sys
from pathlib import Path

import pandas as pd
import pytest
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import cli_batch as batch  # noqa: E402

MANIFEST = {
    "baseAlgorithms": [
        {"name": "NSGA-II", "encoding": "Double", "requiredExtraConfigKeys": []},
        {"name": "NSGA-II", "encoding": "Permutation", "requiredExtraConfigKeys": []},
        {
            "name": "MOEAD",
            "encoding": "Double",
            "requiredExtraConfigKeys": ["weightVectorFilesDirectory"],
        },
        {"name": "RDEMOEA", "encoding": "Double", "requiredExtraConfigKeys": []},
    ],
    "problemCatalogue": [
        {"name": "ZDT1", "encoding": "Double"},
        {"name": "ZDT4", "encoding": "Double"},
        {"name": "KroAB100TSP", "encoding": "Permutation"},
    ],
    "resourceDirectories": {
        "defaultConfigurations": [
            "NSGAIIDoubleDefault.txt",
            "NSGAIIPermutationDefault.txt",
            "MOEADDoubleDefault.txt",
        ]
    },
}


def make_jobs(tmp_path: Path, algorithms=None, problems=("ZDT1", "ZDT4"), encoding="Double"):
    return batch.make_jobs(
        MANIFEST, encoding, algorithms, list(problems), tmp_path, 1000, 3, 7, ["Epsilon"]
    )


class TestNames:
    def test_should_name_the_default_configuration_and_the_space_without_hyphens(self):
        assert batch.default_configuration_file("NSGA-II", "Double") == "NSGAIIDoubleDefault.txt"
        assert batch.space_file("SMS-EMOA", "Binary") == "SMSEMOABinary.yaml"


class TestAlgorithmsAndProblems:
    def test_should_list_only_the_algorithms_that_have_a_default_configuration(self):
        # Assert: RDEMOEA is registered but ships no default
        assert batch.algorithms_with_default(MANIFEST, "Double") == ["NSGA-II", "MOEAD"]

    def test_should_list_the_problems_of_an_encoding(self):
        assert batch.problems_of_encoding(MANIFEST, "Permutation") == {"KroAB100TSP"}


class TestRequests:
    def test_should_build_a_request_with_the_default_configuration(self, tmp_path: Path):
        # Act
        request = batch.build_request(
            MANIFEST["baseAlgorithms"][0], "ZDT1", "resources/referenceFronts/ZDT1.csv",
            tmp_path / "out", 1000, 3, 7, ["Epsilon"],
        )

        # Assert
        assert request["configurationFile"] == "defaultConfigurations/NSGAIIDoubleDefault.txt"
        assert request["yamlParameterSpaceFile"] == "NSGAIIDouble.yaml"
        assert request["referenceFrontFileName"] == "resources/referenceFronts/ZDT1.csv"
        assert request["indicatorNames"] == ["Epsilon"]
        assert request["statusFrequency"] == 100
        assert "extraConfig" not in request

    def test_should_add_the_weight_vectors_when_the_algorithm_requires_them(self, tmp_path: Path):
        # Act
        request = batch.build_request(
            MANIFEST["baseAlgorithms"][2], "ZDT1", None, tmp_path, 1000, 1, 1, ["Epsilon"]
        )

        # Assert
        assert request["extraConfig"] == {"weightVectorFilesDirectory": "resources/weightVectors"}

    def test_should_ask_for_no_indicators_without_a_reference_front(self, tmp_path: Path):
        # Act
        request = batch.build_request(
            MANIFEST["baseAlgorithms"][0], "ZDT1", None, tmp_path, 1000, 1, 1, ["Epsilon"]
        )

        # Assert
        assert "indicatorNames" not in request
        assert "referenceFrontFileName" not in request


class TestMakeJobs:
    def test_should_write_a_request_for_every_algorithm_and_problem(self, tmp_path: Path):
        # Act
        jobs = make_jobs(tmp_path)

        # Assert
        assert [(job.algorithm, job.problem) for job in jobs] == [
            ("NSGA-II", "ZDT1"),
            ("NSGA-II", "ZDT4"),
            ("MOEAD", "ZDT1"),
            ("MOEAD", "ZDT4"),
        ]
        request = yaml.safe_load(jobs[1].request_file.read_text())
        assert request["problem"] == "ZDT4"
        assert request["outputDirectory"] == str(jobs[1].output_directory)

    def test_should_reject_an_algorithm_without_a_default_configuration(self, tmp_path: Path):
        with pytest.raises(ValueError, match="RDEMOEA"):
            make_jobs(tmp_path, algorithms=["RDEMOEA"])

    def test_should_reject_a_problem_of_another_encoding(self, tmp_path: Path):
        with pytest.raises(ValueError, match="KroAB100TSP"):
            make_jobs(tmp_path, problems=["ZDT1", "KroAB100TSP"])


class TestStatus:
    def test_should_read_an_empty_status_while_the_file_is_missing(self, tmp_path: Path):
        job = batch.Job("NSGA-II", "ZDT1", tmp_path)

        assert batch.read_status(job) == {}

    def test_should_read_the_state_of_a_job(self, tmp_path: Path):
        job = batch.Job("NSGA-II", "ZDT1", tmp_path)
        job.status_file.write_text("status: FINISHED\nevaluationsDone: 10\n")

        assert batch.read_status(job)["status"] == "FINISHED"


class TestCollectMedians:
    def write_indicators(self, job: batch.Job, values: list[tuple[float, float]]):
        job.output_directory.mkdir(parents=True)
        rows = [f"{i + 1},{i + 1},{ep},{time}" for i, (ep, time) in enumerate(values)]
        (job.output_directory / "INDICATORS.csv").write_text(
            "Run,Seed,EP,TimeMs\n" + "\n".join(rows) + "\n"
        )

    def test_should_take_the_median_of_each_indicator_over_the_runs(self, tmp_path: Path):
        # Arrange
        job = batch.Job("NSGA-II", "ZDT1", tmp_path / "a")
        self.write_indicators(job, [(0.3, 100), (0.1, 300), (0.2, 200)])

        # Act
        table = batch.collect_medians([job])

        # Assert
        assert table.to_dict("records") == [
            {"Algorithm": "NSGA-II", "Problem": "ZDT1", "EP": 0.2, "TimeMs": 200.0}
        ]

    def test_should_skip_a_job_that_wrote_no_indicators(self, tmp_path: Path):
        # Arrange
        done = batch.Job("NSGA-II", "ZDT1", tmp_path / "a")
        self.write_indicators(done, [(0.2, 1)])
        failed = batch.Job("MOEAD", "ZDT1", tmp_path / "b")

        # Act
        table = batch.collect_medians([failed, done])

        # Assert
        assert isinstance(table, pd.DataFrame)
        assert list(table["Algorithm"]) == ["NSGA-II"]


class TestReferenceFront:
    def test_should_find_a_front_in_either_directory(self, tmp_path: Path):
        # Arrange
        resources = tmp_path / "resources"
        (resources / "referenceFronts").mkdir(parents=True)
        (resources / "referenceFrontsTSP").mkdir()
        (resources / "referenceFronts" / "ZDT1.csv").write_text("0,1\n")
        (resources / "referenceFrontsTSP" / "KroAB100TSP.csv").write_text("0,1\n")

        # Assert
        assert batch.reference_front("ZDT1", resources) == "resources/referenceFronts/ZDT1.csv"
        assert (
            batch.reference_front("KroAB100TSP", resources)
            == "resources/referenceFrontsTSP/KroAB100TSP.csv"
        )
        assert batch.reference_front("Nothing", resources) is None
