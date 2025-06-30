
Evolver Documentation
===========================

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
