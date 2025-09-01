.. _introduction:

Introduction
============

The application of metaheuristics to multi-objective optimization has been an active research area for the past 25 years, 
particularly following the introduction of NSGA-II, SPEA2, and PAES around the year 2000. 
Since then, continuous advances have led to the development of new algorithms designed to address increasingly complex problem domains, 
such as many-objective, dynamic, and large-scale optimization. A fundamental challenge in this field is that the performance of 
metaheuristics is highly dependent on the proper tuning of the algorithm control parameters. 
Given the *No Free Lunch* theorem, which states that no single metaheuristic outperforms all others across all optimization problems, 
parameter tuning remains a critical issue. Traditionally, this process has relied on manual trial-and-error methods, 
which lack scientific rigor and become even more challenging for domain experts who are not familiar with optimization algorithms.

To address this limitation, automated algorithm configuration tools, such as irace and paramILS, have been developed. 
These tools optimize parameter settings by iteratively generating and evaluating configurations on a selected training set of problems 
using predefined quality measures. The search process employs learning strategies to refine configurations, ultimately identifying high-performing 
parameter settings. Once the training phase is complete, the selected configuration is validated on a separate testing set to assess its 
generalization performance.

In this context, we have developed Evolver, a Java-based package for the automatic configuration of multi-objective metaheuristics. 
Evolver formulates the tuning process itself as a multi-objective optimization problem, which is then solved using another metaheuristic. 
It is built upon the jMetal multi-objective optimization framework, which provides a diverse collection of metaheuristic algorithms, benchmark problems, 
and quality indicators. As a result, Evolver becomes a versatile tool for meta-optimization and a valuable research platform in this field.

