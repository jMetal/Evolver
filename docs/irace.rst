.. _irace:

irace Integration
=================

This section shows how irace can be used to find configurations for base-level metaheuristics. NSGA-II will be used as base-level metaheuristics and the trainig set will be  the ZDT problems.

We assume that the reader is already familiar with irace, so we will focus on how use the provided resources, without entering into details of irace. 

Prerequisites
-------------
Evolver contains a sub-package called `irace <https://github.com/jMetal/Evolver/tree/main/src/main/java/org/uma/evolver/util/irace>`_, which contains the AutoNSGAIIIraceHV class. This class uses the HV indicator as metric to evaluate the configurations produced by irace. You cat modify this class according to your needs, in particular the base directory where the reference fronts of the problems are expected to be located.

Before running irace, we need to generate a .jar file with the Evolver project. This can be done by running the following command:

.. code-block:: bash

    mvn clean package -DskipTests=true


The ``Evolver-2.0.jar-with-dependencies.jar`` file will be generated in the ``target`` folder of the source root of the Evolver project. Just copy it to the folder where you want to run irace.

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
- ``scenario-NSGAII.txt``: irace scenario file. 
- ``run.sh``: script to run irace

Running irace
-------------   

To run irace, just execute the following command:

.. code-block:: bash

    ./run.sh scenario-NSGAII.txt 1  

The irace output files will be generated in the folder named ``execdir-1``.

    
