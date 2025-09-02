.. _objective-functions:

Objective Functions
==================

Quality indicators are essential components in Evolver's meta-optimization process, serving as objective functions that guide the search for optimal algorithm configurations. This document describes the available quality indicators and their usage in Evolver.

Available Quality Indicators
----------------------------

jMetal provides a wide range of quality indicators that measure the degree of convergence and/or diversity of a Pareto front approximation obtained by a multi-objective metaheuristic, such as additive epsilon (EP), generational distance (GD), inverted generational distance (IGD), inverted generational distance + (IGD+), spread (SP), or hypervolume (HV). All of them are available in Evolver.

All quality indicators used as objective functions are intended to be minimized in Evolver, as this is a requirement imposed by jMetal. This is the case for most of the quality indicators, with the exception of the hypervolume (HV), as it represents a volume than the larger the better. 

Alternatives for Using the HV as an Objective Function
------------------------------------------------------

If we intend to use the HV as an objective, there are two alternatives: the normalized hypervolume (NHV) or Evolver's utility class ``HypervolumeMinus`` (HV-). 

The NHV is defined as follows:

.. math::
    NHV = 1 - \frac{HV_f}{HV_rf}

where `HV_f` is the hypervolume of the front and `HV_rf` is the hypervolume of the reference front. The range of values of the HNV is [0, 1], with lower values indicating better performance.

Class ``HypervolumeMinus`` simply definies the HV- as -1 * HV.

Recommendations for Selecting Quality Indicators
------------------------------------------------
In Evolver, users can select any combination of quality indicators. However, for an optimization problem to be considered multi-objective, its objectives must be conflicting. This condition does not always hold when using different quality indicators. For example, selecting EP and IGD does not guarantee that improving EP will worsen IGD. Nevertheless, quality indicators can be partially conflicting, allowing some non-dominated solutions to emerge. 

The key point is that most interesting goal of meta-optimization is not to generate a diverse set of solutions but to converge quickly and accurately to optimal configurations of the base-level metaheuristic. An approach is to consider the NHV as primary objective thinking on the fact that it is desirable to find configurations of the base-level metaheuristic promoting both diversity and convergence; the EP can be used as secondary objective. In this scenario, the NHV could be replaced by the IGD or IGD+.

Alternatives when The Reference Fronts are Not Available
------------------------------------------------------

Evolver’s meta-optimization approach may initially seem challenging to be applied in real-world settings since Pareto fronts are generally unknown, making it impractical to use quality indicators as optimization objectives, as most of them require a reference front. The exception is the hypervolume (HV), which only needs a reference point. It the reference point of the problems can be estimated, HV- can serve as the primary objective in the meta-optimization problem

Using only HV- can lead to stagnation, particularly in early iterations, when configurations produce HV- values of zero if no solution in the current population dominates the reference point. At this stage, the search may plateau, preventing further progress. To overcome this limitation, the EP indicator can serve as a secondary objective. When every solution has an HV- of zero, the meta-optimizer can rely on EP to refine configurations, enhance convergence, and eventually reach regions where solutions fall below the reference point, so that the meta-optimizer can work in an effective way.

Minimizing the Number Of Evaluations as Objective
-------------------------------------------------

Finding good configurations of base-level metaheuristics is the main goal of meta-optimization. Here, the number of evaluations set in the stopping conditio of the meta-optimization algorithm is a parameter that must be carefully selected. The higher the number of evaluations, the larger the number of generated configurations and thus the probability of finding more accurate configurations of the base-leve metaheuristic at the const of a increasing the computing time.

In this context, an intesting point could be to determine the minimum number of evaluations that are required to find a good enough configurations of the base-level metaheuristic. This could be done by using the number of evaluations of the meta-optimizer as an objective function to be minimized, while other objective could be the NHV (or a combination of NHV and EP).

However, the number of evaluations is not a quality indicator. To overcome this issue, we provide in Evolver a class named ``EvaluationsQualityIndicator``, which is in fact a fake indicator. The point is that class ``MetaOptimizationProblem``, when computing quality indicators checks if one of them is an instance of ``EvaluationsQualityIndicator`` and if so, it uses the number of evaluations as the value of the quality indicator. 