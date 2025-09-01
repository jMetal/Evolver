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

The meta-optimization approach follows the scheme shown in the figure below. The goal is to find the best configuration of a base-level multi-objective metaheuristic to efficiently solve a set of base-level problems, which we call the training set. This goal is formulated as a meta-optimization problem, where the solution space is composed of algorithm configurations for the base-level optimizer, and the objective space is defined by set of quality indicators that must be minimized.

For detailed explanation, see :doc:`concepts/meta_optimization_approach`.

Parameter Space
~~~~~~~~~~~~~~~
The concept of *parameter space* is key in Evolver, as it defines the space of possible configurations of a base-level metaheuristic. Configuration spaces in Evolver are defined using YAML files, allowing for various parameter types (integer, double, categorical), conditional parameters, and global sub-parameters.

Example of a parameter:

.. code-block:: yaml

    algorithmResult:
      type: categorical
      values: 
        population: {}
        externalArchive: {}

See :doc:`concepts/parameter_spaces` for comprehensive documentation.

Solution Encoding & Evaluation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Evolver encodes all parameters of a given configuration in a vector of real values in the range [0.0, 1.0]. Evaluating a solution requires decoding the real values into the corresponding parameters in order to configure the base algorithm, run it on the base-level problems, and compute the quality indicators for the obtained solutions.

Refer to :doc:`concepts/solution_encoding` and :doc:`concepts/evaluation` for further details.

Objective Functions
~~~~~~~~~~~~~~~~~~~

jMetal provides a wide range of quality indicators that measure the degree of convergence and/or diversity of a Pareto front approximation obtained by a multi-objective metaheuristic, such as additive epsilon (EP), inverted generational distance (IGD), spread (SP), or hypervolume (HV).

As mentioned before, the objective functions of the meta-optimization problem are based on a list of the desired quality indicators. All quality indicators used as objective functions are intended to be minimized in Evolver. 

Additional information can be found in :doc:`concepts/objective_functions`.

Base-Level metaheuristics
~~~~~~~~~~~~~~~~~~~~~~~~~

A base-level metaheuristic is a multi-objective algorithm that must be configured from any given valid configuration of its parameter space. As a consequence, the existing algorithms in jMetal cannot be used as provided in that framework because their implementation is not generic enough. 

Evolver includes a set of algorithms that have been modified to be used as base-level metaheuristics. See :doc:`concepts/base_level_metaheuristics` for more information.

Meta-Optimizers 
~~~~~~~~~~~~~~~
As previously mentioned, choosing a real encoding for the meta-optimizer allows the use of most multi-objective metaheuristics available in jMetal, including evolutionary algorithms (NSGA-II, MOEA/D, SMS-EMOA, SPEA2, etc.), differential evolution (GDE3, MOEA/D-DE) and particle swarm optimization algorithms (OMOPSO, SMPSO).

Some of these algorithms can evaluate the population or swarm in parallel using a synchronous parallel scheme to speed up execution. For NSGA-II, a more efficient asynchronous parallel version is also available. Using parallel meta-optimizers is highly desirable as a meta-optimization can take a long time to complete, and parallelization can significantly reduce the total running time.

Next Steps
----------
- Learn about :doc:`available meta-optimizers </meta_optimizers>`
- Explore :doc:`advanced configuration </user_guide/advanced_configuration>`
- Check out the :doc:`API reference </api_reference>`