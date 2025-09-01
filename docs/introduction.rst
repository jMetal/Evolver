.. _introduction:

Introduction
============

The application of metaheuristics to multi-objective optimization has been an active research area for the past 25 years, 
particularly following the introduction of NSGA-II, SPEA2, and PAES around the year 2000. Since then, continuous advances have led to the development of new algorithms designed to address increasingly complex problem domains, such as many-objective, dynamic, and large-scale optimization. A fundamental challenge in this field is that the performance of 
metaheuristics is highly dependent on the proper tuning of the algorithm control parameters.  Given the *No Free Lunch* theorem, which states that no single metaheuristic outperforms all others across all optimization problems, 
parameter tuning remains a critical issue. Traditionally, this process has relied on manual trial-and-error methods, 
which lack scientific rigor and become even more challenging for domain experts who are not familiar with optimization algorithms.

To address this limitation, automated algorithm configuration tools, such as irace and paramILS, have been developed. These tools optimize parameter settings by iteratively generating and evaluating configurations on a selected training set of problems 
using predefined quality measures. The search process employs learning strategies to refine configurations, ultimately identifying high-performing  parameter settings. Once the training phase is complete, the selected configuration is validated on a separate testing set to assess its  generalization performance.

In this context, we have developed Evolver, a Java-based package for the automatic configuration of multi-objective metaheuristics. Evolver formulates the tuning process itself as a multi-objective optimization problem, which is then solved using another metaheuristic. It is built upon the jMetal multi-objective optimization framework, which provides a diverse collection of metaheuristic algorithms, benchmark problems, and quality indicators. As a result, Evolver becomes a versatile tool for meta-optimization and a valuable research platform in this field.


Meta-Optimization Approach
--------------------------

.. figure:: figures/metaOptimizationApproach.png
   :align: center
   :alt: Meta-optimization approach
   :figwidth: 50%

   Overview of the meta-optimization approach in Evolver.

The meta-optimization approach adopted by Evolver follows the scheme of the figure above. It considers as goal to find the best configuration of a base-level multi-objective metaheuristic to efficiently solve a set of base-level problems, which we call the training set. This goal is formulated as a meta-optimization problem, where the solution space is composed of algorithm configurations for the base-level optimizer, and the objective space is defined by set of quality indicators that must be minimized. The resulting meta-optimization problem is solvable by a metaheuristic, which we call the meta-optimization algorithm.

Under this approach, a solution of the meta-optimization problem is a particular configuration of the base-level algorithm, 
and evaluating the solution by the meta-optimizer involves running the base-level algorithm on the training set. Then, the quality of the obtained front approximations is assessed by applying a number of quality indicators, which constitute the objective values of the solution.   


Parameter Spaces
----------------

If we consider classical multi-objective metaheuristics, such as NSGA-II or MOEA/D, their parameter spaces are small. For example, the parameters of NSGA-II that need to be tuned to solve continuous problems are: population size, crossover and mutation probabilities, and the distribution indices of simulated binary crossover (SBX) and polynomial mutation. However, if we keep the main feature of NSGA-II (i.e., fast non-dominance ranking and crowding distance density estimator, used in the replacement step), there are many other features that could be incorporated. 

The concept of *parameter space* is key in Evolver, as it defines the space of possible configurations of a base-level metaheuristic. A parameter space is defined by a set of parameters, each with a name, a type (integer, double, categorical), and a definition of the values it can take. In the case of categorical parameters, they can include a list of conditional parameters, which are only considered if the categorical parameter takes a specific value. A parameter can also have a list of global sub-parameters, which are always considered independently of the value of the parameter.

Parameter spaces in Evolver are defined in YAML files. As an example, the parameter space of NSGA-II for solving continuous problems is defined in the file `NSGAIIDouble.yaml <https://github.com/jMetal/Evolver/blob/main/src/main/resources/parameterSpaces/NSGAIIDouble.yaml>`_. A subset of the contents of this file are as follows:

.. code-block:: yaml
    
    algorithmResult:
      type: categorical
      values: 
        population: {}
        externalArchive:
          conditionalParameters:
        populationSizeWithArchive:
          type: integer
          range: [10, 200]
        archiveType:
          type: categorical
          values:
            crowdingDistanceArchive: {}
            unboundedArchive: {}

    createInitialSolutions:
      type: categorical
      values:
        default: {}
        latinHypercubeSampling: {}
        scatterSearch: {}

    selection:
      type: categorical
      values:
        tournament:
          conditionalParameters:
            selectionTournamentSize:
              type: integer
              range: [2, 10]
        random: {}

    

Solution Encoding
-----------------

