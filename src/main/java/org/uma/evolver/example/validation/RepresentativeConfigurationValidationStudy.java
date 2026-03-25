package org.uma.evolver.example.validation;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.example.validation.RepresentativeConfigurationCatalog.ConfigurationSpec;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.problem.multiobjective.re.RE22;
import org.uma.jmetal.problem.multiobjective.re.RE23;
import org.uma.jmetal.problem.multiobjective.re.RE24;
import org.uma.jmetal.problem.multiobjective.re.RE25;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.problem.multiobjective.re.RE32;
import org.uma.jmetal.problem.multiobjective.re.RE33;
import org.uma.jmetal.problem.multiobjective.re.RE34;
import org.uma.jmetal.problem.multiobjective.re.RE35;
import org.uma.jmetal.problem.multiobjective.re.RE36;
import org.uma.jmetal.problem.multiobjective.re.RE37;
import org.uma.jmetal.problem.multiobjective.re.RE41;
import org.uma.jmetal.problem.multiobjective.re.RE42;
import org.uma.jmetal.problem.multiobjective.re.RE61;
import org.uma.jmetal.problem.multiobjective.re.RE91;
import org.uma.jmetal.problem.multiobjective.rwa.RWA1;
import org.uma.jmetal.problem.multiobjective.rwa.RWA10;
import org.uma.jmetal.problem.multiobjective.rwa.RWA2;
import org.uma.jmetal.problem.multiobjective.rwa.RWA3;
import org.uma.jmetal.problem.multiobjective.rwa.RWA4;
import org.uma.jmetal.problem.multiobjective.rwa.RWA5;
import org.uma.jmetal.problem.multiobjective.rwa.RWA6;
import org.uma.jmetal.problem.multiobjective.rwa.RWA7;
import org.uma.jmetal.problem.multiobjective.rwa.RWA8;
import org.uma.jmetal.problem.multiobjective.rwa.RWA9;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Compact external validation study for the representative 7k NSGA-II configurations.
 *
 * <p>The study evaluates the standard NSGA-II baseline plus the four representative configurations
 * reported in the manuscript appendix on the full RE and RWA suites. The same runner can also
 * execute the compact real ablations by selecting one representative configuration and resetting
 * archive, crossover, or mutation blocks to their standard defaults.
 */
public class RepresentativeConfigurationValidationStudy {
  private static final int DEFAULT_INDEPENDENT_RUNS = 30;
  private static final int DEFAULT_MAX_EVALUATIONS = 10000;
  private static final int DEFAULT_POPULATION_SIZE = 100;
  private static final int DEFAULT_NUMBER_OF_CORES = -1;
  private static final String YAML_FILE = "NSGAIIDouble.yaml";
  private static final String DEFAULT_OUTPUT_DIRECTORY =
      "experiments/rq4_validation/results/representative-configs";

  /**
   * Validation suite descriptor with experiment name, problems, and split mapping.
   *
   * @param id stable suite id
   * @param experimentName experiment folder name
   * @param problems experiment problems
   * @param splitRows CSV rows describing seen/unseen memberships
   */
  private record SuiteSpec(
      String id,
      String experimentName,
      List<ExperimentProblem<DoubleSolution>> problems,
      List<String[]> splitRows) {}

  /**
   * Parsed command-line arguments.
   *
   * @param suiteId selected suite ({@code re}, {@code rwa}, or {@code all})
   * @param outputDirectory base output directory
   * @param runAlgorithms whether to execute missing FUN/VAR runs
   * @param ablationBaseTag optional representative configuration tag for real ablations
   * @param numberOfCores number of parallel workers used by the experiment
   * @param independentRuns number of stochastic repetitions per problem
   */
  private record Arguments(
      String suiteId,
      String outputDirectory,
      boolean runAlgorithms,
      String ablationBaseTag,
      int numberOfCores,
      int independentRuns) {}

