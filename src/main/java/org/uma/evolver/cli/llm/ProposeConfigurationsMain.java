package org.uma.evolver.cli.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.meta.llm.ConfigurationProposer;
import org.uma.evolver.meta.llm.ConfigurationProposer.Proposal;
import org.uma.evolver.meta.llm.LlmSettings;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;

/**
 * Asks a language model for configurations of a parameter space and writes the valid ones to a
 * file, one per line: a file that a meta-optimizer can start from ({@code initialConfigurations}),
 * or that can be validated. Experimental: whether the proposals help is a question to study.
 *
 * <pre>{@code
 * java -cp Evolver-<version>-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.llm.ProposeConfigurationsMain \
 *     --settings llm.yaml --space NSGAIIDouble.yaml --encoding Double --count 10 \
 *     --task "NSGA-II for the bi-objective ZDT problems, population 100, 12000 evaluations" \
 *     --output results/llm/configurations.txt
 * }</pre>
 *
 * <p>Options: {@code --settings} (the model, see {@link LlmSettings}), {@code --space} (a bundled
 * parameter space or a path), {@code --encoding} (Double, Binary or Permutation), {@code --task} or
 * {@code --task-file}, {@code --count} (10 by default), {@code --output}, {@code --examples} (a file
 * of example configurations) and {@code --log} (the CSV log of the calls, {@code LLM_CALLS.csv}
 * next to the output by default).
 */
public final class ProposeConfigurationsMain {

  static final String USAGE =
      "Usage: ProposeConfigurationsMain --settings <llm.yaml> --space <space.yaml> --encoding"
          + " <Double|Binary|Permutation> (--task <text> | --task-file <file>) --output <file>"
          + " [--count 10] [--examples <file>] [--log <file>]";

  private ProposeConfigurationsMain() {}

  public static void main(String[] args) throws IOException {
    try {
      List<Proposal> proposals = execute(args);
      System.exit(proposals.isEmpty() ? 1 : 0);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(2);
    }
  }

  /** Runs the tool and returns the valid configurations it wrote. */
  static List<Proposal> execute(String[] args) throws IOException {
    String settings = null;
    String space = null;
    String encoding = null;
    String task = null;
    String output = null;
    String examples = null;
    String log = null;
    int count = 10;
    for (int i = 0; i < args.length; i++) {
      if (i + 1 >= args.length) {
        throw new IllegalArgumentException("Missing value of " + args[i] + "\n" + USAGE);
      }
      String value = args[++i];
      switch (args[i - 1]) {
        case "--settings" -> settings = value;
        case "--space" -> space = value;
        case "--encoding" -> encoding = value;
        case "--task" -> task = value;
        case "--task-file" -> task = Files.readString(Path.of(value)).trim();
        case "--output" -> output = value;
        case "--examples" -> examples = value;
        case "--log" -> log = value;
        case "--count" -> count = Integer.parseInt(value);
        default -> throw new IllegalArgumentException("Unknown option " + args[i - 1] + "\n" + USAGE);
      }
    }
    if (settings == null || space == null || encoding == null || task == null || output == null) {
      throw new IllegalArgumentException(USAGE);
    }

    Path outputFile = Path.of(output);
    Path parent = outputFile.toAbsolutePath().getParent();
    Path outputDirectory = parent != null ? parent : Path.of(".");
    Path logFile = log != null ? Path.of(log) : outputDirectory.resolve("LLM_CALLS.csv");
    YAMLParameterSpace parameterSpace = BaseAlgorithmRegistry.resolveParameterSpace(encoding, space);
    List<String> exampleConfigurations =
        examples == null
            ? List.of()
            : Files.readAllLines(Path.of(examples)).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

    var proposer =
        new ConfigurationProposer(
            LlmSettings.load(Path.of(settings)).client(), parameterSpace, yamlText(space), logFile);
    List<Proposal> proposals = proposer.propose(count, task, exampleConfigurations);

    List<String> lines = new ArrayList<>();
    for (Proposal proposal : proposals) {
      lines.add(proposal.configuration());
      System.out.println("- " + proposal.rationale());
    }
    Files.createDirectories(outputDirectory);
    Files.write(outputFile, lines);
    System.out.println(
        proposals.size()
            + " valid configurations of "
            + count
            + " written to "
            + outputFile
            + " (log "
            + logFile
            + ")");
    return proposals;
  }

  /** The text of a parameter space: a file, or a bundled one of {@code parameterSpaces/}. */
  static String yamlText(String space) throws IOException {
    Path path = Path.of(space);
    if (Files.exists(path)) {
      return Files.readString(path);
    }
    try (InputStream stream =
        ProposeConfigurationsMain.class
            .getClassLoader()
            .getResourceAsStream("parameterSpaces/" + space)) {
      if (stream == null) {
        throw new IllegalArgumentException("No parameter space " + space);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
