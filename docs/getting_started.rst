.. _getting_started:

Getting Started
==============

This guide will help you get started with Evolver by walking you through a simple example.

Your First Optimization Problem
------------------------------

Let's solve a simple optimization problem using Evolver. We'll start with a single-objective optimization problem.

1. **Create a Problem Class**
   First, let's create a simple optimization problem:

   .. code-block:: java

      package org.uma.evolver.example;

      import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
      import org.uma.jmetal.solution.doublesolution.DoubleSolution;

      public class MyProblem extends AbstractDoubleProblem {
          public MyProblem() {
              setNumberOfVariables(2);
              setNumberOfObjectives(1);
              setNumberOfConstraints(0);
              
              // Set variable bounds
              List<Double> lowerLimit = Arrays.asList(-5.0, -5.0);
              List<Double> upperLimit = Arrays.asList(5.0, 5.0);
              
              setVariableBounds(lowerLimit, upperLimit);
          }

          @Override
          public DoubleSolution evaluate(DoubleSolution solution) {
              double x = solution.variables().get(0);
              double y = solution.variables().get(1);
              
              // Objective: minimize x² + y²
              solution.objectives()[0] = x * x + y * y;
              
              return solution;
          }
      }

2. **Configure and Run the Optimizer**
   Now, let's configure and run NSGA-II to solve this problem:

   .. code-block:: java

      package org.uma.evolver.example;

      import org.uma.evolver.Evolver;
      import org.uma.evolver.configurablealgorithms.ConfigurableNSGAII;
      import org.uma.jmetal.operator.crossover.CrossoverOperator;
      import org.uma.jmetal.operator.mutation.MutationOperator;
      import org.uma.jmetal.operator.selection.SelectionOperator;
      import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
      import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

      public class MyFirstOptimization {
          public static void main(String[] args) {
              // Create the problem
              var problem = new MyProblem();
              
              // Create algorithm configuration
              var configuration = """
                  algorithm: {
                    name: NSGAII
                    parameters: {
                      populationSize: 100
                      maxEvaluations: 25000
                      crossover: SBX
                      crossoverProbability: 0.9
                      crossoverRepair: true
                      crossoverDistributionIndex: 20.0
                      mutation: polynomial
                      mutationProbability: 0.1
                      mutationRepair: true
                      mutationDistributionIndex: 20.0
                    }
                  }
                  """;
              
              // Create and run the algorithm
              var algorithm = new ConfigurableNSGAII(problem, configuration);
              algorithm.run();
              
              // Get results
              var population = algorithm.getResult();
              
              // Print results
              System.out.println("Solutions found: " + population.size());
              for (var solution : population) {
                  System.out.println(solution.objectives()[0]);
              }
          }
      }

Next Steps
----------
- Learn more about :ref:`user_guide`
- Explore the :ref:`api_reference`
- Check out some :ref:`examples`

Troubleshooting
--------------
- If you encounter any issues, please check the :ref:`faq` section
- For more detailed help, please refer to our `GitHub repository <https://github.com/jMetal/Evolver>`_
