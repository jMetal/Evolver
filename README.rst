Project Evolver 
===============

Project aimed at studying how to use jMetal algorithms to optimize/design multi-objective algorithms.

Things to think about:

- Define a single ``ConfigurableAlgorithmProblem`` class that must receive as input a space parameter, an algorithm able of being configured with a string representing any valid configuration, a problem to optimize, and a list of quality indicators to be optimized. 
- The solution space is composed by valid configurations (i.e, a parameter list with values) and the objective spaces is defined the selected quality indicators.
- The ``ConfigurableAlgorithmProblem`` will be a continuous problem to allow to use most of the algorithms included in jMetal. 
  
