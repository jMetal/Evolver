.. _tutorial_parameter_spaces:

E1. Parameter Spaces
====================

:Level: Introductory
:Version: 1.0 (2026-09-25)
:Time: about 20 minutes
:Prerequisites: Evolver built with Maven (see :ref:`installation`); basic Java

Every configurable algorithm in Evolver is described by a **parameter space**: the set of all the
ways the algorithm can be configured. This tutorial explains what a parameter space is, the kinds of
parameters it contains and how they depend on each other, using the parameter spaces of NSGA-II for
continuous and binary problems. You will:

- load a parameter space from its YAML file and explore its structure;
- see how two encodings of the same algorithm share a structure but offer different operators;
- turn a configuration string into a concrete point of the space, and see how wrong configurations
  are rejected.

No algorithm is run here: running algorithms configured from a parameter space is the subject of
the next tutorial.

The code of this tutorial is the class
`ParameterSpacesTutorial <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java>`_,
in package ``org.uma.evolver.example.tutorial``. Its fragments are shown step by step below.

What is a parameter space?
--------------------------

A metaheuristic such as NSGA-II is built from components: how the initial population is created,
which crossover and mutation operators produce the offspring, how parents are selected, whether the
result is the final population or an external archive, and so on. Each of these choices is a
**parameter**, and many of them have parameters of their own: the SBX crossover needs a
distribution index, tournament selection needs a tournament size.

A **parameter space** gathers all those parameters, their possible values and the relations
between them. It serves two purposes in Evolver:

- **Configuring an algorithm**: picking one value for every parameter that applies gives a
  *configuration*, a concrete variant of the algorithm that can be run.
- **Tuning an algorithm**: the meta-optimizer searches the parameter space for the configurations
  that perform best on a set of training problems.

Parameter spaces are defined in YAML files, in ``src/main/resources/parameterSpaces/``, one per
algorithm and encoding: ``NSGAIIDouble.yaml`` (NSGA-II for continuous problems),
``NSGAIIBinary.yaml`` (binary problems), ``MOEADDouble.yaml`` (MOEA/D), and so on.

Step 1: loading a parameter space
---------------------------------

A parameter space is loaded with ``YAMLParameterSpace``, giving the name of the YAML file and a
**parameter factory** for the encoding (``DoubleParameterFactory`` for continuous problems; its role
is explained in step 3):

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java
   :language: java
   :start-after: // [step-1-start]
   :end-before: // [step-1-end]
   :dedent: 4

The file is looked up in the classpath, first as given and then under ``parameterSpaces/``, and
finally in the file system, so a file name is enough for the bundled spaces, and a path works for
your own files. The **top-level parameters** are the keys at the root of the YAML file, in the
same order. For NSGA-II they are:

.. code-block:: none

   algorithmResult
   createInitialSolutions
   offspringPopulationSize
   variation
   selection

Types of parameters
-------------------

Each parameter has a ``type`` in the YAML file. There are three:

.. list-table::
   :header-rows: 1
   :widths: 15 35 50

   * - Type
     - YAML definition
     - Example from ``NSGAIIDouble.yaml``
   * - ``categorical``
     - ``values:``, a list or a map of choices
     - ``createInitialSolutions`` with values ``default``, ``latinHypercubeSampling``,
       ``scatterSearch``, …
   * - ``integer`` (or ``int``)
     - ``range: [min, max]``
     - ``selectionTournamentSize`` with range ``[2, 10]``
   * - ``double`` (or ``real``)
     - ``range: [min, max]``
     - ``crossoverProbability`` with range ``[0.0, 1.0]``

Some details worth knowing:

- A categorical parameter whose values are numbers, such as ``offspringPopulationSize`` with values
  ``[1, 5, 10, 20, 50, 100, 200, 400]``, only accepts those numbers.
- Integer and double parameters take any value in their range, and ``min`` must be strictly lower
  than ``max``. They cannot be defined with a list of values.
