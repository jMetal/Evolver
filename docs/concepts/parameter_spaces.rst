.. _parameter-spaces:

Parameter Spaces
===============

Parameter spaces in Evolver define the whole set of parameters that can be configured in an automatic way for base-level metaheuristics. Parameters can be categorical, such as the crossover operator in evolutionary algorithms, or numerical, such as the mutation probability (double parameter) or the offspring population size (integer parameter). 

This document describes the structure of a parameter space and how they can be defined in YAML files.

Parameters in Evolver
---------------------
A parameter is defined by:
- A unique name
- A type (integer, double, categorical)
- A definition of valid values
- Relationships with other parameters (conditional parameters and global sub-parameters)

By convention, the name of a parameter should be self-explanatory and defined in camel case such as, for example, ``offspringPopulationSize`` or ``mutationProbability``.

Numeric parameter types (integer and double) define a range of valid values using the ``range`` attribute, which is an inclusive range:

.. code-block:: yaml

  neighborhoodSize:
    type: integer
    range: [5, 50]  # Inclusive range

  laplaceCrossoverScale:
    type: double
    range: [0.1, 1.5] # Inclusive range

Categorical parameters define a set of valid values using the ``values`` attribute, and can be defined in two ways:

.. code-block:: yaml

    createInitialSolutions:
      type: categorical
      values:
        default: {}
        latinHypercubeSampling: {}
        scatterSearch: {}

    offspringPopulationSize:
      type: categorical
      values: [1, 2, 5, 10, 20, 50, 100, 200, 400]

The first option is useful when the parameter can have conditional parameters. If this is not the case, the second option is more compact and concise.

Conditional Parameters
~~~~~~~~~~~~~~~~~~~~~~

In the parameter space, a parameter can have conditional parameters. Conditional parameters are parameters that only apply when a parent categorical parameter has a specific value. 

Let's consider the following example to illustrate this concept:

.. code-block:: yaml

  algorithmResult:
    type: categorical
    values: 
      population: {}
      externalArchive:
        conditionalParameters:
          populationSizeWithArchive:
            type: integer
            range: [10, 200]
          archiveType:
            type: categorical
            values:
              crowdingDistanceArchive: {}
              unboundedArchive: {}

The ``algorithmResult`` parameter is a categorical parameter that can take two values: ``population`` or ``externalArchive``. If the value is ``population``, then the ``populationSizeWithArchive`` parameter is not considered. If the value is ``externalArchive``, then the ``populationSizeWithArchive`` and ``archiveType`` parameters are considered. The ``archiveType`` parameter is a categorical parameter that can take two values: ``crowdingDistanceArchive`` or ``unboundedArchive``.

Global Sub-Parameters
~~~~~~~~~~~~~~~~~~~~~

Besides conditional parameters, a categorical parameter can have also global sub-parameters. Global sub-parameters are parameters that always apply, regardless of other parameter values. Let's take a look to the next example:

.. code-block:: yaml

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
          conditionalParameters:
        blxAlphaCrossoverAlpha:
              type: double
              range: [0.0, 1.0]
        wholeArithmetic: {}

The ``crossover`` parameter is a categorical parameter that can take three values: ``SBX``, ``blxAlpha``, or ``wholeArithmetic``. In contrast to conditional parameters, global sub-parameters always applies. In the example, any crossover has a ``crossoverProbability`` and ``crossoverRepairStrategy`` parameter. We can see that the ``SBX``and ``blxAlpha`` crossovers have a ``sbxDistributionIndex`` and ``blxAlphaCrossoverAlpha`` parameter, respectively, while the ``wholeArithmetic`` crossover does not have these parameters.   

First-Level Parameters in a Parameter Space
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The first-level parameters in a parameter space are the root nodes of a hierarchy, and they do not have parents. The rest of parameters of the parameter space are children of these first-level parameters, which can be either conditional or global sub-parameters.

If we take a look to the `parameter space for NSGA-II for double problems <https://github.com/jMetal/Evolver/blob/main/src/main/resources/parameterSpaces/NSGAIIDouble.yaml>`_, we can observe that the number of first-level parameters is five:

- algorithmResult
- createInitialSolutions
- offspringPopulationSize
- variation
- selection

However, the first-level parameters of the `base-level MOEA/D parameter space <https://github.com/jMetal/Evolver/blob/main/src/main/resources/parameterSpaces/MOEADouble.yaml>`_ are eight:

- neighborhoodSize
- maximumNumberOfReplacedSolutions
- aggregationFunction
- algorithmResult
- createInitialSolutions
- subProblemIdGenerator
- variation
- selection

For more examples, see the `parameterSpaces <https://github.com/jMetal/Evolver/tree/main/src/main/resources/parameterSpaces>`_ directory in the source code.
