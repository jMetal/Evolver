.. _tutorial_llms_in_evolver:

E21. Language Models in Evolver
===============================

:Level: Advanced
:Version: 1.0 (2026-10-14)
:Time: about 30 minutes, plus the runs of the example (about 2 minutes)
:Timings measured on: Apple M5 Pro (18 cores, 8 of them used by the trainings), 64 GB of RAM,
   macOS 26.6.2, Java 21.0.12 (Oracle JDK), Ollama with ``qwen3.5:35b-mlx`` (4-bit) on the same
   computer
:Prerequisites: :doc:`E4. Evolver in 10 minutes <../quick_start>`, and a language model you can
   reach (a local Ollama, or the API of a provider)

Language models know a lot about metaheuristics: they have read the papers and the code of NSGA-II,
SBX or crowding distance. Whether that knowledge helps to configure or design algorithms is an open
research question, and Evolver, as research software for automatic algorithm configuration and
design, offers the pieces to study it, not an answer. This tutorial shows those pieces with one
simple use, **proposing configurations that seed a training**, and lists other directions at the
end. The example is an illustration, not a study.

This part of Evolver is **experimental**: its interfaces may change in a minor version.

Step 1: the model
-----------------

Evolver talks to a model through an interface, ``LlmClient`` (package
``org.uma.evolver.meta.llm``), with two implementations:

- ``OllamaClient``, for a model served by `Ollama <https://ollama.com>`_, usually on the same
  computer: nothing leaves it, and a call costs time but not money;
- ``OpenAiCompatibleClient``, for any server of the OpenAI chat API: OpenAI itself, and also vLLM,
  LM Studio, the llama.cpp server, OpenRouter, or Ollama's own ``http://localhost:11434/v1``.

A small YAML file chooses one. For a local Ollama (``src/main/resources/llm/ollama-settings.yaml``):

.. literalinclude:: ../../src/main/resources/llm/ollama-settings.yaml
   :language: yaml

and for an external API (``src/main/resources/llm/openai-settings.yaml``):

.. literalinclude:: ../../src/main/resources/llm/openai-settings.yaml
   :language: yaml

The key of an external API is **never written in the file**: ``apiKeyEnv`` names the environment
variable that holds it. ``options`` are added to each request as they are, for the settings of each
server: here ``think: false`` turns off the reasoning of the model: with it, a call for 5 configurations
took 12 seconds, while the same call through Ollama's OpenAI API, with the reasoning on, took 108
seconds (there, the same setting is ``reasoning_effort: none``).

Step 2: proposing configurations
--------------------------------

``ConfigurationProposer`` asks the model for configurations of a parameter space. The prompt gives
it the YAML of the space, a description of the task and, optionally, example configurations; the
answer is forced to a JSON list of configurations, each with a one-sentence rationale. Then Evolver
checks each one against the space (by building its derivation tree, the check of the tree encoding)
and keeps only the valid ones, rewritten with exactly the parameters they activate; the invalid ones
are sent back once, with the error of each, to be corrected. Every call goes to a log,
``LLM_CALLS.csv``: time, model, tokens, milliseconds, and each proposal with its validity, its error
and its rationale. A language model is not deterministic, even with a fixed seed, so a run with a
model cannot be repeated exactly: the log is what makes it auditable.

``ProposeConfigurationsMain`` does it from the command line. For NSGA-II on ZDT4, the scenario of
tutorial E4, with the default configuration of NSGA-II as an example:

.. code-block:: bash

    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.cli.llm.ProposeConfigurationsMain \
        --settings src/main/resources/llm/ollama-settings.yaml \
        --space NSGAIIDouble.yaml --encoding Double --count 10 \
        --examples src/main/resources/defaultConfigurations/NSGAIIDoubleDefault.txt \
        --task "NSGA-II for the bi-objective problem ZDT4 (many local fronts), with a population of
                100 and 12000 evaluations; the quality of a front is measured with the additive
                epsilon and the normalized hypervolume indicators" \
        --output results/tutorial-llm/configurations.txt