- There is no boolean type: yes/no choices are categorical parameters with values ``"true"`` and
  ``"false"``, like ``knnNormalizeObjectives`` in ``RDEMOEADouble.yaml``.

Relations between parameters
----------------------------

Parameters form a hierarchy. Besides the top-level parameters, a categorical parameter can have two
kinds of sub-parameters:

- **Global sub-parameters** apply whatever the value of their parent. Every crossover has a
  probability, so ``crossoverProbability`` is a global sub-parameter of ``crossover``.
- **Conditional parameters** apply only when their parent takes a specific value. The distribution
  index only makes sense for SBX, so ``sbxDistributionIndex`` is a conditional parameter of
  ``crossover`` for the value ``SBX``.

A parameter is **active** when it applies to the current configuration: top-level parameters are
always active, and a sub-parameter is active when its parent is active (and, for a conditional
parameter, when the parent has the right value). This is the fragment of ``NSGAIIDouble.yaml`` that
defines the crossover, nested inside the ``variation`` parameter:

.. code-block:: yaml

    variation:
      type: categorical
      values:
        crossoverAndMutationVariation:
          conditionalParameters:
            crossover:
              type: categorical
              globalSubParameters:
                crossoverProbability:
                  type: double
                  range: [0.0, 1.0]
                crossoverRepairStrategy:
                  type: categorical
                  values: [random, round, bounds]
              values:
                SBX:
                  conditionalParameters:
                    sbxDistributionIndex:
                      type: double
                      range: [5.0, 400.0]
                blxAlpha:
                  conditionalParameters:
                    blxAlphaCrossoverAlpha:
                      type: double
                      range: [0.0, 1.0]
                wholeArithmetic: {}
                # ...

The rules of the format follow from it:

- ``globalSubParameters`` hangs from a categorical parameter; ``conditionalParameters`` hangs from
  one of its values. To attach conditional parameters, the values must be written as a map
  (``SBX:``, ``blxAlpha:``, …) instead of a list. A value without sub-parameters can be written
  as ``wholeArithmetic: {}`` or just ``wholeArithmetic:``.
- Sub-parameters can be nested to any depth: ``sbxDistributionIndex`` depends on ``crossover``,
  which depends on ``variation``.
- Parameter names must be unique in the whole space, because the space identifies parameters by
  name. That is why the probabilities are called ``crossoverProbability`` and
  ``mutationProbabilityFactor`` rather than ``probability``.

Step 2: exploring the structure
-------------------------------

The tutorial class includes a small method, ``describe``, that walks the hierarchy and prints one
line per parameter, with its type and domain. Global sub-parameters are marked with ``*`` and
conditional parameters with the value that activates them, in brackets:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java
   :language: java
   :start-after: // [step-2-start]
   :end-before: // [step-2-end]
   :dedent: 4

This is the complete structure of NSGA-II for continuous problems:

.. code-block:: none

   algorithmResult (categorical: population, externalArchive)
     [externalArchive] populationSizeWithArchive (integer: [10, 200])
     [externalArchive] archiveType (categorical: crowdingDistanceArchive, unboundedArchive, spatialSpreadDeviationArchive, knnDistanceArchive, angleArchive)
       [knnDistanceArchive] knnDistanceArchiveK (integer: [1, 10])
   createInitialSolutions (categorical: default, latinHypercubeSampling, scatterSearch, sobol, cauchy, oppositionBased)
   offspringPopulationSize (categorical: 1, 5, 10, 20, 50, 100, 200, 400)
   variation (categorical: crossoverAndMutationVariation)
     [crossoverAndMutationVariation] crossover (categorical: SBX, blxAlpha, wholeArithmetic, blxAlphaBeta, arithmetic, laplace, fuzzyRecombination, PCX, UNDC, SDX)
       * crossoverProbability (double: [0.0, 1.0])
       * crossoverRepairStrategy (categorical: random, round, bounds)
       [SBX] sbxDistributionIndex (double: [5.0, 400.0])
       [blxAlpha] blxAlphaCrossoverAlpha (double: [0.0, 1.0])
       [blxAlphaBeta] blxAlphaBetaCrossoverBeta (double: [0.0, 1.0])
       [blxAlphaBeta] blxAlphaBetaCrossoverAlpha (double: [0.0, 1.0])
       [laplace] laplaceCrossoverScale (double: [0.1, 0.5])
       [fuzzyRecombination] fuzzyRecombinationCrossoverAlpha (double: [1.0E-4, 1.0])
       [PCX] pcxCrossoverZeta (double: [0.0, 1.0])
       [PCX] pcxCrossoverEta (double: [0.0, 1.0])
       [UNDC] undcCrossoverZeta (double: [0.1, 1.0])
       [UNDC] undcCrossoverEta (double: [0.1, 0.5])
       [SDX] sdxCrossoverF (double: [0.0, 1.0])
     [crossoverAndMutationVariation] mutation (categorical: uniform, polynomial, linkedPolynomial, nonUniform, levyFlight, powerLaw)
       * mutationProbabilityFactor (double: [0.0, 2.0])
       * mutationRepairStrategy (categorical: random, round, bounds)
       [uniform] uniformMutationPerturbation (double: [0.0, 1.0])
       [polynomial] polynomialMutationDistributionIndex (double: [5.0, 400.0])
       [linkedPolynomial] linkedPolynomialMutationDistributionIndex (double: [5.0, 400.0])
       [nonUniform] nonUniformMutationPerturbation (double: [0.0, 1.0])
       [levyFlight] levyFlightMutationBeta (double: [1.0001, 2.0])
       [levyFlight] levyFlightMutationStepSize (double: [0.01, 1.0])
       [powerLaw] powerLawMutationDelta (double: [0.1, 10.0])
   selection (categorical: tournament, random, boltzmann, ranking, stochasticUniversalSampling)
     [tournament] selectionTournamentSize (integer: [2, 10])
     [boltzmann] boltzmannTemperature (double: [0.1, 100.0])
   34 parameters, 5 of them top-level

The space has 34 parameters, of which only 5 are top-level. The total is the number of variables of
the *flat* encoding that meta-optimizers use to search the space (see :doc:`../concepts/solution_encoding`).
Most of them are inactive in any given configuration: with SBX, for instance, the parameters of the
other nine crossovers do not apply.

Step 3: the same algorithm with another encoding
------------------------------------------------

NSGA-II can also solve binary problems, and ``NSGAIIBinary.yaml`` is its parameter space for them.
It has exactly the same five top-level parameters, but the crossover and mutation operators are
different, because SBX or polynomial mutation make no sense on bit strings:

.. code-block:: yaml

    variation:
      type: categorical
      values:
        crossoverAndMutationVariation:
          conditionalParameters:
            crossover:
              type: categorical
              globalSubParameters:
                crossoverProbability:
                  type: double
                  range: [0.0, 1.0]
              values:
                HUX:
                uniform:
                singlePoint:
            mutation:
              type: categorical
              globalSubParameters:
                mutationProbabilityFactor:
                  type: double
                  range: [0.0, 2.0]
              values:
                bitFlip:

The YAML file only states which values each parameter can take. Turning ``SBX`` or ``HUX`` into
an actual jMetal operator is the job of the **parameter factory**: when the space is loaded, the
factory creates, for each categorical parameter, the class that knows how to build its component
for that encoding. The step loads both spaces and prints the class behind ``crossover``:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java
   :language: java
   :start-after: // [step-3-start]
   :end-before: // [step-3-end]
   :dedent: 4

.. code-block:: none

   DoubleCrossoverParameter with values [SBX, blxAlpha, wholeArithmetic, blxAlphaBeta, arithmetic, laplace, fuzzyRecombination, PCX, UNDC, SDX]
   BinaryCrossoverParameter with values [HUX, uniform, singlePoint]