  /**
   * Entry point.
   *
   * @param args command-line arguments
   * @throws IOException if metadata or indicator summaries cannot be written
   */
  public static void main(String[] args) throws IOException {
    Arguments arguments;

    arguments = parseArguments(args);
    for (SuiteSpec suite : selectedSuites(arguments.suiteId())) {
      runStudy(arguments, suite);
    }
  }

  private static Arguments parseArguments(String[] args) {
    Arguments result;
    String suiteId;
    String outputDirectory;
    boolean runAlgorithms;
    String ablationBaseTag;
    int numberOfCores;
    int independentRuns;
    int index;

    suiteId = "all";
    outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
    runAlgorithms = false;
    ablationBaseTag = null;
    numberOfCores = DEFAULT_NUMBER_OF_CORES;
    independentRuns = DEFAULT_INDEPENDENT_RUNS;

    index = 0;
    while (index < args.length) {
      String token;

      token = args[index];
      switch (token) {
        case "--suite" -> {
          suiteId = valueOf(args, index, token).toLowerCase();
          index += 2;
        }
        case "--output-dir" -> {
          outputDirectory = valueOf(args, index, token);
          index += 2;
        }
        case "--ablation-base" -> {
          ablationBaseTag = valueOf(args, index, token);
          index += 2;
        }
        case "--cores" -> {
          numberOfCores = Integer.parseInt(valueOf(args, index, token));
          index += 2;
        }
        case "--runs" -> {
          independentRuns = Integer.parseInt(valueOf(args, index, token));
          index += 2;
        }
        case "--run-algorithms" -> {
          runAlgorithms = true;
          index += 1;
        }
        case "--help", "-h" -> {
          printUsageAndExit();
          index += 1;
        }
        default -> throw new IllegalArgumentException("Unknown argument: " + token);
      }
    }

    validateSuiteId(suiteId);
    numberOfCores = normalizeNumberOfCores(numberOfCores);
    if ((ablationBaseTag != null) && suiteId.equals("all")) {
      throw new IllegalArgumentException(
          "Ablation mode requires a concrete suite selection: use --suite re or --suite rwa");
    }

    result =
        new Arguments(
            suiteId,
            outputDirectory,
            runAlgorithms,
            ablationBaseTag,
            numberOfCores,
            independentRuns);

    return result;
  }

  private static void runStudy(Arguments arguments, SuiteSpec suite) throws IOException {
    YAMLParameterSpace parameterSpace;
    List<ConfigurationSpec> configurationSpecs;
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList;
    Experiment<DoubleSolution, List<DoubleSolution>> experiment;

    parameterSpace = new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());
    configurationSpecs = selectedConfigurations(arguments.ablationBaseTag());
    algorithmList =
        configureAlgorithmList(
            suite.problems(),
            configurationSpecs,
            parameterSpace,
            arguments.independentRuns());
    experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(experimentName(arguments, suite))
            .setAlgorithmList(algorithmList)
            .setProblemList(suite.problems())
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(arguments.outputDirectory())
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(Arrays.asList(new Epsilon(), new PISAHypervolume()))
            .setIndependentRuns(arguments.independentRuns())
            .setNumberOfCores(arguments.numberOfCores())
            .build();

    writeMetadata(experiment.getExperimentBaseDirectory(), suite, configurationSpecs, arguments);

