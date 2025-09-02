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

A conditional parameter is a parameter that only applies when a parent categorical parameter has a specific value. Let us consider the following example:

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

Global Sub-Parameters
--------------------
Parameters that always apply, regardless of other parameter values:

.. code-block:: yaml

    mainParameter:
      type: categorical
      values:
        value1: {}
        value2: {}
      globalSubParameters:
        globalParam:
          type: double
          range: [0.0, 1.0]

Example: NSGA-II Parameter Space
-------------------------------
A simplified example of NSGA-II's parameter space:

.. literalinclude:: ../../src/main/resources/parameterSpaces/NSGAIIDouble.yaml
   :language: yaml
   :lines: 1-20
   :caption: NSGAIIDouble.yaml (excerpt)

Best Practices
--------------
1. **Start Simple**: Begin with essential parameters
2. **Use Meaningful Names**: Make parameter names descriptive
3. **Define Reasonable Ranges**: Set appropriate min/max values
4. **Document Parameters**: Add comments in the YAML file
5. **Test Configurations**: Verify that example configurations work

For more examples, see the `parameterSpaces <https://github.com/jMetal/Evolver/tree/main/src/main/resources/parameterSpaces>`_ directory in the source code.
