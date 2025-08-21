
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


.. toctree::
   :maxdepth: 2
   :caption: Contents:
   :numbered:

   introduction
   installation
   getting_started
   user_guide/index
   api_reference
   examples/index
   contributing
   changelog
   faq

Architecture Overview
====================

Evolver implements a two-level optimization architecture:

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
    |  | Base-level Metaheuristic             |  |
    |  | - Configurable parameters            |  |
    |  | - Solves base-level problems         |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    |  +------------------v--------------------+  |
    |  | Base-level Problems                   |  |
    |  | (Training instances)                  |  |
    |  +---------------------------------------+  |
    |                                             |
    +---------------------------------------------+

Flow:
1. Meta-optimization algorithm generates configurations
2. Each configuration is evaluated on base-level problems
3. Performance metrics are fed back to the meta-level
4. The process repeats until stopping criteria are met

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
