package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.cli.*;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.*;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.DefaultZCATSettings;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT20;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.aggregationfunction.impl.PenaltyBoundaryIntersection;
import org.uma.jmetal.util.aggregationfunction.impl.Tschebyscheff;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;
import org.uma.jmetal.util.sequencegenerator.impl.IntegerPermutationGenerator;

/**
 * Example of experimental study based on solving the ZCAT problems with two objectives using
 * several metaheuristics. The ZCAT benchmark must be properly configured by setting two objectives
 * in class {@link DefaultZCATSettings}
 *
 * <p>Four quality indicators are used for performance assessment: {@link Epsilon}, {@link
 * NormalizedHypervolume}, {@link PISAHypervolume}, and {@link InvertedGenerationalDistancePlus}.
 *
 * <p>The steps to carry out are: 1. Configure the experiment 2. Execute the algorithms 3. Compute
 * que quality indicators 4. Generate Latex tables reporting means and medians, and tables with
 * statistical tests 5. Generate HTML pages with tables, boxplots, and fronts.
 *
 * @author Antonio J. Nebro
 */
public class ZCATStudy {
  private static final int INDEPENDENT_RUNS = 15;
  private static final int POPULATION_SIZE = 100;
  private static int MAX_EVALUATIONS = 500 * POPULATION_SIZE;

  public static void main(String[] args) throws IOException {
    DefaultZCATSettings.numberOfObjectives = 2;
    DefaultZCATSettings.numberOfVariables = 30;
    DefaultZCATSettings.complicatedParetoSet = false;
    DefaultZCATSettings.level = 1;
    DefaultZCATSettings.bias = true;
    DefaultZCATSettings.imbalance = false;
    String experimentBaseDirectory = "." ;
    String studyName = "ZCATStudy" ;
    int numberOfCores = 8 ;

    Options options = new Options();

    options.addOption(null, "numberOfObjectives", true, "Number of objectives");
    options.addOption(null, "numberOfVariables", true, "Number of variables");
    options.addOption(null, "complicatedParetoSet", true, "Use complicated Pareto set");
    options.addOption(null, "level", true, "Level parameter");
    options.addOption(null, "bias", true, "Bias setting");
    options.addOption(null, "imbalance", true, "Imbalance setting");
    options.addOption(null, "experimentBaseDirectory", true, "Experiment base directory");
    options.addOption(null, "studyName", true, "Experiment name");
    options.addOption(null, "numberOfCores", true, "Number of cores") ;
    options.addOption(null, "maxEvaluations", true, "Maximum number of evaluations") ;

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("numberOfObjectives")) {
        DefaultZCATSettings.numberOfObjectives = Integer.parseInt(cmd.getOptionValue("numberOfObjectives"));
      }
      if (cmd.hasOption("numberOfVariables")) {
        DefaultZCATSettings.numberOfVariables = Integer.parseInt(cmd.getOptionValue("numberOfVariables"));
      }
      if (cmd.hasOption("complicatedParetoSet")) {
        DefaultZCATSettings.complicatedParetoSet = Boolean.parseBoolean(cmd.getOptionValue("complicatedParetoSet"));
      }
      if (cmd.hasOption("level")) {
        DefaultZCATSettings.level = Integer.parseInt(cmd.getOptionValue("level"));
      }
      if (cmd.hasOption("bias")) {
        DefaultZCATSettings.bias = Boolean.parseBoolean(cmd.getOptionValue("bias"));
      }
      if (cmd.hasOption("imbalance")) {
        DefaultZCATSettings.imbalance = Boolean.parseBoolean(cmd.getOptionValue("imbalance"));
      }
      if (cmd.hasOption("numberOfCores")) {
        numberOfCores = Integer.parseInt(cmd.getOptionValue("numberOfCores"));
      }
      if (cmd.hasOption("maxEvaluations")) {
        MAX_EVALUATIONS = Integer.parseInt(cmd.getOptionValue("maxEvaluations"));
      }

      experimentBaseDirectory = cmd.getOptionValue("experimentBaseDirectory", ".");
      studyName = cmd.getOptionValue("studyName", "ZCATStudy");

