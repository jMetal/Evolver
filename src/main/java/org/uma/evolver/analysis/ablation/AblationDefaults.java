package org.uma.evolver.analysis.ablation;

import java.nio.file.Path;

/**
 * Provides default settings for ablation analyses by problem suite.
 */
final class AblationDefaults {

  private AblationDefaults() {
    // Utility class
  }

  static Defaults forSuite(ProblemSuite suite) {
    Defaults result;
    if (suite == ProblemSuite.ZDT) {
      result = zdtDefaults();
    } else if (suite == ProblemSuite.DTLZ) {
      result = dtlzDefaults();
    } else if (suite == ProblemSuite.WFG) {
      result = wfgDefaults();
    } else {
      throw new IllegalArgumentException("Unsupported problem suite: " + suite);
    }
    return result;
  }

  private static Defaults zdtDefaults() {
    Defaults result;
    result = new Defaults(
        "Ablation Analysis",
        10,
        0,
        Runtime.getRuntime().availableProcessors(),
        100,
        "NSGAIIDouble.yaml",
        IndicatorDefinition.normalizedHypervolume(),
        true,
        true,
        "ablation_results",
        defaultOutputDirectory("ZDT"),
        standardBaselineConfiguration(),
        zdtOptimizedConfiguration());
    return result;
  }

  private static Defaults dtlzDefaults() {
    Defaults result;
    result = new Defaults(
        "Ablation Analysis",
        10,
        0,
        Runtime.getRuntime().availableProcessors(),
        100,
        "NSGAIIDouble.yaml",
        IndicatorDefinition.normalizedHypervolume(),
        true,
        true,
        "ablation_results",
        defaultOutputDirectory("DTLZ3D"),
        standardBaselineConfiguration(),
        dtlzOptimizedConfiguration());
    return result;
  }

  private static Defaults wfgDefaults() {
    Defaults result;
    result = new Defaults(
        "Ablation Analysis",
        10,
        0,
        Runtime.getRuntime().availableProcessors(),
        100,
        "NSGAIIDoubleFull.yaml",
        IndicatorDefinition.normalizedHypervolume(),
        true,
        true,
        "ablation_results",
        defaultOutputDirectory("WFG2D"),
        standardBaselineConfiguration(),
        wfgOptimizedConfiguration());
    return result;
  }

  private static Path defaultOutputDirectory(String suiteName) {
    Path result;
    result = Path.of("results", "ablation", suiteName);
    return result;
  }

  private static String standardBaselineConfiguration() {
    String result;
    result = "--algorithmResult population "
        + "--createInitialSolutions default "
        + "--offspringPopulationSize 100 "
        + "--variation crossoverAndMutationVariation "
        + "--crossover SBX "
        + "--crossoverProbability 0.9 "
        + "--crossoverRepairStrategy bounds "
        + "--sbxDistributionIndex 20.0 "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0 "
        + "--selection tournament "
        + "--selectionTournamentSize 2";
    return result;
  }

  private static String zdtOptimizedConfiguration() {
    String result;
    result = "--algorithmResult externalArchive "
        + "--populationSizeWithArchive 52 "
        + "--archiveType unboundedArchive "
        + "--createInitialSolutions default "
        + "--offspringPopulationSize 1 "
        + "--variation crossoverAndMutationVariation "
        + "--crossover SBX "
        + "--crossoverProbability 0.8344405454288435 "
        + "--crossoverRepairStrategy bounds "
        + "--sbxDistributionIndex 46.40624621381956 "
        + "--mutation nonUniform "
        + "--mutationProbabilityFactor 0.3873069125490838 "
        + "--mutationRepairStrategy bounds "
        + "--nonUniformMutationPerturbation 0.20422968168417088 "
        + "--selection tournament "
        + "--selectionTournamentSize 7";
    return result;
  }

  private static String dtlzOptimizedConfiguration() {
    String result;
    result = "--algorithmResult externalArchive "
        + "--populationSizeWithArchive 68 "
        + "--archiveType unboundedArchive "
        + "--createInitialSolutions default "
        + "--offspringPopulationSize 5 "
        + "--variation crossoverAndMutationVariation "
        + "--crossover SBX "
        + "--crossoverProbability 0.954734302713461 "
        + "--crossoverRepairStrategy random "
        + "--sbxDistributionIndex 108.03261497125926 "
        + "--mutation uniform "
        + "--mutationProbabilityFactor 1.1248170788140066 "
        + "--mutationRepairStrategy bounds "
        + "--uniformMutationPerturbation 0.46304358234940657 "
        + "--selection tournament "
        + "--selectionTournamentSize 7";
    return result;
  }

  private static String wfgOptimizedConfiguration() {
    String result;
    result = "--algorithmResult externalArchive "
        + "--populationSizeWithArchive 73 "
        + "--archiveType crowdingDistanceArchive "
        + "--createInitialSolutions latinHypercubeSampling "
        + "--offspringPopulationSize 2 "
        + "--variation crossoverAndMutationVariation "
        + "--crossover blxAlphaBeta "
        + "--crossoverProbability 0.6466458903885894 "
        + "--crossoverRepairStrategy bounds "
        + "--blxAlphaBetaCrossoverBeta 0.4213474801441299 "
        + "--blxAlphaBetaCrossoverAlpha 0.8024887081802909 "
        + "--mutation nonUniform "
        + "--mutationProbabilityFactor 0.6740009648716038 "
        + "--mutationRepairStrategy round "
        + "--nonUniformMutationPerturbation 0.6355943954800332 "
        + "--selection tournament "
        + "--selectionTournamentSize 6";
    return result;
  }

  record Defaults(
      String analysisName,
      int numberOfRuns,
      int maxEvaluations,
      int numberOfThreads,
      int populationSize,
      String yamlParameterSpaceFile,
      IndicatorDefinition indicatorDefinition,
      boolean enableProgressReporting,
      boolean validateConfigurations,
      String outputPrefix,
      Path outputDirectory,
      String baselineConfiguration,
      String optimizedConfiguration) {
  }
}
