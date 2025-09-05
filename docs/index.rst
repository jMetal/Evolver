
Evolver Documentation
=====================

*Author: Antonio J. Nebro* <ajnebro@uma.es>

DISCLAIMER: we are developing a new version of Evolver (version 2.0). The package is
ready to be used but we are still working on the documentation.

Evolver is a software tool designed for the automatic configuration of multi-objective metaheuristics. 
Its core approach is meta-optimization, where the process of tuning the parameters of a base-level metaheuristic is framed as a multi-objective problem which
is solvable by a multi-objective optimizer (i.e., the meta-optimization algorithm). In this problem, the variable encoding represents a particular configuration 
of the base-level algorithm and 
evaluating a solution involves a run of the metaheuristic under that configuration; the resulting solution front is evaluated against a combination of 
quality indicators, which are the objective functions of the resulting multi-objective problem.

The current stable version is 2.0 (https://github.com/jMetal/Evolver). The working version in GitHub is 2.1-SNAPSHOT.

Evolver 2.0 is a full re-implementation of the original Evolver framework, which is described in the following paper: `Evolver: Meta-optimizing multi-objective metaheuristics <https://doi.org/10.1016/j.softx.2023.101551>`_.

The development of Evolver 2.0 was motivated by two key objectives: enhancing the original framework's capabilities and serving as a case study in AI-assisted software development. Throughout this project, we've extensively utilized generative AI tools including Windsurf, ChatGPT, and Claude to support various aspects of the development process.

Architecture  
^^^^^^^^^^^^
Evolver follows a two-level optimization architecture:

.. code-block:: none

    +---------------------------------------------+
    |        Meta-optimization Level              |
    |  +---------------------------------------+  |
    |  | Meta-optimization Algorithm           |  |
    |  | (e.g., NSGA-II, MOEA/D)               |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    |  +------------------v--------------------+  |
    |  | Meta-optimization Problem             |  |
    |  | - Evaluates base-level configurations |  |
    |  | - Uses quality indicators as          |  |
    |  |   optimization objectives             |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    +---------------------|-----------------------+
                          |
    +---------------------|-----------------------+
    |  Base-level         v                       |
    |  +------------------+--------------------+  |
    |  | Base-level Metaheuristic              |  |
    |  | - Parameter space                     |  |
    |  | - Solves base-level problems          |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    |  +------------------v--------------------+  |
    |  | Base-level Problems                   |  |
    |  | (Training instances)                  |  |
    |  +---------------------------------------+  |
    |                                             |
    +---------------------------------------------+

The components of the architecture are:

1. **Base-level Components**

   * A set of **Base-level Problems** serving as training instances
   * A **Base-level Multi-objective Metaheuristic** to be configured
   * A parameter space defining the algorithm's configurable parameters and their constraints

2. **Meta-level Components**

   * A **Meta-optimization Problem** that evaluates base-level configurations
   * Quality indicators (e.g., Epsilon, normalized Hypervolume, etc.) as optimization objectives
   * A **Meta-optimization Multi-objective Metaheuristic** that searches for optimal configurations of a meta-optimization problem


The flow is as follows:

1. Given a set of base-level problems, the meta-optimization algorithm generates configurations for a base-level metaheuristic
2. Each configuration is evaluated in the meta-optimization problem using quality indicators
3. The process repeats until stopping criteria are met


Key Features
^^^^^^^^^^^^
- **Automated Configuration**: Automatically finds accurate parameter settings for metaheuristics
- **Flexible Architecture**: Supports various metaheuristics at both meta and base levels and several encodings (double, binary, permutation, etc.)
- **Multi-objective Optimization at the meta level**: Optimizes multiple performance criteria (quality indicators) simultaneously
- **Extensible Design**: Allows the integration of new algorithms, problems, and quality indicators
- **YAML Parameter Space Definition**: The parameter space of base-level metaheuristics can be defined in a YAML file

Other Features
^^^^^^^^^^^^
- **irace Support**: The search of base-level metaheuristic configurations can be performed with irace.

Available algorithms
--------------------
Evolver currently supports the following base-level and meta-optimization algorithms:

- Base-optimization algorithms:

  - NSGA-II (double, binary, permutation encodings)
  - MOEA/D (double, binary, permutation encodings)
  - SMS/EMOA (double encoding)
  - MOPSO -- multi-objective particle swarm optimization -- (double encoding)
  - RDEMOEA -- ranking and density estimator MOEA -- (double, permutation encoding)

- Meta-optimization algorithms:

  - NSGA-II
  - Async NSGA-II
  - SMPSO


.. toctree::
   :maxdepth: 2
   :caption: Getting Started
   :name: start

   introduction
   installation
   quick_start
   examples/index

.. toctree::
   :maxdepth: 2
   :caption: BASICS
   :name: basics

   concepts/index
   project_structure
   parameter_spaces

.. toctree::
   :maxdepth: 2
   :caption: ADVANCED
   :name: advanced

   irace
   


