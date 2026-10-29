.. _tutorial_problems_with_constraints:

E12. Problems with Constraints
==============================

:Level: Intermediate
:Version: 1.0 (2026-10-29)
:Time: about 40 minutes, of which the code of the tutorial runs about 5
:Timings measured on: Apple M5 Pro (18 cores, 14 of them used by the training), 64 GB of RAM,
   macOS 26.6.2, Java 21.0.12 (Oracle JDK)
:Prerequisites: :doc:`E3. Meta-optimization workflow <meta_optimization_workflow>`,
   :doc:`E6. Training sets, indicators and budgets <training_sets_indicators_budgets>`;
   recommended: :doc:`E7. Analyzing training results <analyzing_training_results>`

Many real problems have constraints: a solution is only valid if it satisfies them, whatever its
objective values. This tutorial explains how Evolver deals with them at both levels: how NSGA-II
handles the constraints of a problem with a constraint-handling technique, how the runs are
measured, and how a configuration is tuned, technique included, for a problem with constraints. It
uses two problems of the DAS-CMOP suite, DASCMOP2 and DASCMOP5, and the class
`ProblemsWithConstraintsTutorial <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/tutorial/ProblemsWithConstraintsTutorial.java>`_.

Step 1: problems with constraints
---------------------------------

In jMetal, a problem with constraints evaluates them with the objectives: each constraint has a
value, which is negative when the constraint is violated. A solution is **feasible** when no
constraint is violated, and its **violation** is the sum of the negative values (in absolute value):
how far it is from satisfying them.

Evolver registers the problems with constraints of jMetal with short names, with their reference
fronts in ``resources/referenceFronts/``: LIR-CMOP1-14, CF1-16, C-DTLZ, DAS-CMOP1-9, MW1-14, the
real-world suites CRE and RCM (CEC 2021), and classical and engineering problems such as Srinivas,
Tanaka or Osyczka2. The manifest of the CLI gives the ``numberOfConstraints`` of each problem
(:doc:`E18 <automating_with_the_cli>`).

Step 2: NSGA-II with a constraint-handling technique
----------------------------------------------------

``NSGAIIDouble.yaml``, the parameter space of NSGA-II for real-valued problems, ignores the
constraints: it ranks the solutions only by their objectives. ``NSGAIIDoubleConstrained.yaml`` is
the same space plus a ``constraintHandling`` parameter, with the constraint-handling techniques of
jMetal:

.. list-table::
   :header-rows: 1
   :widths: 25 75

   * - Technique
     - How it compares two solutions
   * - ``cdp``
     - Deb's constrained-domination principle: a feasible solution is better than an infeasible
       one, two infeasible ones are compared by their violation, and two feasible ones by
       dominance. It is the technique of the default configuration
       (``NSGAIIDoubleConstrainedDefault.txt``).
   * - ``violatedConstraints``
     - As ``cdp``, but two infeasible solutions are compared by the number of violated constraints.
   * - ``epsilonConstrained``
     - As ``cdp``, but a violation below a tolerance ε counts as feasible; ε starts at a quantile
       of the violations of the initial population and decreases to 0 during the run.
   * - ``stochasticRanking``
     - Runarsson and Yao's stochastic bubble sort: when one of two solutions is infeasible, they
       are compared by their objectives with a given probability, and by their violation otherwise,
       so that the search can cross infeasible regions.
   * - ``pushPull``
     - Push and pull search: first the constraints are ignored (push) until the population
       settles, and then it is pulled to the feasible region with an epsilon-constrained method
       (pull).
   * - ``selfAdaptivePenalty``
     - The objectives are penalized with the violation, with a penalty that adapts to the fraction
       of feasible solutions of the population.

Without the parameter, NSGA-II does not change: with the same seed it gives the same fronts as with
``NSGAIIDouble.yaml``. The examples of the package
`example.baselevel.constrained <https://github.com/jMetal/Evolver/tree/develop/src/main/java/org/uma/evolver/example/baselevel/constrained>`_
solve Srinivas, Tanaka, Osyczka2 and LIR-CMOP2 with several techniques, and show the population in
a live plot (``ConstrainedFrontPlotObserver``), with the feasible solutions in black and the
infeasible ones in red.