It took 35 seconds. The first call returned 10 configurations, 4 of them valid; the other 6 had
forgotten a parameter (``mutationRepairStrategy``, ``crossoverProbability``: the *global*
sub-parameters of the YAML, active whenever their parent is), and the second call corrected 3 of
them. ``configurations.txt`` has the 7 valid ones, one per line, and the rationales are printed:

.. code-block:: none

    - Uses crowding distance for bi-objective preservation with Latin Hypercube initialization to
      ensure diversity across the many local fronts of ZDT4.
    - Leverages scatter search for strong initialization on multimodal problems and unbounded
      archive to capture diverse fronts, supported by ranking selection.
    ...

The rationales read well, but they are not evidence: they are what the model says about its
proposals, not what the proposals do.

Step 3: seeding a training
--------------------------

The configurations file can be the ``initialConfigurations`` of a meta-optimizer
(:doc:`../utilities/cli_tools`): its first solutions are those configurations, and the rest of the
initial population is random. ``TutorialQuickNSGAIISeededMetaSearch.yaml`` is the meta-optimizer of
tutorial E4 with that line added, and ``tutorial-llm-request.yaml`` the request:

.. code-block:: bash

    cp src/main/resources/cli/training/tutorial-llm-request.yaml results/tutorial-llm/request.yaml
    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.cli.training.TrainingRunnerMain results/tutorial-llm/request.yaml

and, for comparison, the same training with a random initial population, the request of E4 with
``outputDirectory: results/tutorial-llm/random``. Each took about 30 seconds. The best NHV of the
front of the meta-optimizer (lower is better):

.. list-table::
   :header-rows: 1
   :widths: 40 30 30

   * - Training
     - After 100 configurations
     - After 500
   * - Seeded with the 7 proposals
     - 0.0246
     - 0.0142
   * - Random initial population
     - 0.0413
     - 0.0159

The seeded training starts better, as expected from starting at reasonable configurations, and ends
slightly better. With **one run of each**, that says nothing about whether seeding with a language
model helps: random and seeded trainings vary from run to run (tutorial
:doc:`E20 <independent_replications>` shows how much), the budget is small, and the problem is one
that the model may know well. Answering it needs a study.

Step 4: research directions
---------------------------

What Evolver offers (a client for local and external models, a proposer that validates and logs,
the seeding of the meta-optimizers, the evaluation server, replications and statistics) makes
several questions studiable:

- **Seeding**: are configurations proposed by a model better starting points than random ones, or
  than the default configuration, at equal total time (the calls cost time)? Does it hold on
  problems the model is unlikely to know?
- **Guided mutation**: a model proposing changes to configurations during the search, as a mutation
  operator. A proof of concept with a (1+1) search on NSGA-II and ZDT problems found a hybrid (the
  usual tree mutation, and the model when the search stalls) better than the tree mutation at equal
  time in 4 of 5 seeds; the model made 15 % of the mutations but took half of the time. Five seeds
  are not enough to conclude anything.
- **Explaining configurations**: a model describing in words what a tuned configuration does, or the
  result of an ablation (tutorial E13).
- **Designing parameter spaces and components**: proposing new parameter spaces or operators, which
  Evolver then checks and evaluates.

Any such study should take into account:

- **non-determinism**: log every call and report the model, its version and its settings;
- **contamination**: the model may know configurations of NSGA-II for ZDT; control with renamed
  parameters and values, or with spaces and problems the model is unlikely to know;
- **cost**: report the time of the calls and their share of the total, and compare at equal time;
- **replications**: as for any training (tutorial E20).

What you can try
----------------

- Ask without examples (remove ``--examples``): are the proposals more varied, and how many are
  valid?
- Use another model, local or external, and compare the validity and the time per call.
- Seed a training of tutorial E20 with model proposals and run five replications of each variant.
