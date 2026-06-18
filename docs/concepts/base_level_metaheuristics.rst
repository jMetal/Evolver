.. _base-level-metaheuristics:

Base-Level Metaheuristics
=========================

Base-level metaheuristics in Evolver are multi-objective optimization algorithms that can be automatically configured through the meta-optimization process. This section describes the how to implement the algorithms, the provided solveres, and  their configuration options.

The base-level metaheuristics are implemented in the ``org.uma.evolver.algorithm.base`` package. This package contains the ``BaseLevelAlgorithm`` interface that defines the contract for all configurable metaheuristics in Evolver.

Supported Algorithms
--------------------

The following algorithms are currently available as configurable base-level metaheuristics:

.. list-table::
   :header-rows: 1
   :widths: 20 15 65

   * - Algorithm
     - Encodings
     - Notes
   * - NSGA-II
     - Double, Binary, Permutation
     - Classic non-dominated sorting GA with crowding distance
   * - NSGA-III
     - Double
     - Reference-point-based many-objective variant of NSGA-II
   * - MOEA/D
     - Double
     - Decomposition-based EA; supports multiple aggregation functions and DE variants
   * - SMS-EMOA
     - Double, Binary, Permutation
     - Hypervolume-indicator-based steady-state EA
   * - MOPSO
     - Double
     - Multi-objective particle swarm optimizer
   * - RDEMOEA
     - Double, Permutation
     - Ranking- and density-estimator-based EA with differential evolution variation
   * - RVEA
     - Double
     - Reference-vector-guided EA
   * - AGE-MOEA
     - Double
     - Adaptive geometry estimation-based MOEA
   * - SSMOEA
     - Double
     - Steady-state MOEA (offspring population size fixed at 1); see :ref:`ssmoea` below
   * - PAES
     - Double
     - Pareto Archived Evolution Strategy (1+1 ES with archive-based density tiebreaking); see :ref:`paes` below

BaseLevelAlgorithm Interface
----------------------------

The ``BaseLevelAlgorithm`` interface serves as the foundation for all configurable metaheuristics. It provides the necessary methods to:

1. Define and access the parameter space of the algorithm
2. Build configured algorithm instances
3. Create new instances with different problem configurations
4. Parse arguments for parameter configuration

Key characteristics:

- **Generic Type Parameter**: ``<S extends Solution<?>>`` ensures type safety for the solutions managed by the algorithm
- **Immutable Configuration**: The interface encourages immutable configuration through the builder pattern
- **Parameter Space Integration**: Tightly integrated with the `ParameterSpace` class for flexible parameter management

Here's the interface definition with detailed method documentation:

.. code-block:: java

  /**
   * Interface representing a configurable evolutionary algorithm.
   * 
   * @param <S> the solution type handled by the algorithm
   */
  public interface BaseLevelAlgorithm<S extends Solution<?>> {
    
    /**
     * Returns the parameter space associated with this algorithm.
     */
    ParameterSpace parameterSpace();

    /**
     * Builds and returns a configured {@link Algorithm} instance.
     */
    Algorithm<List<S>> build();

    /**
     * Creates a new instance of the algorithm for the given problem and maximum number of evaluations.
     */
    BaseLevelAlgorithm<S> createInstance(Problem<S> problem, int maximumNumberOfEvaluations);

    /**
     * Parses the given arguments and configures all top-level parameters in the parameter space.
     * The arguments should be provided as an array of strings in the format
     * ["--param1", "value1", "--param2", "value2", ...].
     * Returns {@code this} for fluent usage.
     *
     * @param args the arguments to parse, in the format ["--param1", "value1", "--param2", "value2", ...]
     * @return this algorithm instance, configured according to the arguments
     */
    default BaseLevelAlgorithm<S> parse(String[] args) {
      for (Parameter<?> parameter : parameterSpace().topLevelParameters()) {
        parameter.parse(args);
      }
      return this;
    }
  }


.. _ssmoea:

