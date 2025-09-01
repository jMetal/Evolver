.. _parameter-spaces:

Parameter Spaces
===============

Parameter spaces in Evolver define the configuration space for metaheuristics using YAML files. These files specify the parameters, their types, and possible values that can be tuned during the meta-optimization process.

Basic Structure
---------------
A parameter space is defined by a set of parameters, each with:
- A unique name
- A type (integer, double, categorical)
- A definition of valid values
- Optional constraints and conditions

Parameter Types
---------------

Integer Parameters
~~~~~~~~~~~~~~~~~~
.. code-block:: yaml

    parameterName:
      type: integer
      range: [min, max]  # Inclusive range

Double Parameters
~~~~~~~~~~~~~~~~
.. code-block:: yaml

    parameterName:
      type: double
      range: [min, max]  # Inclusive range

Categorical Parameters
~~~~~~~~~~~~~~~~~~~~~
.. code-block:: yaml

    parameterName:
      type: categorical
      values:
        value1: {}
        value2: {}

Conditional Parameters
---------------------
Parameters that only apply when a parent parameter has a specific value:

.. code-block:: yaml

    parentParameter:
      type: categorical
      values:
        value1: {}
        value2:
          conditionalParameters:
            childParameter:
              type: integer
              range: [1, 10]

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