Step 3: measuring a run
-----------------------

The quality indicators of a run (EP, NHV, ...) are computed on its **feasible** solutions only: an
infeasible solution is not a solution of the problem, and it may lie beyond the true front, where it
would make the run look better than it is. A run is therefore described by two things: whether it
found feasible solutions, and their quality.

The indicator of Vodopija, Tušar and Filipič (IEEE TEVC 29(1), 2025), ``ICMOP`` (its name in
``indicatorNames`` is ``ConstrainedMultiobjectiveIndicator``), puts both in one value:

- if the run has feasible solutions, it is minus the hypervolume of the feasible ones (normalized
  with the reference front), or their distance to the region of interest if none of them dominates
  the nadir point; this value is at most 1;
- if it has none, it is 1 plus the least violation of its solutions.

So a value of at most 1 means feasible solutions, lower is better, and the minimum is −1, the
hypervolume of the whole normalized region. ICMOP is the indicator used in this tutorial to compare runs.

Step 4: with and without a technique
------------------------------------

The first step of the class runs NSGA-II with its default configuration, without a technique
(``NSGAIIDouble.yaml``) and with ``cdp`` (``NSGAIIDoubleConstrained.yaml``), 15 times on each
problem with 20000 evaluations:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ProblemsWithConstraintsTutorial.java
   :language: java
   :start-after: // [step-1-start]
   :end-before: // [step-1-end]
   :dedent: 4

.. list-table::
   :header-rows: 1
   :widths: 20 40 40

   * - Problem
     - Without a technique
     - With ``cdp``
   * - DASCMOP2
     - feasible in 0 of 15 runs (median ICMOP 1.543)
     - feasible in 15 of 15 runs (median ICMOP −0.428)
   * - DASCMOP5
     - feasible in 11 of 15 runs (median ICMOP −0.335)
     - feasible in 10 of 15 runs (median ICMOP −0.001)

On DASCMOP2, NSGA-II without a technique never finds a feasible solution: its population goes to
the front of the problem without constraints, which is infeasible. With ``cdp`` every run does.

DASCMOP5 shows the other side: with its default configuration, ``cdp`` is not better than ignoring
the constraints. A technique is necessary, but the technique and the rest of the configuration have
to suit the problem, which is what tuning is for.

Step 5: evaluating a configuration on problems with constraints
---------------------------------------------------------------

During a training, a configuration is run on each problem of the training set, and its value for
each indicator aggregates those runs (:ref:`evaluation`). When a problem of the training set has
constraints, the value of an indicator is

.. math::

   6k + 2\bar{v} + Q

where :math:`k` is the number of runs, over all the problems, without feasible solutions;
:math:`\bar{v}`, in [0, 1), is the mean violation of those runs, scaled by problem; and :math:`Q` is
the mean over the problems of the median of the indicator on the feasible runs, capped at a
threshold (1 for ICMOP, EP and NHV), which is also the value of a problem without feasible runs.

Each failed run adds 6, more than the other two terms together, so a configuration with fewer failed
runs is always better; with the same number of failed runs, the one closer to the feasible region
is better; and only when both tie does the quality decide. No failed run is hidden by the
aggregation. With one run on one problem, as in this tutorial, a configuration with feasible
solutions has values of at most 1, and one without them has values of 6 or more.

The meta-objectives used here are ICMOP and EP, as recommended in :ref:`evaluation` (section
*Choosing the indicators*): ICMOP should not be used alone, since once every run is feasible it is a
single indicator, the hypervolume.

Step 6: tuning NSGA-II for DASCMOP5
-----------------------------------

The second step tunes NSGA-II with ``NSGAIIDoubleConstrained.yaml``, so the technique is one more
parameter, for DASCMOP5, with the asynchronous NSGA-II as meta-optimizer:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ProblemsWithConstraintsTutorial.java
   :language: java
   :start-after: // [step-2-start]
   :end-before: // [step-2-end]
   :dedent: 4

Each configuration is run once with 10000 evaluations, half of the 20000 of the other steps: a
smaller training budget makes the training affordable, and the comparison of Step 8 checks that the
configuration found stays good with the full budget. The request writes a checkpoint every 10
configurations and, with its last argument (``writePopulation``), the whole population of the
meta-optimizer as well as its front: the front alone hides the configurations without feasible
solutions as soon as one configuration has them. The training took 158 s.

