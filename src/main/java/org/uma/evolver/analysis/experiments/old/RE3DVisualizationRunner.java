package org.uma.evolver.analysis.experiments.old;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.multiobjective.re.*;
import org.uma.jmetal.problem.multiobjective.rwa.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Runner to generate result files for visualization (1 run, 10,000 evals).
 */
public class RE3DVisualizationRunner {

  private static final int INDEPENDENT_RUNS = 1;
  private static final int MAX_EVALUATIONS = 10000;
  private static final String YAML_FILE = "NSGAIIDoubleFull.yaml";

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    // RE
    problemList.add(new ExperimentProblem<>(new RE31()).setReferenceFront("RE31.csv"));
    problemList.add(new ExperimentProblem<>(new RE32()).setReferenceFront("RE32.csv"));
    problemList.add(new ExperimentProblem<>(new RE33()).setReferenceFront("RE33.csv"));
    problemList.add(new ExperimentProblem<>(new RE34()).setReferenceFront("RE34.csv"));
    problemList.add(new ExperimentProblem<>(new RE35()).setReferenceFront("RE35.csv"));
    problemList.add(new ExperimentProblem<>(new RE36()).setReferenceFront("RE36.csv"));
    problemList.add(new ExperimentProblem<>(new RE37()).setReferenceFront("RE37.csv"));
    // RWA
    problemList.add(new ExperimentProblem<>(new RWA1()).setReferenceFront("RWA1.csv"));
    problemList.add(new ExperimentProblem<>(new RWA2()).setReferenceFront("RWA2.csv"));
    problemList.add(new ExperimentProblem<>(new RWA3()).setReferenceFront("RWA3.csv"));
    problemList.add(new ExperimentProblem<>(new RWA6()).setReferenceFront("RWA6.csv"));
    problemList.add(new ExperimentProblem<>(new RWA7()).setReferenceFront("RWA7.csv"));
    problemList.add(new ExperimentProblem<>(new RWA8()).setReferenceFront("RWA8.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList = configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment = new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(
        "RE3DVisualization")
        .setAlgorithmList(algorithmList)
        .setProblemList(problemList)
        .setExperimentBaseDirectory(experimentBaseDirectory)
        .setOutputParetoFrontFileName("FUN")
        .setOutputParetoSetFileName("VAR")
        .setIndependentRuns(INDEPENDENT_RUNS)
        .setNumberOfCores(8)
        .build();

    new ExecuteAlgorithms<>(experiment).run();
  }

  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    String pStandard = "--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";
    String pRE3D = "--algorithmResult externalArchive --populationSizeWithArchive 147 --archiveType unboundedArchive --createInitialSolutions gridBased --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover blxAlpha --crossoverProbability 0.6485460321233425 --crossoverRepairStrategy bounds --blxAlphaCrossoverAlpha 0.8632413529094627 --mutation uniform --mutationProbabilityFactor 0.8395270667276914 --mutationRepairStrategy round --uniformMutationPerturbation 0.9022056831950059 --selection tournament --selectionTournamentSize 8";
    String pEst = "--algorithmResult externalArchive --populationSizeWithArchive 68 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 400 --variation crossoverAndMutationVariation --crossover blxAlpha --crossoverProbability 0.9080706427288077 --crossoverRepairStrategy bounds --blxAlphaCrossoverAlpha 0.8575338178558894 --mutation levyFlight --mutationProbabilityFactor 0.8920195868917605 --mutationRepairStrategy random --levyFlightMutationBeta 1.227144043359349 --levyFlightMutationStepSize 0.6805871241428736 --selection random";

    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {
        algorithms.add(createAlgo(expProblem, run, "NSGAII-Standard", pStandard, 100, parameterSpace));
        algorithms.add(createAlgo(expProblem, run, "NSGAII-RE3D", pRE3D, 100, parameterSpace));
        algorithms.add(createAlgo(expProblem, run, "NSGAII-RE3D-Est", pEst, 100, parameterSpace));
      }
    }
    return algorithms;
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createAlgo(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag,
      String params,
      int popSize,
      YAMLParameterSpace parameterSpace) {

    DoubleNSGAII factory = new DoubleNSGAII(expProblem.getProblem(), popSize, MAX_EVALUATIONS, parameterSpace);
    factory.parse(params.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = factory.build();
    return new ExperimentAlgorithm<>(algorithm, tag, expProblem, run);
  }
}
