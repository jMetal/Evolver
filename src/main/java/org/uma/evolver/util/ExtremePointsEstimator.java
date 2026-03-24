package org.uma.evolver.util;

import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.singleobjective.GeneticAlgorithmBuilder;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtremePointsEstimator {

  private final DoubleProblem problem;
  private final int totalBudget;
  private final int runsPerObjective;

  private double[] ideal;
  private double[] nadir;

  public ExtremePointsEstimator(DoubleProblem problem, int totalBudget, int runsPerObjective) {
    this.problem = problem;
    this.totalBudget = totalBudget;
    this.runsPerObjective = runsPerObjective;

    int nObj = problem.numberOfObjectives();
    ideal = new double[nObj];
    nadir = new double[nObj];
    Arrays.fill(ideal, Double.MAX_VALUE);
    Arrays.fill(nadir, -Double.MAX_VALUE);
  }

  public void run() {
    int nObj = problem.numberOfObjectives();
    int budgetPerRun = totalBudget / (nObj * runsPerObjective);

    // Acumula todas las soluciones evaluadas para estimar el nadir
    List<DoubleSolution> allSolutions = new ArrayList<>();

    for (int obj = 0; obj < nObj; obj++) {
      for (int run = 0; run < runsPerObjective; run++) {
        DoubleSolution best = optimizeObjective(problem, obj, budgetPerRun);
        
        // Reevaluate over the original problem to re-populate objectives array of size problem.numberOfObjectives()
        DoubleSolution originalSolution = problem.createSolution();
        for (int i = 0; i < best.variables().size(); i++) {
          originalSolution.variables().set(i, best.variables().get(i));
        }
        problem.evaluate(originalSolution);
        
        allSolutions.add(originalSolution);

        // Ideal: mejor valor del objetivo optimizado
        ideal[obj] = Math.min(ideal[obj], originalSolution.objectives()[obj]);
      }
    }

    // Nadir: peor valor de cada objetivo sobre TODAS las soluciones
    // Una solución óptima para obj_i suele ser mala para obj_j
    for (DoubleSolution s : allSolutions) {
      for (int i = 0; i < nObj; i++) {
        nadir[i] = Math.max(nadir[i], s.objectives()[i]);
      }
    }

    applyConservativeMargin();
  }

  private DoubleSolution optimizeObjective(DoubleProblem problem, int objectiveIndex, int budget) {
    SingleObjectiveWrapper wrappedProblem = new SingleObjectiveWrapper(problem, objectiveIndex);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / problem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 25;
    int offspringPopulationSize = populationSize;

    Termination termination = new TerminationByEvaluations(budget);

    EvolutionaryAlgorithm<DoubleSolution> geneticAlgorithm =
        new GeneticAlgorithmBuilder<>(
                "GGA", wrappedProblem, populationSize, offspringPopulationSize, crossover, mutation)
            .setTermination(termination)
            .build();

    geneticAlgorithm.run();
    return geneticAlgorithm.result().getFirst();
  }

  private void applyConservativeMargin() {
    for (int i = 0; i < ideal.length; i++) {
      double range = nadir[i] - ideal[i];
      nadir[i] = nadir[i] + 0.10 * range; // margen del 10%
    }
  }

  public double[] ideal() {
    return ideal;
  }

  public double[] nadir() {
    return nadir;
  }

  public static void main(String[] args) {
    DoubleProblem targetProblem = new org.uma.jmetal.problem.multiobjective.re.RE33() ;
    
    int[] budgets = {500, 1000, 2000, 5000, 10000, 20000, 50000};
    
    for (int budget : budgets) {
      ExtremePointsEstimator estimator = new ExtremePointsEstimator(targetProblem, budget, 2);
      estimator.run();
      System.out.println("Budget: " + budget);
      System.out.println("  Ideal: " + Arrays.toString(estimator.ideal()));
      System.out.println("  Nadir: " + Arrays.toString(estimator.nadir()));
      System.out.println("-".repeat(50));
    }
  }
}