    if (arguments.runAlgorithms()) {
      new ExecuteAlgorithms<>(experiment).run();
    }
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanTestTables<>(experiment).run();
  }

  private static List<ConfigurationSpec> selectedConfigurations(String ablationBaseTag) {
    List<ConfigurationSpec> result;

    if (ablationBaseTag == null) {
      result = new ArrayList<>();
      result.add(RepresentativeConfigurationCatalog.standard());
      result.addAll(RepresentativeConfigurationCatalog.representativeConfigurations());
    } else {
      result = new ArrayList<>();
      result.add(RepresentativeConfigurationCatalog.standard());
      result.add(RepresentativeConfigurationCatalog.configurationByTag(ablationBaseTag));
      result.addAll(RepresentativeConfigurationCatalog.ablationVariants(ablationBaseTag));
    }

    return result;
  }

  private static String experimentName(Arguments arguments, SuiteSpec suite) {
    String result;

    if (arguments.ablationBaseTag() == null) {
      result = suite.experimentName();
    } else {
      result =
          suite.experimentName()
              + "-Ablation-"
              + arguments.ablationBaseTag().replace('-', '_');
    }

    return result;
  }

  private static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList,
      List<ConfigurationSpec> configurationSpecs,
      YAMLParameterSpace parameterSpace,
      int independentRuns) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> result;

    result = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {
        for (ConfigurationSpec configuration : configurationSpecs) {
          result.add(createAlgorithm(expProblem, run, configuration, parameterSpace));
        }
      }
    }

    return result;
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createAlgorithm(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      ConfigurationSpec configuration,
      YAMLParameterSpace parameterSpace) {
    ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> result;
    DoubleNSGAII factory;
    EvolutionaryAlgorithm<DoubleSolution> algorithm;

    factory =
        new DoubleNSGAII(
            expProblem.getProblem(),
            DEFAULT_POPULATION_SIZE,
            DEFAULT_MAX_EVALUATIONS,
            parameterSpace);
    factory.parse(configuration.parameterString().split("\\s+"));
    algorithm = factory.build();
    result = new ExperimentAlgorithm<>(algorithm, configuration.tag(), expProblem, run);

    return result;
  }

  private static List<SuiteSpec> selectedSuites(String suiteId) {
    List<SuiteSpec> result;

    result = new ArrayList<>();
    if (suiteId.equals("all") || suiteId.equals("re")) {
      result.add(reSuite());
    }
    if (suiteId.equals("all") || suiteId.equals("rwa")) {
      result.add(rwaSuite());
    }

    return result;
  }

  private static SuiteSpec reSuite() {
    SuiteSpec result;
    List<ExperimentProblem<DoubleSolution>> problems;
    List<String[]> splitRows;

    problems = new ArrayList<>();
    problems.add(new ExperimentProblem<>(new RE21()).setReferenceFront("RE21.csv"));
    problems.add(new ExperimentProblem<>(new RE22()).setReferenceFront("RE22.csv"));
    problems.add(new ExperimentProblem<>(new RE23()).setReferenceFront("RE23.csv"));
    problems.add(new ExperimentProblem<>(new RE24()).setReferenceFront("RE24.csv"));
    problems.add(new ExperimentProblem<>(new RE25()).setReferenceFront("RE25.csv"));
    problems.add(new ExperimentProblem<>(new RE31()).setReferenceFront("RE31.csv"));
    problems.add(new ExperimentProblem<>(new RE32()).setReferenceFront("RE32.csv"));
    problems.add(new ExperimentProblem<>(new RE33()).setReferenceFront("RE33.csv"));
    problems.add(new ExperimentProblem<>(new RE34()).setReferenceFront("RE34.csv"));
    problems.add(new ExperimentProblem<>(new RE35()).setReferenceFront("RE35.csv"));
    problems.add(new ExperimentProblem<>(new RE36()).setReferenceFront("RE36.csv"));
    problems.add(new ExperimentProblem<>(new RE37()).setReferenceFront("RE37.csv"));
    problems.add(new ExperimentProblem<>(new RE41()).setReferenceFront("RE41.csv"));
    problems.add(new ExperimentProblem<>(new RE42()).setReferenceFront("RE42.csv"));
    problems.add(new ExperimentProblem<>(new RE61()).setReferenceFront("RE61.csv"));
    problems.add(new ExperimentProblem<>(new RE91()).setReferenceFront("RE91.csv"));

    splitRows = new ArrayList<>();
    splitRows.add(new String[] {"RE21", "unseen"});
    splitRows.add(new String[] {"RE22", "unseen"});
    splitRows.add(new String[] {"RE23", "unseen"});
    splitRows.add(new String[] {"RE24", "unseen"});
    splitRows.add(new String[] {"RE25", "unseen"});
    splitRows.add(new String[] {"RE31", "seen"});
    splitRows.add(new String[] {"RE32", "seen"});
    splitRows.add(new String[] {"RE33", "seen"});
    splitRows.add(new String[] {"RE34", "seen"});
    splitRows.add(new String[] {"RE35", "seen"});
    splitRows.add(new String[] {"RE36", "seen"});
    splitRows.add(new String[] {"RE37", "seen"});
    splitRows.add(new String[] {"RE41", "unseen"});
    splitRows.add(new String[] {"RE42", "unseen"});
    splitRows.add(new String[] {"RE61", "unseen"});
    splitRows.add(new String[] {"RE91", "unseen"});

    result = new SuiteSpec("re", "RepresentativeConfigsRE", problems, splitRows);

    return result;
  }

  private static SuiteSpec rwaSuite() {
    SuiteSpec result;
    List<ExperimentProblem<DoubleSolution>> problems;
    List<String[]> splitRows;

    problems = new ArrayList<>();
    problems.add(new ExperimentProblem<>(new RWA1()).setReferenceFront("RWA1.csv"));
    problems.add(new ExperimentProblem<>(new RWA2()).setReferenceFront("RWA2.csv"));
    problems.add(new ExperimentProblem<>(new RWA3()).setReferenceFront("RWA3.csv"));
    problems.add(new ExperimentProblem<>(new RWA4()).setReferenceFront("RWA4.csv"));
    problems.add(new ExperimentProblem<>(new RWA5()).setReferenceFront("RWA5.csv"));
    problems.add(new ExperimentProblem<>(new RWA6()).setReferenceFront("RWA6.csv"));
    problems.add(new ExperimentProblem<>(new RWA7()).setReferenceFront("RWA7.csv"));
    problems.add(new ExperimentProblem<>(new RWA8()).setReferenceFront("RWA8.csv"));
    problems.add(new ExperimentProblem<>(new RWA9()).setReferenceFront("RWA9.csv"));
    problems.add(new ExperimentProblem<>(new RWA10()).setReferenceFront("RWA10.csv"));

    splitRows = new ArrayList<>();
    splitRows.add(new String[] {"RWA1", "unseen"});
    splitRows.add(new String[] {"RWA2", "seen"});
    splitRows.add(new String[] {"RWA3", "seen"});
    splitRows.add(new String[] {"RWA4", "seen"});
    splitRows.add(new String[] {"RWA5", "seen"});
    splitRows.add(new String[] {"RWA6", "seen"});
    splitRows.add(new String[] {"RWA7", "seen"});
    splitRows.add(new String[] {"RWA8", "unseen"});
    splitRows.add(new String[] {"RWA9", "unseen"});
    splitRows.add(new String[] {"RWA10", "unseen"});

    result = new SuiteSpec("rwa", "RepresentativeConfigsRWA", problems, splitRows);

    return result;
  }

  private static void writeMetadata(
      String experimentDirectory,
      SuiteSpec suite,
      List<ConfigurationSpec> configurationSpecs,
      Arguments arguments)
      throws IOException {
    Files.createDirectories(Path.of(experimentDirectory));
    writeAlgorithmMetadata(experimentDirectory, configurationSpecs);
    writeProblemSplitMetadata(experimentDirectory, suite);
    writeRunMetadata(experimentDirectory, suite, arguments);
  }

  private static void writeAlgorithmMetadata(
      String experimentDirectory, List<ConfigurationSpec> configurationSpecs) throws IOException {
    String fileName;

    fileName = experimentDirectory + "/metadata_algorithms.csv";
    try (FileWriter writer = new FileWriter(fileName, false)) {
      writer.write("tag,description,parameter_string\n");
      for (ConfigurationSpec spec : configurationSpecs) {
        writer.write(csv(spec.tag()));
        writer.write(",");
        writer.write(csv(spec.description()));
        writer.write(",");
        writer.write(csv(spec.parameterString()));
        writer.write("\n");
      }
    }
  }

  private static void writeProblemSplitMetadata(String experimentDirectory, SuiteSpec suite)
      throws IOException {
    String fileName;

    fileName = experimentDirectory + "/metadata_problem_splits.csv";
    try (FileWriter writer = new FileWriter(fileName, false)) {
      writer.write("suite,problem,split\n");
      for (String[] row : suite.splitRows()) {
        writer.write(suite.id());
        writer.write(",");
        writer.write(row[0]);
        writer.write(",");
        writer.write(row[1]);
        writer.write("\n");
      }
    }
  }

  private static void writeRunMetadata(
      String experimentDirectory, SuiteSpec suite, Arguments arguments) throws IOException {
    String fileName;
    String ablationMode;

    fileName = experimentDirectory + "/metadata_run_configuration.csv";
    ablationMode = arguments.ablationBaseTag() == null ? "false" : "true";
    try (FileWriter writer = new FileWriter(fileName, false)) {
      writer.write("suite,run_algorithms,ablation_mode,ablation_base_tag,independent_runs,max_evaluations,population_size,cores,yaml_file\n");
      writer.write(suite.id());
      writer.write(",");
      writer.write(Boolean.toString(arguments.runAlgorithms()));
      writer.write(",");
      writer.write(ablationMode);
      writer.write(",");
      writer.write(arguments.ablationBaseTag() == null ? "" : arguments.ablationBaseTag());
      writer.write(",");
      writer.write(Integer.toString(arguments.independentRuns()));
      writer.write(",");
      writer.write(Integer.toString(DEFAULT_MAX_EVALUATIONS));
      writer.write(",");
      writer.write(Integer.toString(DEFAULT_POPULATION_SIZE));
      writer.write(",");
      writer.write(Integer.toString(arguments.numberOfCores()));
      writer.write(",");
      writer.write(YAML_FILE);
      writer.write("\n");
    }
  }

  private static void validateSuiteId(String suiteId) {
    if (!List.of("all", "re", "rwa").contains(suiteId)) {
      throw new IllegalArgumentException("Unknown suite id: " + suiteId);
    }
  }

  private static int normalizeNumberOfCores(int requestedNumberOfCores) {
    int result;

    if (requestedNumberOfCores == -1) {
      result = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    } else if (requestedNumberOfCores >= 1) {
      result = requestedNumberOfCores;
    } else {
      throw new IllegalArgumentException(
          "Invalid core count: " + requestedNumberOfCores + ". Use -1 or a positive integer.");
    }

    return result;
  }

  private static String valueOf(String[] args, int index, String token) {
    String result;

    if (index + 1 >= args.length) {
      throw new IllegalArgumentException("Missing value for " + token);
    }
    result = args[index + 1];

    return result;
  }

  private static void printUsageAndExit() {
    System.out.println("RepresentativeConfigurationValidationStudy");
    System.out.println("  --suite <re|rwa|all>          Suite selection (default: all)");
    System.out.println("  --output-dir <path>           Base output directory");
    System.out.println("  --run-algorithms              Execute missing FUN/VAR runs");
    System.out.println("  --ablation-base <tag>         Run compact real ablations for one representative tag");
    System.out.println(
        "  --cores <int|-1>              Number of parallel workers (-1 = all available minus one; default: -1)");
    System.out.println("  --runs <int>                  Independent runs per problem (default: 30)");
    System.exit(0);
  }

  private static String csv(String value) {
    String result;

    result = "\"" + value.replace("\"", "\"\"") + "\"";

    return result;
  }
}
