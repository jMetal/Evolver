Project Evolver 
===============

Project aimed at studying how to use jMetal algorithms to optimize/design multi-objective algorithms.

Things to think about:

- Define a single ``ConfigurableAlgorithmProblem`` class that will receive as input a space parameter, an algorithm able of being configured with a string representing any valid configuration, a problem to optimize, and a list of quality indicators to be optimized. The list could contain a single indicator (i.e., the problem would be single-objective) or more than one (for example, maximize HV and minimize the number of evaluations to get an aspiration point.)
- The solution space is composed by valid configurations (i.e, a parameter list with values) and the objective spaces is defined the selected quality indicators.
- The ``ConfigurableAlgorithmProblem`` will be a continuous problem to allow to use most of the algorithms included in jMetal. We must cope with parameter that will not be continuous; some of them can be integer and others can be categorical. The approach to be adopted to convert them as continuous parameters can be the same applied in ML algorithms.
- The stopping condition can be a list of increased maximum evaluation number thresholds so that a single run can allow to optimize the problem until, for example, 10000, 25000, 50000, and 100000 evaluations.
- The running of the algorithm can be configured with different levels of logging data. The log information will be semantically annotated and stored in files.
- The first prototype of the project would use AutoNSGA-II to configure AutoNSGA-II when solving the ZDT1 problem and optimizing a) only the HV and b) the HV and the IGD+.
- A major issue would be the high computing time, so it would necessary to apply parallelism.


More complex goals:

- We should cope with the feature of allowing to carry out a number of independent runs, so that the final solution would be that corresponding to the median value of the quality indicator used for performance assessment. 
- Optionally, we can assume that the reference Pareto front is unknown beforehand, so a reference front can be computed on the fly by using the found solutions in every iteration of the algorithm.
- Our goal is not to compete against irace, but it could be interesting to make some comparisons.

Sample configuration
--------------------

The following example can use a generic NSGAII to auto-design the NSGAII algorithm to solve the ZDT1, ZDT4, and DTLZ3 problems.

.. code-block:: yaml

    external_algorithm_arguments:
      meta_optimizer_algorithm: NSGAII
      meta_optimizer_population: 50
      meta_optimizer_max_evaluations: 3000
      independent_runs: 3
      indicators_names: NormalizedHypervolume,Epsilon
      output_directory: TEST/DIRECTORY

    internal_algorithm_arguments:
      configurable_algorithm: NSGAII
      population: 100
      problem_names: ZDT1,ZDT4,DTLZ3
      reference_front_file_name: resources/referenceFronts/ZDT1.csv,resources/referenceFronts/ZDT4.csv,resources/referenceFronts/ZDT4.csv,resources/referenceFronts/DTLZ3.csv
      max_number_of_evaluations: 8000,16000,20000
