"""Tests of the SMAC3 client of the evaluation server (evolver_smac.py), with a stub server.

Run from the root of the repository, in an environment with SMAC3 and pytest:
``python -m pytest scripts/tests/test_evolver_smac.py``.
"""

import json
import sys
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "smac"))

import evolver_smac as client  # noqa: E402
from ConfigSpace import ConfigurationSpace, Float  # noqa: E402


class StubServer:
    """An HTTP server that answers /evaluate like the evaluation server, and records the requests."""

    def __init__(self, status: int = 200):
        self.requests: list[dict] = []
        stub = self

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                stub.requests.append(body)
                if status == 200:
                    payload = json.dumps({"problem": body["problem"], "EP": 0.25, "NHV": 0.5,
                                          "timeMs": 3}).encode()
                else:
                    payload = b"A configuration failed on problem ZDT1"
                self.send_response(status)
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def log_message(self, *args):
                pass

        self.httpd = HTTPServer(("127.0.0.1", 0), Handler)
        self.url = f"http://127.0.0.1:{self.httpd.server_port}"
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()

    def close(self):
        self.httpd.shutdown()


@pytest.fixture
def stub():
    server = StubServer()
    yield server
    server.close()


class TestTarget:
    def test_should_send_the_configuration_the_problem_the_tool_and_the_seed(self, stub):
        # Act
        client.EvaluationServerTarget(stub.url).evaluate("--crossover SBX", "ZDT4", seed=9)

        # Assert
        assert stub.requests == [{"configuration": "--crossover SBX", "problem": "ZDT4",
                                  "tool": "smac", "seed": 9}]

    def test_should_return_the_two_objectives(self, stub):
        # Act
        result = client.EvaluationServerTarget(stub.url).evaluate("--a 1", "ZDT1")

        # Assert
        assert result == {"EP": 0.25, "NHV": 0.5}

    def test_should_raise_when_the_configuration_fails(self):
        # Arrange
        server = StubServer(status=422)
        try:
            # Act / Assert
            with pytest.raises(RuntimeError, match="422"):
                client.EvaluationServerTarget(server.url).evaluate("--a 1", "ZDT1")
        finally:
            server.close()

    def test_should_turn_a_configuration_of_smac3_into_arguments(self, stub):
        # Arrange
        space = ConfigurationSpace({"crossover": ["SBX", "blx"], "p": Float("p", (0.0, 1.0))})
        configuration = space.get_default_configuration()

        # Act
        arguments = client.configuration_to_arguments(configuration)

        # Assert
        assert arguments == "--crossover SBX --p 0.5"
        client.EvaluationServerTarget(stub.url).train(configuration, "ZDT2", 4)
        assert stub.requests[0]["configuration"] == arguments
        assert stub.requests[0]["problem"] == "ZDT2"


class TestOptimize:
    def test_should_run_the_trials_on_the_problems_and_find_the_front(self, stub, tmp_path):
        # Arrange
        space = ConfigurationSpace({"p": Float("p", (0.0, 1.0))}, seed=1)
        target = client.EvaluationServerTarget(stub.url)

        # Act
        problems = ["ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6"]
        smac = client.optimize(space, target, problems, trials=60, workers=1, seed=1,
                               output_dir=tmp_path, initial_configurations=5)

        # Assert: 60 trials is enough to train the model of SMAC3 with 5 instance features
        assert len(stub.requests) == 60
        assert {r["problem"] for r in stub.requests} <= set(problems)
        assert all(r["tool"] == "smac" for r in stub.requests)
        assert len(smac.intensifier.get_incumbents()) >= 1
