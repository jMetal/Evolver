.. _tutorial_extending_evolver:

E17. Extending Evolver: Your Own Problem and Operator
=====================================================

:Level: Advanced
:Version: 1.0 (2026-10-07)
:Time: about 40 minutes, plus the runs of the examples (about 5 minutes)
:Timings measured on: Apple M5 Pro (18 cores, 14 of them used by the training), 64 GB of RAM,
   macOS 26.6.2, Java 21.0.12 (Oracle JDK)
:Prerequisites: :doc:`E1. Parameter spaces <parameter_spaces>`,
   :doc:`E2. Base-level algorithms <base_level_algorithms>`,
   :doc:`E9. Problems without a reference front <problems_without_reference_front>`

Evolver is useful when it works with *your* problems and when it offers the operators that you want
to try. Those are the two extensions that people make most often, and this tutorial shows both: how
to use a problem of your own, with nothing to change in Evolver, and how to add an operator that the
catalogue does not have, which does need a small change. Extending Evolver with more complex
components, or with a new algorithm, is outside of the scope of the tutorials for now.

The code of the first part is the class
`ExtendingTutorial <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/tutorial/ExtendingTutorial.java>`_
and the problem
`BiSphere <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/tutorial/BiSphere.java>`_
(package ``org.uma.evolver.example.tutorial``). The operator of the second part is **not** part of
Evolver: its code, a test and a patch are in ``docs/tutorials/extending_evolver/``, to apply to your
copy of the repository.

Part 1: your own problem
------------------------

Step 1: the problem
~~~~~~~~~~~~~~~~~~~

A problem for Evolver is a problem for jMetal: a class that implements ``DoubleProblem`` (or the
interface of another encoding, see tutorial :doc:`E10 <binary_and_permutation_encodings>`). The one of
this tutorial, ``BiSphere``, has two objectives, the squared distance to the origin and the squared
distance to the point (2, ..., 2), with the variables in [-5, 5]:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/BiSphere.java
   :language: java
   :start-at: public class BiSphere

Its Pareto set is the segment between the two points, so its Pareto front is known exactly: with
``n`` variables and ``t`` in [0, 1], ``f1 = 4 n t²`` and ``f2 = 4 n (1 − t)²``. The class has a method
that returns the points of that front.

Step 2: the reference front
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The quality indicators need a reference front (tutorial :doc:`E9 <problems_without_reference_front>`
explains what to do when it is not known). When the front can be computed, as here, a few lines
write it, as a CSV file without a header:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ExtendingTutorial.java
   :language: java
   :start-after: // [step-1-start]
   :end-before: // [step-1-end]
   :dedent: 4

Step 3: running an algorithm on it
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A request refers to a problem by its **class name**, and the arguments of its constructor, if it has
them. Evolver finds the class by reflection, so it only has to be on the classpath: nothing is
registered. This is the solve request of the tutorial, NSGA-II with its default configuration on
``BiSphere`` with 20 variables:

.. literalinclude:: ../../src/main/resources/cli/solving/tutorial-extending-request.yaml
   :language: yaml
   :start-at: algorithmName

``BiSphere`` is in the jar of Evolver, so the jar is enough. For a problem of your own, put the jar and
your classes in the classpath (``java -cp evolver.jar:myclasses ...``), or a jar of yours. If the
problem has the wrong encoding for the algorithm, the request fails before running with a message that
says so (tutorial :doc:`E16 <automating_with_the_cli>`). Running it from Java is the same, and prints
the median of each indicator:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ExtendingTutorial.java
   :language: java
   :start-after: // [step-2-start]
   :end-before: // [step-2-end]
   :dedent: 4

.. code-block:: none

    EP: median 0.02055 over 10 runs
    NHV: median 0.01655 over 10 runs

Step 4: tuning an algorithm for your problem
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The training sets of Evolver are made of problems that are also given by name or by class, so the
meta-optimization works on your problem in the same way. The base-level file lists it, with its
reference front and the budget of each run:

.. literalinclude:: ../../src/main/resources/baseLevelConfigurations/TutorialBiSphereBaseLevel.yaml
   :language: yaml
   :start-at: algorithmName

With the meta-optimizer of tutorial E11 (NSGA-II, stopped after 2 minutes) the training is:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ExtendingTutorial.java
   :language: java
   :start-after: // [step-3-start]
   :end-before: // [step-3-end]
   :dedent: 4

