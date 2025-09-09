.. _meta-optimization-level-metaheuristics:

Meta-Optimization-Level Metaheuristics
======================================

In Evolver, the meta-optimization process is performed by a meta-optimization metaheuristic, which is a multi-objective optimization algorithm that searches for the best configuration of a base-level metaheuristic by solving a meta-optimization problem. This problem is a continuous optimization problem, so most of the metaheuristics available in jMetal could potentially be used as meta-optimization metaheuristics. 

In this context, were each solution of the meta-optimization problem requires one or more independent runs of the base-level metaheuristic on all the problems of the training set, using non-parallel meta-optimizers can lead to very long running times, making the meta-optimization process unfeasible. For this reason, it is advisable to use parallel meta-optimizers. This means that, for instance, algorithms such as a MOEA/D or SMS-EMOA are not good candidates because their steady-state nature difficults to make parallel versions of them. On the other hand, algorithms such as NSGA-II or SMPSO are good candidates because they can be easily parallelized. 

A further constraint is that meta-optimization metaheuristics must implement the `EvolutionaryAlgorithm` or the `ParticleSwarmOptimizationAlgorithm` classes of the jMetal ``jmetal-component`` subpackage, as the meta-optimization approach requires the use of observers to store the population or swarm of solutions at each generation or iteration of the meta-optimizers. This means that, for instance, SPEA2 could not be used as is, although it can be parallelized. 

With these considerations, Evolver provides currently three meta-optimizers:

- NSGA-II
- SMPSO
- Async NSGA-II

NSGA-II and SMPSO can be parallelized by using a synchronous parallel scheme, in which all the solutions/particles in the population/swarm are evaluated in parallel. According to this scheme, the behavior of these algorithms is the same as if they were run sequentially. However, scalability may be limited when the number of cores is high, because once the solutions have been evaluated, the rest of the algorithm consists of sequential code.

The Async NSGA-II variant uses an asynchronous parallel scheme, where new solutions are sent to workers to be evaluated in an asynchronous manner. The consequence of this scheme is that the behavior of the algorithm is different from the sequential version, but it can scale to a higher number of cores.

To facilitate the instantion of meta-optimizers, Evolver includes three builders: ``MetaNSGAIIBuilder``, ``MetaSMPSOBuilder``, and ``MetaAsyncNSGAIIBuilder``. These builders provide a simple way to create the meta-optimizers using typical parameter settings. For example, class ``MetaNSGAIIBuilder`` can be used in this way:

.. code-block:: java
  
    int maxEvaluations = 2000;
    int numberOfCores = 8 ;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = 
        new MetaNSGAIIBuilder(metaOptimizationProblem, parameterSpace)
            .setMaxEvaluations(maxEvaluations)
            .setNumberOfCores(numberOfCores)
            .build();
  

At this point, it should be remarked that finding the best parameter settings for the meta-optimizers is a task that deserves further research. Adding another level of meta-optimization, in which meta-level metaheuristics are treated as base-level metaheuristics, would introduce an additional layer of computational complexity that would require a vast amount of resources.
