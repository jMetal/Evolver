.. _examples:

Examples
========

The Evolver project includes many runnable examples, in the ``org.uma.evolver.example`` package:

- ``org.uma.evolver.example.baselevel``: configurable algorithms used on their own, without meta-optimization.

    - ``standard``: typical configurations of every algorithm (e.g. ``NSGAIIForZDT1Example``, ``MOEADBiObjectiveTSPExample``).
    - ``tuned``: configurations found by meta-optimization (e.g. ``NSGAIIBiObjectiveTSPExample``).
    - ``features``: demonstrations of specific capabilities, such as external archives or observers (e.g. ``NSGAIIZDT4WithArchiveExample``).
    - ``constrained``: the algorithms with their parameter spaces for problems with constraints, which add a constraint-handling technique (e.g. ``ConstrainedNSGAIIForSrinivasExample``, with the default technique, the constrained-domination principle; ``ConstrainedNSGAIIForTanakaExample`` and ``ConstrainedNSGAIIForOsyczka2Example``, with other techniques; and ``ConstrainedNSGAIILIRCMOP2Example``, which runs NSGA-II with and without a constraint-handling technique on LIR-CMOP2).

- ``org.uma.evolver.example.training``: meta-optimization runs, grouped by benchmark (``zdt``, ``dtlz``, ``re3d``, ``tsp``). They cover the flat and tree encodings and several meta-optimizers (NSGA-II, SPEA2, SMPSO, Async NSGA-II, Random Search, …); most of them build a ``TrainingRequest`` and run it with ``TrainingRunner``, the same pipeline used by ``cli.training``.
- ``org.uma.evolver.example.validation``: comparative studies of tuned and standard configurations on validation problems.

Most training examples can also be run without Java code from the ``request.yaml`` files in ``src/main/resources/cli/training/`` (see :doc:`../utilities/cli_tools`).