It performed 2600 meta-evaluations in 2 minutes and 1 second. The 7 configurations of its final front
share the same structure: an external archive, 5 offspring per generation, the ``wholeArithmetic``
crossover, polynomial mutation, and best NHV between 0.0003 and 0.0006. That NHV comes from runs of
10000 evaluations, and it is optimistic (tutorial :doc:`E8 <validating_a_configuration>` explains
why), so the configuration with the best NHV was validated with the budget and the 10 runs of Step 3:

.. list-table::
   :header-rows: 1
   :widths: 40 30 30

   * - Configuration
     - EP (median)
     - NHV (median)
   * - NSGA-II, default
     - 0.02055
     - 0.01655
   * - Tuned for ``BiSphere``
     - 0.00490
     - 0.00010

The tuned configuration is far better than the default on this problem, and it has no reason to be
good on others: ``BiSphere`` is smooth and separable, and the arithmetic crossover that the training
chose suits it. It shows that the meta-optimization works on a problem that Evolver had never seen,
and the point of the tutorial is how to set it up, not what it finds.

Step 5: from a class name to a name of the catalogue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you contribute the problem to Evolver, it can have a short name (``problem: BiSphere``), that
``DescribeMain`` lists, and that Evolver-Studio offers. It takes three changes in
``org.uma.evolver.cli.ProblemRegistry``:

- an entry in its table of names, ``Map.entry("BiSphere", BiSphere.class)``;
- if the problem has constructor arguments, an entry of the table of arguments with their names, types
  and defaults (``numberOfVariables``), which is what ``DescribeMain`` shows to the tools that build
  requests (tutorial E16);
- its reference front, as ``resources/referenceFronts/BiSphere.csv``, named after the problem so that
  Evolver-Studio finds it.

The tests of the registry build every problem of the table with its defaults, and check that it has a
descriptor and that its encoding is the one of its interface. The family that ``DescribeMain`` shows for a problem is the last
segment of its package (``zdt``, ``dtlz``, ...), so a problem is best contributed in a package that
says what it is.

Part 2: your own operator
-------------------------

Step 6: what the catalogue has
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An operator is a value of a parameter: ``crossover`` or ``mutation`` in the parameter spaces of
tutorial :doc:`E1 <parameter_spaces>`. These are the ones that Evolver offers:

.. list-table::
   :header-rows: 1
   :widths: 18 41 41

   * - Encoding
     - Crossover
     - Mutation
   * - Double
     - SBX, blxAlpha, blxAlphaBeta, wholeArithmetic, arithmetic, fuzzyRecombination, laplace, PCX,
       UNDC, SDX
     - polynomial, linkedPolynomial, uniform, nonUniform, levyFlight, powerLaw
   * - Binary
     - HUX, uniform, singlePoint
     - bitFlip
   * - Permutation
     - PMX, CX, OXD, positionBased, edgeRecombination
     - swap, displacement, insert, scramble, inversion, simpleInversion

jMetal has operators that Evolver does not offer (the n-point crossover, for instance, which only needs
a parameter for the number of cutting points), and any of them would be added in the way of this part.
A **Gaussian mutation**, which adds to each variable a
number from a normal distribution, is in neither of them. It is a classic, simple, with one parameter
and not competitive with the polynomial mutation of NSGA-II, which makes it a good example: the
point is how to add an operator, not that it improves the algorithms. It is not part of Evolver; the
code of this part is in ``docs/tutorials/extending_evolver/``.

Step 7: the operator
~~~~~~~~~~~~~~~~~~~~

The operator is a class of jMetal, ``MutationOperator<DoubleSolution>``, with a probability, one
parameter (the standard deviation, as a fraction of the width of the range of each variable) and the
repair strategy that the other operators of the catalogue have. It goes next to the parameter that will
use it, in ``org.uma.evolver.parameter.catalogue.mutationparameter``:

.. literalinclude:: extending_evolver/GaussianMutation.java
   :language: java
   :start-at: public class GaussianMutation

Step 8: the three changes
~~~~~~~~~~~~~~~~~~~~~~~~~

Adding it to the catalogue takes three changes, and a test.

**1. The catalogue knows its name and how to build it**, in ``DoubleMutationParameter``: the name
is added to the list of valid names, to the ``switch`` of ``getMutation()``, and a method builds the
operator, reading its parameter from the configuration:

.. code-block:: diff

    -      List.of("polynomial", "linkedPolynomial", "uniform", "nonUniform", "levyFlight", "powerLaw");
    +      List.of(
    +          "polynomial", "linkedPolynomial", "uniform", "nonUniform", "levyFlight", "powerLaw",
    +          "gaussian");
     ...
           case "powerLaw" -> createPowerLawMutation(mutationProbability, repairDoubleSolution);
    +      case "gaussian" -> createGaussianMutation(mutationProbability, repairDoubleSolution);
     ...
    +  private MutationOperator<DoubleSolution> createGaussianMutation(
    +      double mutationProbability, RepairDoubleSolutionStrategyParameter repairStrategy) {
    +
    +    double sigma = (Double) findConditionalParameter("gaussianMutationSigma").value();
    +    return new GaussianMutation(
    +        mutationProbability, sigma, repairStrategy.getRepairDoubleSolutionStrategy());
    +  }

