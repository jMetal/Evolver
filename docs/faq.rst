.. _faq:

Frequently Asked Questions
==========================

Here are some common questions about Evolver.

General
-------

What is Evolver?
~~~~~~~~~~~~~~~~
Evolver is a framework for metaheuristic optimization, built on top of jMetal. It provides tools for configuring and running optimization algorithms.

What problems can I solve with Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Evolver is designed for single and multi-objective optimization problems. It's particularly useful for complex, real-world optimization problems.

Installation
------------

What are the system requirements?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
- Java 11 or higher
- Maven 3.6 or higher
- (Optional) Python 3.8+ for documentation

I'm getting build errors. What should I do?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1. Make sure you have all prerequisites installed
2. Try cleaning the Maven repository:
   .. code-block:: bash

      mvn dependency:purge-local-repository
3. Check the error message and consult the :ref:`installation` guide
4. If the problem persists, please open an issue on GitHub

Usage
-----

How do I create a custom optimization problem?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
See the :ref:`getting_started` guide for an example of creating a custom problem.

How do I configure a base-level metaheuristic?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Algorithms can be configured using YAML configuration files. See the :ref:`user_guide` for examples.

Troubleshooting
---------------

Getting Help
-----------

Where can I get help with Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
- Check the :ref:`troubleshooting` section for common issues
- Search the `GitHub issues <https://github.com/jMetal/Evolver/issues>`_ to see if your question has been asked before
- Open a new issue on GitHub for bug reports or feature requests
- For general discussions, use the jMetal discussion forum

Troubleshooting
---------------
Common Issues and Solutions:

Algorithm Not Converging
~~~~~~~~~~~~~~~~~~~~~~~~
- Check if your problem's search space is properly defined
- Verify that your objective functions are correctly implemented
- Try increasing the population size or number of evaluations
- Review the algorithm's parameter settings in the configuration

Performance Issues
~~~~~~~~~~~~~~~~~
- Enable parallel evaluation if not already enabled
- Consider using a more efficient solution representation
- Check for memory leaks in custom operators
- Review the performance tuning guide for optimization tips

Build/Compilation Errors
~~~~~~~~~~~~~~~~~~~~~~~
- Ensure all dependencies are correctly specified in pom.xml
- Check for version conflicts between dependencies
- Run `mvn clean install -U` to update dependencies
- Verify your Java version matches the project requirements

I found a bug. What should I do?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Please report bugs by opening an issue on our `GitHub repository <https://github.com/jMetal/Evolver/issues>`_. Include:
1. A clear description of the issue
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Any error messages or logs
6. Version information (Evolver, Java, Maven)

Contributing
------------

How can I contribute to Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
See our :ref:`contributing` guide for details on how to contribute code, report bugs, or request features.

Can I add a new algorithm to Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Yes! We welcome contributions of new algorithms. Please follow the :ref:`contributing` guide and make sure to include appropriate tests and documentation.