SSMOEA — Steady-State MOEA
---------------------------

``DoubleSSMOEA`` is a configurable steady-state multi-objective evolutionary algorithm. Its
offspring population size is fixed at 1 (non-configurable): the algorithm generates one candidate
solution per iteration, evaluates it, and decides immediately whether to incorporate it into the
population.

Architecture
~~~~~~~~~~~~

The algorithm is implemented in two classes:

- ``BaseSSMOEA<S>`` — abstract base class handling component assembly.
- ``DoubleSSMOEA`` — concrete subclass for double-encoded problems; sets
  problem-specific non-configurable parameters (number of variables, etc.).

Variation branches
~~~~~~~~~~~~~~~~~~

SSMOEA supports two variation strategies, selected via the ``variation`` parameter:

**crossoverAndMutationVariation**
  Standard recombination. A parent is selected from the population using ``gaSelection``
  (tournament, random, Boltzmann, ranking, or SUS). The selected parent is crossed with a
  second parent and then mutated.

**differentialEvolutionVariation**
  Differential evolution crossover (``RAND_1_BIN``, ``RAND_1_EXP``, ``RAND_2_BIN``,
  ``RAND_2_EXP``). Requires ``sequenceGenerator`` (``randomPermutationCycle`` or
  ``cyclicIntegerSequence``) and the flag ``takeCurrentSolutionAsParent``.
  All three DE components (variation, selection, replacement) share a single
  ``SequenceGenerator`` instance so they operate on the same population index each step.

Replacement strategies
~~~~~~~~~~~~~~~~~~~~~~

**rankingAndDensityEstimator**
  The offspring is merged with the population and the combined set is reduced back to
  ``populationSize`` using non-dominated sorting and crowding distance (or the configured
  density estimator). For a population of size *N* with one offspring, this is equivalent
  to NSGA-II steady-state: the worst solution is evicted.

**singleSolutionReplacement**
  One-to-one DEMO-style comparison. The offspring competes only against the individual at
  the current sequence generator index. The incumbent is replaced only if the offspring
  strictly dominates it (``DefaultDominanceComparator``).

Parameter space
~~~~~~~~~~~~~~~

The parameter space is defined in ``SSMOEADouble.yaml`` (43 parameters, 6 top-level):

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Top-level parameter
     - Description
   * - ``algorithmResult``
     - Whether the result is the final population or an external archive
   * - ``createInitialSolutions``
     - Initialisation strategy (default, Latin hypercube, scatter search)
   * - ``ranking``
     - Ranking method (dominance ranking, strength ranking)
   * - ``densityEstimator``
     - Density estimator (crowding distance, k-NN, shifted, angle)
   * - ``variation``
     - Variation branch and all its sub-parameters
   * - ``replacement``
     - Replacement strategy (``rankingAndDensityEstimator``, ``singleSolutionReplacement``)

Usage example
~~~~~~~~~~~~~

.. code-block:: java

  var problem = new ZDT1();
  var ssmoea = new DoubleSSMOEA(
      problem, 100, 25000,
      new YAMLParameterSpace("SSMOEADouble.yaml", new DoubleParameterFactory()));

  var args = ("--algorithmResult population "
      + "--createInitialSolutions default "
      + "--ranking dominanceRanking "
      + "--densityEstimator crowdingDistance "
      + "--variation crossoverAndMutationVariation "
      + "--crossover SBX --crossoverProbability 0.9 "
      + "--crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 "
      + "--gaSelection tournament --selectionTournamentSize 2 "
      + "--mutation polynomial --mutationProbabilityFactor 1.0 "
      + "--mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 "
      + "--replacement rankingAndDensityEstimator").split("\\s+");

  var algorithm = ssmoea.parse(args).build();
  algorithm.run();
  List<DoubleSolution> result = algorithm.result();


.. _paes:

PAES — Pareto Archived Evolution Strategy
------------------------------------------

