.. _introduction:

Introduction
============

Evolver is a Java-based framework for automatically configuring multi-objective metaheuristics through meta-optimization. It formulates the algorithm configuration problem as a multi-objective optimization task, leveraging the jMetal framework's extensive collection of algorithms, benchmark problems, and quality indicators.

Key Concepts
------------

Meta-Optimization Approach
~~~~~~~~~~~~~~~~~~~~~~~~~~
.. figure:: figures/metaOptimizationApproach.png
   :align: center
   :alt: Meta-optimization approach
   :figwidth: 50%

   Overview of Evolver's meta-optimization approach

Evolver automates the configuration of metaheuristics by treating algorithm configuration as a multi-objective optimization problem, using quality indicators as objectives to minimize, and supporting various meta-optimization algorithms for configuration search.

For detailed explanation, see :doc:`concepts/meta_optimization_approach`.

Parameter Space
~~~~~~~~~~~~~~~
Configuration spaces in Evolver are defined using YAML files, allowing for various parameter types (integer, double, categorical), conditional parameters, and global sub-parameters.

Example (simplified):

.. code-block:: yaml

    algorithmResult:
      type: categorical
      values: 
        population: {}
        externalArchive: {}

See :doc:`concepts/parameter_spaces` for comprehensive documentation.

Solution Encoding & Evaluation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
- Solutions are encoded as real-valued vectors in [0.0, 1.0]
- Evaluation involves running the configured algorithm on target problems
- Multiple quality indicators can be used as objectives

Refer to :doc:`concepts/solution_encoding` and :doc:`concepts/evaluation` for details.

Getting Started
---------------
- :doc:`Installation Guide </installation>`
- :doc:`Quick Start </getting_started>`
- :doc:`Examples </examples>`

Next Steps
----------
- Learn about :doc:`available meta-optimizers </meta_optimizers>`
- Explore :doc:`advanced configuration </user_guide/advanced_configuration>`
- Check out the :doc:`API reference </api_reference>`