**2. A parameter space offers it**, with its parameter as a conditional one, active only when the
mutation is ``gaussian``. The bundled spaces do not change (so that the results of the other
tutorials stay the same): the tutorial uses a copy of ``NSGAIIDouble.yaml`` that adds, to the values
of ``mutation``:

.. code-block:: yaml

            gaussian:
              conditionalParameters:
                gaussianMutationSigma:
                  type: double
                  range: [0.001, 0.5]

The name of a parameter has to be unique in the space, as the names of the other operators' parameters
show (``polynomialMutationDistributionIndex``, ``levyFlightMutationBeta``, ...); and the ranges are a
decision, as tutorial :doc:`E5 <designing_parameter_spaces>` explains.

**3. A test**, with the operator on its own (probability 1 changes every variable and keeps it in its
bounds, probability 0 changes none, a sigma that is not positive is rejected) and the parameter that
builds it from a configuration string:

.. literalinclude:: extending_evolver/GaussianMutationTest.java
   :language: java
   :start-at: @DisplayName("Unit tests for the Gaussian mutation of tutorial E17")

To apply all of it to your copy of Evolver (the class, the test, the changes of the catalogue and the
parameter space, ``NSGAIIDoubleGaussian.yaml``), from the root of the repository:

.. code-block:: bash

    git apply docs/tutorials/extending_evolver/gaussian-mutation.patch
    mvn test -Dtest=GaussianMutationTest

``git apply -R`` on the same file removes it again.

Step 9: using it
~~~~~~~~~~~~~~~~

The operator is a value of ``mutation`` like any other: a configuration that uses it, written
against the parameter space of Step 8, runs the algorithm. NSGA-II with its default configuration,
with the polynomial mutation replaced by the Gaussian one with a standard deviation of 10 % of the
range:

.. code-block:: none

    --algorithmResult population --createInitialSolutions default
    --variation crossoverAndMutationVariation --offspringPopulationSize 100
    --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0
    --mutation gaussian --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds
    --gaussianMutationSigma 0.1
    --selection tournament --selectionTournamentSize 2

On ``BiSphere`` with the request of Step 3 (10 runs of 25000 evaluations, same seeds):

.. list-table::
   :header-rows: 1
   :widths: 40 30 30

   * - Mutation
     - EP (median)
     - NHV (median)
   * - polynomial (the default)
     - 0.02055
     - 0.01655
   * - Gaussian, sigma 0.1
     - 0.02448
     - 0.01920

It is a little worse than the polynomial mutation, as expected: it is not the aim of the example, and
nobody has tuned its sigma. But a meta-optimizer can now choose it: with this space in a training request,
the Gaussian mutation is one more value that it can combine with the rest.

What this tutorial does not cover
---------------------------------

- Components with more structure than an operator (a ranking, a density estimator, a replacement) and
  **new algorithms** are outside of the scope of the tutorials for now. The algorithms are built from
  the components of jMetal, and what a new one needs is described by the classes of
  ``org.uma.evolver.algorithm`` (``BaseNSGAII`` is a good one to read).
- Operators from outside of Evolver, with no change in it, are not supported: the names that a
  parameter accepts are a list of the class (Step 8, change 1), so adding an operator means changing
  Evolver, or a copy of it.

Try it yourself
---------------

- Add the **uniform crossover for real numbers** (each variable of a child comes from one of the
  parents, with probability 0.5). jMetal has one for binary solutions only, and Evolver has none for
  doubles. It has no parameter, so it is simpler than the mutation of the tutorial: the changes are the
  ones of Step 8, in ``DoubleCrossoverParameter``.
- Give ``BiSphere`` a short name in the registry (Step 5), and check what ``DescribeMain`` says about it.
- Tune NSGA-II with the space that has the Gaussian mutation, with the training of Step 4. Does the
  meta-optimizer choose it?
- Write a problem with a constraint, or with three objectives, and run it as in Step 3.

What's next
-----------

- :doc:`E16 <automating_with_the_cli>` explains the requests that this tutorial uses, and how to run
  many of them.
- :doc:`E12 <ablation>` studies which components of a tuned configuration matter, which is how to
  know whether an operator that you added helps.