``DoubleParameterFactory``, ``BinaryParameterFactory`` and ``PermutationParameterFactory`` (in
``org.uma.evolver.parameter.factory``) do the same for mutation, initialization, selection and the
other components. The factory must match the encoding of the YAML file.

Step 4: from a parameter space to a configuration
-------------------------------------------------

A configuration is written as a **configuration string**: a sequence of ``--name value`` pairs,
one for every active parameter. The step parses a standard NSGA-II configuration (SBX and
polynomial mutation, binary tournament) by asking each top-level parameter to read its value; each
parameter then reads its active sub-parameters, recursively:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java
   :language: java
   :start-after: // [step-4-start]
   :end-before: // [step-4-end]
   :dedent: 4

The method ``describeActive`` prints the parameters that the configuration activates, with their
values:

.. code-block:: none

   algorithmResult = population
   createInitialSolutions = default
   offspringPopulationSize = 100
   variation = crossoverAndMutationVariation
     crossover = SBX
       crossoverProbability = 0.9
       crossoverRepairStrategy = bounds
       sbxDistributionIndex = 20.0
     mutation = polynomial
       mutationProbabilityFactor = 1.0
       mutationRepairStrategy = bounds
       polynomialMutationDistributionIndex = 20.0
   selection = tournament
     selectionTournamentSize = 2

Only 14 of the 34 parameters are active. Parameters that are not active do not need to appear in the
configuration string, and a value given for an inactive parameter (say
``--blxAlphaCrossoverAlpha 0.5`` in this configuration) is simply ignored. If a parameter appears
twice, its first occurrence is used.

This is exactly what configuring an algorithm in Evolver means: the ``parse`` method of an
algorithm such as ``DoubleNSGAII`` runs this same loop over its parameter space, and the algorithm
is then assembled from the chosen components (:doc:`tutorial E2 <base_level_algorithms>`).

Step 5: wrong configurations
----------------------------

A configuration is checked while it is parsed. The step parses two wrong variants of the previous
configuration: one without the distribution index that SBX requires, and one that asks for the
binary crossover ``HUX`` in the continuous space:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/tutorial/ParameterSpacesTutorial.java
   :language: java
   :start-after: // [step-5-start]
   :end-before: // [step-5-end]
   :dedent: 4

.. code-block:: none

   Rejected: Missing parameter: --sbxDistributionIndex
   Rejected: Parameter crossover: Invalid value: HUX. Valid values: [SBX, blxAlpha, wholeArithmetic, blxAlphaBeta, arithmetic, laplace, fuzzyRecombination, PCX, UNDC, SDX]

Both are rejected with a message that names the problem. The first one shows that a conditional
parameter becomes mandatory as soon as its value is chosen.

Running the tutorial
--------------------

Run ``ParameterSpacesTutorial`` from your IDE, or build the project and run it from the command
line, from the root of the Evolver repository:

.. code-block:: bash

    mvn -DskipTests package
    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.example.tutorial.ParameterSpacesTutorial

Try it yourself
---------------

- Describe other parameter spaces: ``NSGAIIPermutation.yaml`` (with ``PermutationParameterFactory``)
  or ``MOEADDouble.yaml``. Which top-level parameters does MOEA/D add?
- Change the configuration of step 4 to use an external archive
  (``--algorithmResult externalArchive``). Which parameters become mandatory? Check your answer
  with the output of step 2, then parse it.
- Copy ``NSGAIIDouble.yaml`` to a file of your own, leave only SBX and polynomial mutation, and load
  it by its path. How many parameters does the reduced space have?

What's next
-----------

- :doc:`E2. Base-level algorithms <base_level_algorithms>`: configuring and running NSGA-II and
  other algorithms from a parameter space.
- :doc:`../concepts/parameter_spaces` covers parameter spaces in more depth, including how they are
  implemented.