Evolver encodes all parameters of a given configuration in a vector of real values in the range [0.0, 1.0]. This means that, in case of using the base-level NSGA-II, each solution is a vector of 31 real numbers. Evaluating a solution requires decoding the real values into the corresponding parameters in order to configure the base NSGA-II and run it on the base-level problems.

The adopted encoding scheme is simple and has the advantage that any jMetal multi-objective algorithm capable of solving continuous problems can be used as a meta-optimizer. However, there are two caveats to take into account. 
First, all parameters are flattened in the encoding, and constraints are not considered. For example, uniform mutation perturbations will appear regardless of whether the selected mutation operator is uniform mutation or another type. 
Second, encoding boolean and categorical parameters within the interval [0.0, 1.0] can lead to cases where a mutation does not alter the value of the decoded parameter. For instance, if a variable representing a boolean parameter has a value of 0.2 and a mutation changes it to 0.4, the decoded value remains False in both cases. This happens because values below 0.5 are decoded as False, while values of 0.5 or higher are decoded as True. 

The potential effect of these situations is that the resulting solution after applying variation operators (e.g., crossover and mutation) may not be different from the original solution, so evaluating it is a waste of time. However, this effect is mitigated by increasing the probability of the mutation operator of the meta-optimizer.

Solution Evaluation
-------------------

Given a solution generated by the variation operators of the meta-optimizer algorithm, evaluating it implies running the base-level metaheuristic on the set of :math:`P` base-level problems. As metaheuristics are stochastic techniques, a number :math:`N` of independent runs per each combination of <configuration, problem> should be performed. Given the list of chosen quality indicators that are intended to be minimized, each of them is applied to the resulting fronts of the :math:`N` independent runs. Then the objectives are computed in two steps:

#. For each problem, the median of the quality indicators for the :math:`N` runs is calculated.
#. For each quality indicator, the mean value of the medians of each problem is the resulting objective value.

:math:`N` is a parameter of the meta-optimization problem and it must be set carefully because it can have a high impact on the total running time of the meta-optimization process. By default, :math:`N` is equal to 1.

Objective Functions
-------------------

jMetal provides a wide range of quality indicators that measure the degree of convergence and/or diversity of a Pareto front approximation obtained by a multi-objective metaheuristic, such as additive epsilon (EP), inverted generational distance (IGD), spread (SP), or hypervolume (HV).

As mentioned before, the objective functions of the meta-optimization problem are based on a list of the desired quality indicators. All quality indicators used as objective functions are intended to be minimized in Evolver. Therefore, special care is needed when selecting HV as an objective, as it represents a volume to be maximized. Instead of HV, it can be replaced by the normalized hypervolume (NHV), defined as :math:`1 - HV_f/HV_{rf}`, where :math:`HV_f` is the HV of a Pareto front approximation and :math:`HV_{rf}` is the HV of the reference front used to compute the HV of the front. NHV values range from 0.0 to 1.0, with lower NHV values indicating better performance.


Base-Level metaheuristics
-------------------------
A base-level metaheuristic is a multi-objective metaheuristic that must be configured from any given valid configuration of its parameter space. As a consequence, the existing algorithms in jMetal cannot be used as provided in that framework because their implementation is not generic enough. 

Evolver includes a set of algorithms that have been modified to be used as base-level metaheuristics. Concretely, a base-level metaheuristic can be configured by setting the values of a parameter space as a string of pairs <--parameterName, value>. For example, the configuration of the base-level NSGA-II algorithm for solving continuous problems (ZDT4 in the exampleusing standard settings can be specified as shown in this code snippet:

.. code-block:: java

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 100 "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split("\\s+");

    String yamlParameterSpaceFile = "NSGAIIDouble.yaml" ;

    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    int populationSize = 100 ;
    int maximumNumberOfEvaluations = 20000 ;

    var baseNSGAII = new DoubleNSGAII(new ZDT4(), populationSize, maximumNumberOfEvaluations, parameterSpace);
    baseNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();


Meta-Optimizers 
---------------

As previously mentioned, choosing a real encoding for the meta-optimizer allows the use of most multi-objective metaheuristics available in jMetal, including evolutionary algorithms (NSGA-II, MOEA/D, SMS-EMOA, SPEA2, etc.), differential evolution (GDE3, MOEA/D-DE) and particle swarm optimization algorithms (OMOPSO, SMPSO).

Some of these algorithms can evaluate the population or swarm in parallel using a synchronous parallel scheme to speed up execution. For NSGA-II, a more efficient asynchronous parallel version is also available. Using parallel meta-optimizers is highly desirable as a meta-optimization can take a long time to complete, and parallelization can significantly reduce the total running time.