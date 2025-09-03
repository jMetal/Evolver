.. _project_structure:

Project Structure
================

This document provides an overview of the Evolver project's directory structure and key components.

Directory Layout
---------------

.. code-block:: text

    Evolver/
    ├── docs/                # Project documentation
    ├── resources/           # Resource files including parameter spaces and reference fronts
    │   ├── parameterSpaces/ # YAML files defining algorithm parameter spaces
    │   ├── referenceFronts/ # Reference Pareto fronts for benchmark problems
    │   └── scripts/         # Utility scripts for analysis and visualization
    ├── src/                 # Source code
    │   ├── main/            # Main source code
    │   │   ├── java/        # Java source files
    │   │   └── resources/   # Resources for the main application
    │   └── test/            # Test source code
    │       ├── java/        # Test source files
    │       └── resources/   # Test resources
    └── pom.xml              # Maven build configuration

Key Components
-------------

Source Code Organization
~~~~~~~~~~~~~~~~~~~~~~~

The main source code is organized into the following key packages:

- ``org.uma.evolver``: Core framework components and interfaces
- ``org.uma.evolver.algorithm``: Implementation of evolutionary algorithms
- ``org.uma.evolver.parameter``: Parameter handling and configuration
- ``org.uma.evolver.problem``: Problem definitions and utilities
- ``org.uma.evolver.util``: Utility classes and helper functions

Resource Files
~~~~~~~~~~~~~~

The ``resources/`` directory contains important configuration and data files:

- ``parameterSpaces/``: YAML files defining the search space for algorithm parameters
- ``referenceFronts/``: CSV files containing reference Pareto fronts for benchmark problems
- ``scripts/``: Utility scripts for result analysis and visualization

Build System
~~~~~~~~~~~~

The project uses Maven for dependency management and build automation. The main build file is ``pom.xml``, which defines:

- Project metadata and versioning
- Dependencies on external libraries
- Build plugins and configurations
- Test configurations

Documentation
~~~~~~~~~~~~~

Project documentation is maintained in the ``docs/`` directory using reStructuredText format. The documentation includes:

- User guides and tutorials
- API reference
- Conceptual documentation
- Examples and use cases

For more detailed information about specific components, please refer to the :ref:`api_reference` section.
