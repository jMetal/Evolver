package org.uma.evolver;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoConfigurableAlgorithm;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.auto.parameter.Parameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;


public class ConfigurableAlgorithmProblem extends AbstractDoubleProblem {
  private AutoConfigurableAlgorithm algorithm ;
  private List<QualityIndicator> indicators ;
  private List<Parameter<?>> parameters ;
  //private DynamicReferenceFrontUpdate<DoubleSolution> referenceFrontUpdate = new DynamicReferenceFrontUpdate<>();

  public ConfigurableAlgorithmProblem() {
    this(null, List.of(new Epsilon(), new Spread())) ;
  }

  public ConfigurableAlgorithmProblem(AutoConfigurableAlgorithm algorithm, List<QualityIndicator> indicators) {
    this.algorithm = algorithm ;
    this.indicators = indicators ;
    parameters = null ;

    // Parameters to configure
    List<Double> lowerLimit = new ArrayList<>();
    List<Double> upperLimit = new ArrayList<>();

    for (int i = 0; i<numberOfVariables(); i++) {
      lowerLimit.add(0.0) ;
      upperLimit.add(1.0) ;
    }

    variableBounds(lowerLimit, upperLimit);
  }

  @Override
  public int numberOfVariables() {
    return 11 ;
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
    int offspringPopulationSize = ParameterValues.minMaxIntegerFromProbability(
            ParameterValues.offspringPopulationSize,
            solution.variables().get(0)
    );
    int tournamentSize = ParameterValues.minMaxIntegerFromProbability(
            ParameterValues.tournamentSize,
            solution.variables().get(1)
    );
    double sbxDistributionIndex = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.sbxCrossoverDistributionIndexRange,
            solution.variables().get(2)
    );
    double blxAlphaCrossoverValue = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.blxAlphaCrossoverValueRange,
            solution.variables().get(3)
    );
    double polynomialMutationDistributionIndex = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.polynomialMutationDistributionIndexRange,
            solution.variables().get(4)
    );
    double linkedPolynomialMutationDistributionIndex = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.linkedPolynomialMutationDistributionIndexRange,
            solution.variables().get(5)
    );
    double uniformMutationPerturbation = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.uniformMutationPerturbationRange,
            solution.variables().get(6)
    );
    double nonUniformMutationPerturbation = ParameterValues.minMaxDoubleFromProbability(
            ParameterValues.nonUniformMutationPerturbationRange,
            solution.variables().get(7)
    );
    String createInitialSolutions = ParameterValues.getFromProbability(
            ParameterValues.createInitialSolutions,
            solution.variables().get(8)
    );

    String crossover = ParameterValues.getFromProbability(
            ParameterValues.crossover,
            solution.variables().get(9)
    );

    String mutation = ParameterValues.getFromProbability(
            ParameterValues.mutation,
            solution.variables().get(10)
    );

    String referenceFrontFileName = "resources/ZDT1.csv" ;

    String crossoverConfiguration;
    if (crossover.equals("SBX"))
      crossoverConfiguration = "--sbxDistributionIndex " + sbxDistributionIndex + " ";
    else if (crossover.equals("BLX_ALPHA"))
      crossoverConfiguration = "--blxAlphaCrossoverAlphaValue " + blxAlphaCrossoverValue + " ";
    else // wholeArithmetic
      crossoverConfiguration = "";

    String mutationConfiguration;
    if (mutation.equals("polynomial"))
      mutationConfiguration = "--polynomialMutationDistributionIndex " + polynomialMutationDistributionIndex + " ";
    else if (mutation.equals("linkedPolynomial"))
      mutationConfiguration = "--linkedPolynomialMutationDistributionIndex " + linkedPolynomialMutationDistributionIndex + " ";
    else if (mutation.equals("uniform"))
      mutationConfiguration = "--uniformMutationPerturbation " + uniformMutationPerturbation + " ";
    else if (mutation.equals("nonUniform"))
      mutationConfiguration = "--nonUniformMutationPerturbation " + nonUniformMutationPerturbation + " ";
    else //Not posible
      mutationConfiguration = "";

    String[] parameters =
        ("--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT4 "
            + "--randomGeneratorSeed 15 "
            + "--referenceFrontFileName "+ referenceFrontFileName + " "
            + "--maximumNumberOfEvaluations 15000 "
            + "--algorithmResult population "
            + "--populationSize 100 "
            + "--offspringPopulationSize " + offspringPopulationSize + " "
            + "--createInitialSolutions " + createInitialSolutions + " "
            + "--variation crossoverAndMutationVariation "
            + "--selection tournament "
            + "--selectionTournamentSize " + tournamentSize + " "
            + "--rankingForSelection dominanceRanking "
            + "--densityEstimatorForSelection crowdingDistance "
            + "--crossover " + crossover + " "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy bounds "
            + crossoverConfiguration
            + "--mutation " + mutation + " "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + mutationConfiguration
        )
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

    NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>() ;
    nonDominatedSolutions.addAll(nsgaII.getResult()) ;

    double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions()) ;

    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    /*
    referenceFrontUpdate.update(nsgaII.getResult());
    System.out.println("RS: " + referenceFrontUpdate.referenceFront().length) ;
    double[][] front = getMatrixWithObjectiveValues(nsgaII.getResult()) ;

    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFrontUpdate.referenceFront()),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFrontUpdate.referenceFront()));
     */
    indicators.get(0).setReferenceFront(normalizedReferenceFront) ;
    indicators.get(1).setReferenceFront(normalizedReferenceFront);

    solution.objectives()[0] = indicators.get(0).compute(normalizedFront)  ;
    solution.objectives()[1] = indicators.get(1).compute(normalizedFront)   ;

    return solution ;
  }
}