.. note::

   A training stops at the start if its output directory already has the results of another one.
   To run the class again, delete ``results/tutorial-problems-with-constraints`` first.

To watch the meta-front during a training, run the example
`AsyncNSGAIIOptimizingNSGAIIForProblemDASCMOP5 <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/training/constrained/AsyncNSGAIIOptimizingNSGAIIForProblemDASCMOP5.java>`_,
the same training with a live plot.

Step 7: how the search reaches the feasible configurations
----------------------------------------------------------

The third step counts, at some checkpoints of ``POPULATION_INDICATORS.csv``, the configurations of
the population with feasible solutions (ICMOP at most 1), and chooses the configuration of the final
front with the lowest ICMOP:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ProblemsWithConstraintsTutorial.java
   :language: java
   :start-after: // [step-3-start]
   :end-before: // [step-3-end]
   :dedent: 4

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Evaluation
     - Configurations with feasible solutions
   * - 50
     - 4 of 50
   * - 100
     - 10 of 50
   * - 150
     - 14 of 50
   * - 200
     - 31 of 50
   * - 250
     - 45 of 50
   * - 300 and later
     - 50 of 50

The asynchronous NSGA-II adds every configuration to its population until it has 50, so the
checkpoint at evaluation 50 is the random initial population: 46 of its 50 configurations fail.
Random configurations are much worse than the default one, which fails in 5 of 15 runs. The failed
configurations are then replaced, and from evaluation 300 every configuration of the population
finds feasible solutions; from then on the search improves their quality. Later, a configuration
that fails is discarded as soon as it is evaluated, so the population does not show every failure,
only when the search stops producing them often.

Step 8: the tuned configuration against the default one
-------------------------------------------------------

The last step runs the chosen configuration and the default one 15 times on DASCMOP5 with 20000
evaluations:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ProblemsWithConstraintsTutorial.java
   :language: java
   :start-after: // [step-4-start]
   :end-before: // [step-4-end]
   :dedent: 4

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Configuration
     - DASCMOP5, 15 runs with 20000 evaluations
   * - Default (``cdp``)
     - feasible in 10 of 15 runs, median ICMOP −0.001
   * - Tuned
     - feasible in 15 of 15 runs, median ICMOP −0.622

The tuned configuration finds feasible solutions in every run, and its median ICMOP is close to the
value of the reference front. The meta-optimizer is stochastic, and two trainings do not give the
same configuration: in this one, the chosen configuration uses ``pushPull``, an offspring population
of 5 and Boltzmann selection; in another training it used ``cdp``, whole arithmetic crossover and
stochastic universal sampling, and was feasible in 14 of 15 runs with a median ICMOP of −0.615.
Different configurations of similar quality are usual when the training set is a single problem.

This comparison is on the problem of the training, with 15 runs: it shows that the training worked,
not that the configuration generalizes. Choosing a training set of several problems with
constraints, and validating on problems not seen in the training (:doc:`E8
<validating_a_configuration>`), is the subject of a planned advanced tutorial.

Try it yourself
---------------

- Run the base-level examples of ``example.baselevel.constrained`` and watch the feasible and
  infeasible solutions; in ``ConstrainedNSGAIIForLIRCMOP2Example``, try the configuration without
  a technique and the epsilon-constrained one of its Javadoc.
- Run the class again (after deleting its output directory): does the training choose the same
  technique? Count the techniques of the final population with ``POPULATION_CONFIGURATIONS.csv``.
- Tune NSGA-II for DASCMOP2 instead of DASCMOP5: how many configurations of the initial population
  fail there?
- Run NSGA-II with the default configuration and ``--constraintHandling pushPull`` (with values of
  the ranges of ``NSGAIIDoubleConstrained.yaml``) on DASCMOP5: how much of the improvement does the
  technique alone bring?

What's next
-----------

- :ref:`evaluation` describes in detail how a configuration is evaluated on problems with
  constraints, and how to choose the indicators.
- :doc:`E7 <analyzing_training_results>` explains the output files of a training, including the
  population files.
- :doc:`E8 <validating_a_configuration>` explains how to validate a configuration on other problems.