``DoublePAES`` is a configurable PAES algorithm. It follows a 1+1 evolution strategy: the
population contains exactly one solution (non-configurable), and one offspring is created each
iteration by mutation only. A bounded archive serves as both the density estimator for
non-dominated tiebreaking and the main result container.

The bounded archive size (``numberOfSolutionsToFind``) is fixed and provided through the
constructor — analogous to the population size of other algorithms (typical value: 100) — rather
than being part of the tunable parameter space.

Architecture
~~~~~~~~~~~~

The algorithm is implemented in two classes:

- ``BasePAES<S>`` — abstract base class handling component assembly.
- ``DoublePAES`` — concrete subclass for double-encoded problems.

Three new components are introduced:

- ``MutationOnlyVariation<S>`` — implements ``Variation<S>``; copies and mutates the single
  mating pool member without any crossover.
- ``PAESSelection<S>`` — implements ``Selection<S>``; chooses the mutation parent as either the
  current solution or a random archive member (see *Parent selection* below).
- ``PAESReplacement<S>`` — implements ``Replacement<S>``; applies the three-way PAES rule.

Replacement logic
~~~~~~~~~~~~~~~~~

On each iteration:

1. **Offspring dominates current** → accept offspring, add to archive.
2. **Current dominates offspring** → keep current (no change).
3. **Neither dominates** → try adding offspring to archive; if accepted, use the archive's
   density comparator to choose the solution in the less-crowded region.

Parent selection
~~~~~~~~~~~~~~~~

The ``archiveSelectionProbability`` parameter (range [0.0, 1.0]) controls how the mutation parent
is chosen each iteration, analogous to MOEA/D's ``neighborhoodSelectionProbability``:

- with this probability, a randomly chosen member of the PAES archive is mutated;
- otherwise the current solution is mutated.

A value of ``0.0`` reproduces classic PAES (always mutate the current solution).

Archive types
~~~~~~~~~~~~~

The PAES archive type is set via the ``paesArchiveType`` parameter. All options are bounded
archives that provide a density estimator for the non-dominated tiebreaking step:

.. list-table::
   :header-rows: 1
   :widths: 35 65

   * - Value
     - Description
   * - ``crowdingDistanceArchive``
     - Bounded archive pruned by crowding distance
   * - ``hypervolumeArchive``
     - Bounded archive pruned by hypervolume contribution
   * - ``spatialSpreadDeviationArchive``
     - Bounded archive pruned by angular spread deviation

Parameter space
~~~~~~~~~~~~~~~

The parameter space is defined in ``PAESDouble.yaml`` (14 parameters, 5 top-level):

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Top-level parameter
     - Description
   * - ``paesArchiveType``
     - Density archive type (``crowdingDistanceArchive``, ``hypervolumeArchive``,
       ``spatialSpreadDeviationArchive``)
   * - ``algorithmResult``
     - ``paesArchive`` (returns the bounded archive contents) or ``externalArchive`` (returns an
       additional unbounded non-dominated archive)
   * - ``archiveSelectionProbability``
     - Probability of mutating a random archive member instead of the current solution
   * - ``createInitialSolutions``
     - Initialisation strategy (default, Latin hypercube, scatter search)
   * - ``mutation``
     - Mutation operator and its sub-parameters (uniform, polynomial, linked polynomial, etc.)

Usage example
~~~~~~~~~~~~~

.. code-block:: java

  var problem = new ZDT1();
  int numberOfSolutionsToFind = 100;
  var paes = new DoublePAES(
      problem, numberOfSolutionsToFind, 25000,
      new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));

  var args = ("--paesArchiveType crowdingDistanceArchive "
      + "--algorithmResult paesArchive "
      + "--archiveSelectionProbability 0.0 "
      + "--createInitialSolutions default "
      + "--mutation polynomial --mutationProbabilityFactor 1.0 "
      + "--mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0")
      .split("\\s+");

  var algorithm = paes.parse(args).build();
  algorithm.run();
  List<DoubleSolution> result = algorithm.result(); // archive contents



