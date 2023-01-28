package org.uma.evolver;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class ConfigurableAlgorithmProblem extends AbstractDoubleProblem {
  private AutoConfigurableAlgorithm algorithm ;
  private List<QualityIndicator> indicators ;
  private List<Parameter<?>> parameters ;

  public ConfigurableAlgorithmProblem() {
    this(null, List.of(new Epsilon(), new PISAHypervolume())) ;
  }

  public ConfigurableAlgorithmProblem(AutoConfigurableAlgorithm algorithm, List<QualityIndicator> indicators) {
    this.algorithm = algorithm ;
    this.indicators = indicators ;
    parameters = null ;

    // Parameters to configure
    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    // 1. Offspring population size: 1 - 200
    lowerLimit.add(1.0) ;
    upperLimit.add(200.0) ;

    // 2. Tournament size: 2 - 8
    lowerLimit.add(2.0) ;
    upperLimit.add(8.0) ;

    // 3. SBX crossover distribution index: 5.0 - 400.0
    lowerLimit.add(5.0) ;
    upperLimit.add(400.0) ;

    // 3. Polynomial mutation distribution index: 5.0 - 400.0
    lowerLimit.add(5.0) ;
    upperLimit.add(400.0) ;

    variableBounds(lowerLimit, upperLimit);
  }

  @Override
  public int numberOfVariables() {
    return 4 ;
  }

  @Override
  public int numberOfObjectives() {
    return indicators.size();
  }

  @Override
  public int numberOfConstraints() {
    return 0;
  }

  @Override
  public String name() {
    return "AutoNSGAII";
  }


  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    int offspringPopulationSize = (int)Math.round(solution.variables().get(0));
    int tournamentSize = (int)Math.round(solution.variables().get(1));
    double sbxDistributionIndex = solution.variables().get(2);
    double polynomialMutationDistributionIndex = solution.variables().get(3);

    String referenceFrontFileName = "resources/ZDT1.csv" ;

    String[] parameters =
        ("--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT4 "
            + "--randomGeneratorSeed 12 "
            + "--referenceFrontFileName "+ referenceFrontFileName + " "
            + "--maximumNumberOfEvaluations 5000 "
            + "--algorithmResult population "
            + "--populationSize 100 "
            + "--offspringPopulationSize " + offspringPopulationSize + " "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--selection tournament "
            + "--selectionTournamentSize " + tournamentSize + " "
            + "--rankingForSelection dominanceRanking "
            + "--densityEstimatorForSelection crowdingDistance "
            + "--crossover SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy bounds "
            + "--sbxDistributionIndex " + sbxDistributionIndex + " "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex " + polynomialMutationDistributionIndex + " ")
            .split("\\s+");

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parseAndCheckParameters(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.create();
    nsgaII.run();

    double[][] referenceFront = null;
    try {
      referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    double[][] front = getMatrixWithObjectiveValues(nsgaII.getResult()) ;

    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    indicators.get(0).setReferenceFront(normalizedReferenceFront);
    indicators.get(1).setReferenceFront(normalizedReferenceFront);

    solution.objectives()[0] = indicators.get(0).compute(normalizedFront)  ;
    solution.objectives()[1] = indicators.get(1).compute(normalizedFront)  * -1.0 ;

    return solution ;
  }
}
