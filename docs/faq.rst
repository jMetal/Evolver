.. _faq:

Frequently Asked Questions
=========================

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

How do I configure an algorithm?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Algorithms can be configured using YAML configuration files. See the :ref:`user_guide` for examples.

Troubleshooting
---------------

The algorithm is not converging. What should I do?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1. Check your problem formulation
2. Try adjusting the algorithm parameters
3. Increase the number of evaluations
4. Check the algorithm's documentation for specific guidance

I found a bug. What should I do?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Please report bugs by opening an issue on our `GitHub repository <https://github.com/jMetal/Evolver/issues>`_. Include:
1. A clear description of the issue
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Any error messages or logs

Contributing
------------

How can I contribute to Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
See our :ref:`contributing` guide for details on how to contribute code, report bugs, or request features.

Can I add a new algorithm to Evolver?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Yes! We welcome contributions of new algorithms. Please follow the :ref:`contributing` guide and make sure to include appropriate tests and documentation.