      JMetalLogger.logger.info("Configuration:");
      JMetalLogger.logger.info("Number of Objectives: " + DefaultZCATSettings.numberOfObjectives);
      JMetalLogger.logger.info("Number of Variables: " + DefaultZCATSettings.numberOfVariables);
      JMetalLogger.logger.info("Complicated Pareto Set: " + DefaultZCATSettings.complicatedParetoSet);
      JMetalLogger.logger.info("Level: " + DefaultZCATSettings.level);
      JMetalLogger.logger.info("Bias: " + DefaultZCATSettings.bias);
      JMetalLogger.logger.info("Imbalance: " + DefaultZCATSettings.imbalance);
      JMetalLogger.logger.info("Study name: " + studyName);
      JMetalLogger.logger.info("Experiment Base Directory: " + experimentBaseDirectory);
      JMetalLogger.logger.info("Number of cores: " + numberOfCores);
      JMetalLogger.logger.info("Maximum number of evaluations: " + MAX_EVALUATIONS);
    } catch (ParseException e) {
      JMetalLogger.logger.severe("Error parsing command line arguments: " + e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Main", options);
    }

    // Create a list with the problems
    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    IntStream.rangeClosed(1, 20)
        .forEach(
            i -> {
              Problem<DoubleSolution> problem =
                  ProblemFactory.loadProblem("org.uma.jmetal.problem.multiobjective.zcat.ZCAT" + i);
              problemList.add(
                  new ExperimentProblem<>(problem)
                      .setReferenceFront(
                          "ZCAT" + i + "." + DefaultZCATSettings.numberOfObjectives + "D.csv"));
            });
    
    // Create a list with the algorithms
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    // Experiment configuration
    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(studyName)
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFrontsCSV")
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                List.of(
                    new Epsilon(),
                    new PISAHypervolume(),
                    new NormalizedHypervolume(),
                    new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(numberOfCores)
            .build();

    // Execution and generation of statistical stuff
    //new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    //new GenerateFriedmanHolmTestTables<>(experiment).run();
    //new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    //new GenerateBoxplotsWithR<>(experiment).setRows(5).setColumns(4).run();
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (var experimentProblem : problemList) {
        autoNgaii(algorithms, run, experimentProblem);
        autoSMSEMOA(algorithms, run, experimentProblem);
        nsgaii(algorithms, run, experimentProblem);
        nsgaiide(algorithms, run, experimentProblem);
        moead(algorithms, run, experimentProblem);
        moeadde(algorithms, run, experimentProblem);
        smpso(algorithms, run, experimentProblem);
        smsemoa(algorithms, run, experimentProblem);
        smsemoade(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void autoNgaii(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
          int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
            ("--algorithmResult externalArchive --populationSizeWithArchive 63 --externalArchive unboundedArchive --createInitialSolutions random --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.9981823902234037 --crossoverRepairStrategy bounds --sbxDistributionIndex 68.00790628874215 --blxAlphaCrossoverAlphaValue 0.4889180982256614 --mutation polynomial --mutationProbabilityFactor 0.00817510064962912 --mutationRepairStrategy round --polynomialMutationDistributionIndex 286.0704738452895 --linkedPolynomialMutationDistributionIndex 159.08935473756773 --uniformMutationPerturbation 0.1990471714741109 --nonUniformMutationPerturbation 0.586239154007381 --selection random --selectionTournamentSize 2 \n")
                    .split("\\s+");

    ConfigurableNSGAII algorithm =
            new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(new ExperimentAlgorithm<>(algorithm.build(), "AutoNSGAII", experimentProblem, run));
  }

  private static void autoSMSEMOA(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
          int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 48 --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.997977868432611 --crossoverRepairStrategy round --sbxDistributionIndex 269.5970174079065 --blxAlphaCrossoverAlphaValue 0.5134648325935949 --mutation uniform --mutationProbabilityFactor 0.001391226205827329 --mutationRepairStrategy random --polynomialMutationDistributionIndex 149.59824609056594 --linkedPolynomialMutationDistributionIndex 139.20357113434 --uniformMutationPerturbation 0.17464392869704493 --nonUniformMutationPerturbation 0.4902199588130758 --selection random --selectionTournamentSize 3 \n")
            .split("\\s+");

    var algorithm =
            new ConfigurableSMSEMOA((DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(new ExperimentAlgorithm<>(algorithm.build(), "AutoSMSEMOA", experimentProblem, run));
  }

  private static void nsgaii(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / experimentProblem.getProblem().numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int offspringPopulationSize = POPULATION_SIZE;

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    EvolutionaryAlgorithm<DoubleSolution> algorithm =
        new NSGAIIBuilder<>(
                experimentProblem.getProblem(),
                POPULATION_SIZE,
                offspringPopulationSize,
                crossover,
                mutation)
            .setTermination(termination)
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "NSGAII", experimentProblem, run));
  }

  private static void nsgaiide(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    double cr = 1.0;
    double f = 0.5;

    Algorithm<List<DoubleSolution>> algorithm =
        new NSGAIIDEBuilder(
                experimentProblem.getProblem(),
                POPULATION_SIZE,
                cr,
                f,
                new PolynomialMutation(
                    1.0 / experimentProblem.getProblem().numberOfVariables(), 20.0),
                DifferentialEvolutionCrossover.DE_VARIANT.RAND_1_BIN)
            .setTermination(termination)
            .build();
    algorithms.add(new ExperimentAlgorithm<>(algorithm, "NSGAIIDE", experimentProblem, run));
  }

  private static void moead(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / experimentProblem.getProblem().numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    String weightVectorDirectory = "resources/weightVectorFiles/moead";
    SequenceGenerator<Integer> sequenceGenerator = new IntegerPermutationGenerator(POPULATION_SIZE);
    boolean normalizeObjectives = true;

    EvolutionaryAlgorithm<DoubleSolution> algorithm =
        new MOEADBuilder<>(
                experimentProblem.getProblem(),
                POPULATION_SIZE,
                crossover,
                mutation,
                weightVectorDirectory,
                sequenceGenerator,
                normalizeObjectives)
            .setTermination(termination)
            .setAggregationFunction(new PenaltyBoundaryIntersection(5.0, normalizeObjectives))
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "MOEAD", experimentProblem, run));
  }

  private static void moeadde(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    double cr = 1.0;
    double f = 0.5;

    double mutationProbability = 1.0 / experimentProblem.getProblem().numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    String weightVectorDirectory = "resources/weightVectorFiles/moead";
    SequenceGenerator<Integer> sequenceGenerator = new IntegerPermutationGenerator(POPULATION_SIZE);

    boolean normalizeObjectives = false;
    EvolutionaryAlgorithm<DoubleSolution> algorithm =
        new MOEADDEBuilder(
                experimentProblem.getProblem(),
                POPULATION_SIZE,
                cr,
                f,
                mutation,
                weightVectorDirectory,
                sequenceGenerator,
                normalizeObjectives)
            .setTermination(termination)
            .setMaximumNumberOfReplacedSolutionsy(2)
            .setNeighborhoodSelectionProbability(0.9)
            .setNeighborhoodSize(20)
            .setAggregationFunction(new Tschebyscheff(normalizeObjectives))
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "MOEADDE", experimentProblem, run));
  }

  private static void smpso(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    ParticleSwarmOptimizationAlgorithm algorithm =
        new SMPSOBuilder((DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE)
            .setTermination(termination)
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "SMPSO", experimentProblem, run));
  }

  private static void smsemoa(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / experimentProblem.getProblem().numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    var algorithm =
        new SMSEMOABuilder<>(experimentProblem.getProblem(), POPULATION_SIZE, crossover, mutation)
            .setTermination(termination)
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "SMSEMOA", experimentProblem, run));
  }

  private static void smsemoade(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    double mutationProbability = 1.0 / experimentProblem.getProblem().numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    double cr = 1.0;
    double f = 0.5;
    EvolutionaryAlgorithm<DoubleSolution> algorithm =
        new SMSEMOADEBuilder(
                experimentProblem.getProblem(),
                POPULATION_SIZE,
                cr,
                f,
                mutation,
                DifferentialEvolutionCrossover.DE_VARIANT.RAND_1_BIN)
            .setTermination(termination)
            .build();

    algorithms.add(new ExperimentAlgorithm<>(algorithm, "SMSEMOADE", experimentProblem, run));
  }
}
