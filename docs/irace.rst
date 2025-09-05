.. _irace:

irace Integration
=================

This section shows how irace can be used to find configurations for base-level metaheuristics. NSGA-II is used as base-level metaheuristics and the trainig set are the ZDT problems.

We assume that the reader is already familiar with irace, so we will focus on how use the provided resources, without entering into details of irace. 

Prerequisites
-------------



To run irace, you need to have irace installed in your system. You can download it from the `irace website <https://irace.toulouse.inrae.fr/>`_.

Resources
---------

The ``src/main/java/resources/irace`` folder contains the following files:

- ``irace_4.2.0.tar.gz``: irace 4.2.0
- ``instances-list-ZDT.txt``: file containing the ZDT qualified names according to the jMetal package where they are located, the reference front file names, the maximum number of evaluations of the base-level algorithm and its population size:

.. literalinclude:: ../src/main/resources/irace/instances-list-ZDT.txt
   :language: bash
   :linenos:
   :name: instances-list-zdt

- ``parameters-NSGAII.txt``: Paramater space of NSGA-II for solving continuous problems in irace format
- ``scenario-NSGAII.txt``: irace scenario file
- ``run.sh``: script to run irace

To run irace, just execute the following command:

.. code-block:: bash

    ./run.sh scenario-NSGAII.txt 1

    
