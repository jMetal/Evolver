package org.uma.evolver.example.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Catalog of the representative 7k NSGA-II configurations reported in the manuscript appendix.
 *
 * <p>The catalog exposes the default baseline, the four representative configurations, and helper
 * methods to derive knockout ablations and forward candidates that mimic the stepwise
 * source-to-target ablation used by irace.
 */
public final class RepresentativeConfigurationCatalog {
  /**
   * Immutable definition of one validation configuration.
   *
   * @param tag stable identifier used by the validation studies
   * @param description human-readable description
   * @param parameterString CLI-compatible parameter string for {@code DoubleNSGAII}
   */
  public record ConfigurationSpec(String tag, String description, String parameterString) {}

  /**
   * Candidate generated during one forward-ablation step.
   *
   * @param configuration generated configuration
   * @param changedParameters parameters copied from the target at this step
   */
  public record ForwardStepCandidate(
      ConfigurationSpec configuration, List<String> changedParameters) {}

  private record ParameterDefinition(
      String name, boolean dependent, Predicate<Map<String, String>> activeCondition) {}

  private static final String STANDARD_PARAMETER_STRING =
      String.join(
          " ",
          "--algorithmResult population",
          "--createInitialSolutions default",
          "--variation crossoverAndMutationVariation",
          "--offspringPopulationSize 100",
          "--crossover SBX",
          "--crossoverProbability 0.9",
          "--crossoverRepairStrategy bounds",
          "--sbxDistributionIndex 20.0",
          "--mutation polynomial",
          "--mutationProbabilityFactor 1.0",
          "--mutationRepairStrategy bounds",
          "--polynomialMutationDistributionIndex 20.0",
          "--selection tournament",
          "--selectionTournamentSize 2");

  private static final List<ConfigurationSpec> REPRESENTATIVE_CONFIGURATIONS =
      List.of(
          new ConfigurationSpec(
              "Complete-RE3D",
              "Representative configuration extracted from the complete-front RE3D 7k campaign",
              String.join(
                  " ",
                  "--algorithmResult externalArchive",
                  "--populationSizeWithArchive 161",
                  "--archiveType unboundedArchive",
                  "--createInitialSolutions cauchy",
                  "--offspringPopulationSize 5",
                  "--variation crossoverAndMutationVariation",
                  "--crossover laplace",
                  "--crossoverProbability 0.9615663946714375",
                  "--crossoverRepairStrategy bounds",
                  "--laplaceCrossoverScale 0.4642099390483123",
                  "--mutation levyFlight",
                  "--mutationProbabilityFactor 0.9923224334691945",
                  "--mutationRepairStrategy bounds",
                  "--levyFlightMutationBeta 1.0232779923931032",
                  "--levyFlightMutationStepSize 0.918794510661894",
                  "--selection ranking")),
          new ConfigurationSpec(
              "Extreme-RE3D",
              "Representative configuration extracted from the extreme-point RE3D 7k campaign",
              String.join(
                  " ",
                  "--algorithmResult externalArchive",
                  "--populationSizeWithArchive 129",
                  "--archiveType unboundedArchive",
                  "--createInitialSolutions cauchy",
                  "--offspringPopulationSize 50",
                  "--variation crossoverAndMutationVariation",
                  "--crossover laplace",
                  "--crossoverProbability 0.8393366475584481",
                  "--crossoverRepairStrategy bounds",
                  "--laplaceCrossoverScale 0.4819615489847",
                  "--mutation powerLaw",
                  "--mutationProbabilityFactor 0.6103858727595163",
                  "--mutationRepairStrategy bounds",
                  "--powerLawMutationDelta 4.867134573461407",
                  "--selection boltzmann",
                  "--boltzmannTemperature 80.20746963695628")),
          new ConfigurationSpec(
              "Complete-RWA3D",
              "Representative configuration extracted from the complete-front RWA3D 7k campaign",
              String.join(
                  " ",
                  "--algorithmResult externalArchive",
                  "--populationSizeWithArchive 10",
                  "--archiveType unboundedArchive",
                  "--createInitialSolutions latinHypercubeSampling",
                  "--offspringPopulationSize 10",
                  "--variation crossoverAndMutationVariation",
                  "--crossover laplace",
                  "--crossoverProbability 0.718608920496554",
                  "--crossoverRepairStrategy round",
                  "--laplaceCrossoverScale 0.3741039932602056",
                  "--mutation powerLaw",
                  "--mutationProbabilityFactor 1.8730419254665496",
                  "--mutationRepairStrategy round",
                  "--powerLawMutationDelta 6.691352846910596",
                  "--selection boltzmann",
                  "--boltzmannTemperature 1.259821579456496")),
          new ConfigurationSpec(
              "Extreme-RWA3D",
              "Representative configuration extracted from the extreme-point RWA3D 7k campaign",
              String.join(
                  " ",
                  "--algorithmResult externalArchive",
                  "--populationSizeWithArchive 14",
                  "--archiveType unboundedArchive",
                  "--createInitialSolutions sobol",
                  "--offspringPopulationSize 1",
                  "--variation crossoverAndMutationVariation",
                  "--crossover laplace",
                  "--crossoverProbability 0.48929336020292413",
                  "--crossoverRepairStrategy round",
                  "--laplaceCrossoverScale 0.43756191193250993",
                  "--mutation powerLaw",
                  "--mutationProbabilityFactor 1.5612013342020188",
                  "--mutationRepairStrategy round",
                  "--powerLawMutationDelta 6.893826848335108",
                  "--selection stochasticUniversalSampling")));

  private static final List<ParameterDefinition> FORWARD_PARAMETER_DEFINITIONS =
      List.of(
          new ParameterDefinition("algorithmResult", false, params -> true),
          new ParameterDefinition(
              "populationSizeWithArchive",
              true,
              params -> "externalArchive".equals(params.get("algorithmResult"))),
          new ParameterDefinition(
              "archiveType",
              true,
              params -> "externalArchive".equals(params.get("algorithmResult"))),
          new ParameterDefinition("createInitialSolutions", false, params -> true),
          new ParameterDefinition("offspringPopulationSize", false, params -> true),
          new ParameterDefinition("crossover", false, params -> true),
          new ParameterDefinition("crossoverProbability", false, params -> true),
          new ParameterDefinition("crossoverRepairStrategy", false, params -> true),
          new ParameterDefinition(
              "sbxDistributionIndex", true, params -> "SBX".equals(params.get("crossover"))),
          new ParameterDefinition(
              "blxAlphaCrossoverAlpha",
              true,
              params -> "blxAlpha".equals(params.get("crossover"))),
          new ParameterDefinition(
              "blxAlphaBetaCrossoverAlpha",
              true,
              params -> "blxAlphaBeta".equals(params.get("crossover"))),
          new ParameterDefinition(
              "blxAlphaBetaCrossoverBeta",
              true,
              params -> "blxAlphaBeta".equals(params.get("crossover"))),
          new ParameterDefinition(
              "laplaceCrossoverScale",
              true,
              params -> "laplace".equals(params.get("crossover"))),
          new ParameterDefinition(
              "fuzzyRecombinationCrossoverAlpha",
              true,
              params -> "fuzzyRecombination".equals(params.get("crossover"))),
          new ParameterDefinition(
              "pcxCrossoverZeta", true, params -> "PCX".equals(params.get("crossover"))),
          new ParameterDefinition(
              "pcxCrossoverEta", true, params -> "PCX".equals(params.get("crossover"))),
          new ParameterDefinition(
              "undcCrossoverZeta", true, params -> "UNDC".equals(params.get("crossover"))),
          new ParameterDefinition(
              "undcCrossoverEta", true, params -> "UNDC".equals(params.get("crossover"))),
          new ParameterDefinition("mutation", false, params -> true),
          new ParameterDefinition("mutationProbabilityFactor", false, params -> true),
          new ParameterDefinition("mutationRepairStrategy", false, params -> true),
          new ParameterDefinition(
              "uniformMutationPerturbation",
              true,
              params -> "uniform".equals(params.get("mutation"))),
          new ParameterDefinition(
              "polynomialMutationDistributionIndex",
              true,
              params -> "polynomial".equals(params.get("mutation"))),
          new ParameterDefinition(
              "linkedPolynomialMutationDistributionIndex",
              true,
              params -> "linkedPolynomial".equals(params.get("mutation"))),
          new ParameterDefinition(
              "nonUniformMutationPerturbation",
              true,
              params -> "nonUniform".equals(params.get("mutation"))),
          new ParameterDefinition(
              "levyFlightMutationBeta",
              true,
              params -> "levyFlight".equals(params.get("mutation"))),
          new ParameterDefinition(
              "levyFlightMutationStepSize",
              true,
              params -> "levyFlight".equals(params.get("mutation"))),
          new ParameterDefinition(
              "powerLawMutationDelta",
              true,
              params -> "powerLaw".equals(params.get("mutation"))),
          new ParameterDefinition("selection", false, params -> true),
          new ParameterDefinition(
              "selectionTournamentSize",
              true,
              params -> "tournament".equals(params.get("selection"))),
          new ParameterDefinition(
              "boltzmannTemperature",
              true,
              params -> "boltzmann".equals(params.get("selection"))));

  private static final List<String> PARAMETER_STRING_ORDER =
      List.of(
          "algorithmResult",
          "populationSizeWithArchive",
          "archiveType",
          "createInitialSolutions",
          "variation",
          "offspringPopulationSize",
          "crossover",
          "crossoverProbability",
          "crossoverRepairStrategy",
          "sbxDistributionIndex",
          "blxAlphaCrossoverAlpha",
          "blxAlphaBetaCrossoverAlpha",
          "blxAlphaBetaCrossoverBeta",
          "laplaceCrossoverScale",
          "fuzzyRecombinationCrossoverAlpha",
          "pcxCrossoverZeta",
          "pcxCrossoverEta",
          "undcCrossoverZeta",
          "undcCrossoverEta",
          "mutation",
          "mutationProbabilityFactor",
          "mutationRepairStrategy",
          "uniformMutationPerturbation",
          "polynomialMutationDistributionIndex",
          "linkedPolynomialMutationDistributionIndex",
          "nonUniformMutationPerturbation",
          "levyFlightMutationBeta",
          "levyFlightMutationStepSize",
          "powerLawMutationDelta",
          "selection",
          "selectionTournamentSize",
          "boltzmannTemperature");

  private RepresentativeConfigurationCatalog() {
    // Utility class
  }

  /**
   * Returns the standard NSGA-II baseline used in the manuscript.
   *
   * @return the baseline configuration
   */
  public static ConfigurationSpec standard() {
    ConfigurationSpec result;

    result =
        new ConfigurationSpec(
            "NSGAII-Standard",
            "Default NSGA-II with SBX, polynomial mutation, tournament selection, and no archive",
            STANDARD_PARAMETER_STRING);

    return result;
  }

  /**
   * Returns the four representative configurations used by the compact external validation study.
   *
   * @return a defensive copy of the representative configuration list
   */
  public static List<ConfigurationSpec> representativeConfigurations() {
    List<ConfigurationSpec> result;

    result = new ArrayList<>(REPRESENTATIVE_CONFIGURATIONS);

    return result;
  }

  /**
   * Looks up one representative configuration by tag.
   *
   * @param tag stable configuration tag
   * @return the matching configuration
   * @throws IllegalArgumentException if the tag does not belong to the catalog
   */
  public static ConfigurationSpec configurationByTag(String tag) {
    ConfigurationSpec result;

    result = null;
    for (ConfigurationSpec spec : REPRESENTATIVE_CONFIGURATIONS) {
      if (spec.tag().equals(tag)) {
        result = spec;
      }
    }

    if (result == null) {
      throw new IllegalArgumentException("Unknown representative configuration tag: " + tag);
    }

    return result;
  }

  /**
   * Creates the three compact knockout ablations (target with one block reset to default).
   *
   * @param baseTag representative configuration tag to ablate
   * @return archive, crossover, and mutation reset variants derived from the selected base
   */
  public static List<ConfigurationSpec> ablationVariants(String baseTag) {
    List<ConfigurationSpec> result;
    ConfigurationSpec base;

    base = configurationByTag(baseTag);
    result =
        List.of(
            resetArchive(base),
            resetCrossover(base),
            resetMutation(base));

    return result;
  }

  /**
   * Lists the remaining parameter changes that can still be applied from {@code current} toward
   * {@code target}.
   *
   * <p>The result follows the irace ablation convention: only parameters active in the target
   * configuration are considered explicit candidates, while deactivated conditional parameters are
   * handled when their parent selector changes.
   *
   * @param current current forward-ablation state
   * @param target target configuration
   * @return parameter names that can be copied from the target at the next step
   */
  public static List<String> forwardRemainingParameters(
      ConfigurationSpec current, ConfigurationSpec target) {
    List<String> result;
    LinkedHashMap<String, String> currentParams;
    LinkedHashMap<String, String> targetParams;

    result = new ArrayList<>();
    currentParams = parseParameterString(current.parameterString());
    targetParams = parseParameterString(target.parameterString());

    for (ParameterDefinition definition : FORWARD_PARAMETER_DEFINITIONS) {
      String currentValue;
      String targetValue;

      currentValue = currentParams.get(definition.name());
      targetValue = targetParams.get(definition.name());
      if ((targetValue == null) || targetValue.equals(currentValue)) {
        continue;
      }
      if (definition.dependent() && !definition.activeCondition().test(currentParams)) {
        continue;
      }
      result.add(definition.name());
    }

    return result;
  }

  /**
   * Generates all valid one-step forward candidates from {@code current} toward {@code target}.
   *
   * <p>Each candidate copies one remaining target parameter. If that change activates or deactivates
   * conditional parameters, the dependent values are repaired so that the generated configuration is
   * valid and mirrors the target whenever the corresponding branch becomes active.
   *
   * @param current current forward-ablation state
   * @param target target configuration
   * @param stepIndex one-based ablation step index
   * @return valid forward candidates for the current step
   */
  public static List<ForwardStepCandidate> forwardStepCandidates(
      ConfigurationSpec current, ConfigurationSpec target, int stepIndex) {
    List<ForwardStepCandidate> result;
    LinkedHashMap<String, String> currentParams;
    LinkedHashMap<String, String> targetParams;
    LinkedHashSet<String> seenParameterStrings;

    result = new ArrayList<>();
    currentParams = parseParameterString(current.parameterString());
    targetParams = parseParameterString(target.parameterString());
    seenParameterStrings = new LinkedHashSet<>();

    for (String parameterName : forwardRemainingParameters(current, target)) {
      LinkedHashMap<String, String> candidateParams;
      List<String> changedParameters;
      String parameterString;
      ConfigurationSpec candidateSpec;

      candidateParams = new LinkedHashMap<>(currentParams);
      candidateParams.put(parameterName, targetParams.get(parameterName));
      changedParameters = repairConditionalParameters(candidateParams, targetParams, parameterName);
      parameterString = toParameterString(candidateParams);
      if (!seenParameterStrings.add(parameterString)) {
        continue;
      }

      if (sameParameterStrings(parameterString, target.parameterString())) {
        candidateSpec = target;
      } else {
        candidateSpec =
            new ConfigurationSpec(
                forwardCandidateTag(target.tag(), stepIndex, changedParameters),
                "Forward ablation candidate from "
                    + current.tag()
                    + " toward "
                    + target.tag()
                    + " by changing "
                    + String.join("+", changedParameters),
                parameterString);
      }
      result.add(new ForwardStepCandidate(candidateSpec, List.copyOf(changedParameters)));
    }

    return result;
  }

  /**
   * Checks whether two configurations encode the same active parameter values.
   *
   * @param first first configuration
   * @param second second configuration
   * @return {@code true} when both parameter maps are equal
   */
  public static boolean sameConfiguration(ConfigurationSpec first, ConfigurationSpec second) {
    boolean result;

    result = sameParameterStrings(first.parameterString(), second.parameterString());

    return result;
  }

  private static ConfigurationSpec resetArchive(ConfigurationSpec base) {
    ConfigurationSpec result;
    LinkedHashMap<String, String> params;
    String description;

    params = parseParameterString(base.parameterString());
    params.put("algorithmResult", "population");
    params.remove("populationSizeWithArchive");
    params.remove("archiveType");
    description =
        base.description() + " with the archive block reset to the standard NSGA-II default";
    result =
        new ConfigurationSpec(
            base.tag() + "-ArchiveReset",
            description,
            toParameterString(params));

    return result;
  }

  private static ConfigurationSpec resetCrossover(ConfigurationSpec base) {
    ConfigurationSpec result;
    LinkedHashMap<String, String> params;
    String description;

    params = parseParameterString(base.parameterString());
    params.put("crossover", "SBX");
    params.put("crossoverProbability", "0.9");
    params.put("crossoverRepairStrategy", "bounds");
    params.put("sbxDistributionIndex", "20.0");
    removeKeys(
        params,
        "blxAlphaCrossoverAlpha",
        "blxAlphaBetaCrossoverAlpha",
        "blxAlphaBetaCrossoverBeta",
        "laplaceCrossoverScale",
        "fuzzyRecombinationCrossoverAlpha",
        "pcxCrossoverZeta",
        "pcxCrossoverEta",
        "undcCrossoverZeta",
        "undcCrossoverEta");
    description =
        base.description() + " with the crossover block reset to the standard NSGA-II default";
    result =
        new ConfigurationSpec(
            base.tag() + "-CrossoverReset",
            description,
            toParameterString(params));

    return result;
  }

  private static ConfigurationSpec resetMutation(ConfigurationSpec base) {
    ConfigurationSpec result;
    LinkedHashMap<String, String> params;
    String description;

    params = parseParameterString(base.parameterString());
    params.put("mutation", "polynomial");
    params.put("mutationProbabilityFactor", "1.0");
    params.put("mutationRepairStrategy", "bounds");
    params.put("polynomialMutationDistributionIndex", "20.0");
    removeKeys(
        params,
        "uniformMutationPerturbation",
        "linkedPolynomialMutationDistributionIndex",
        "nonUniformMutationPerturbation",
        "levyFlightMutationBeta",
        "levyFlightMutationStepSize",
        "powerLawMutationDelta");
    description =
        base.description() + " with the mutation block reset to the standard NSGA-II default";
    result =
        new ConfigurationSpec(
            base.tag() + "-MutationReset",
            description,
            toParameterString(params));

    return result;
  }

  private static LinkedHashMap<String, String> parseParameterString(String parameterString) {
    LinkedHashMap<String, String> result;
    String[] tokens;
    int index;

    result = new LinkedHashMap<>();
    tokens = parameterString.trim().split("\\s+");
    index = 0;
    while (index < tokens.length) {
      if (tokens[index].startsWith("--")) {
        String key;
        String value;

        key = tokens[index].substring(2);
        value = "";
        if ((index + 1 < tokens.length) && !tokens[index + 1].startsWith("--")) {
          value = tokens[index + 1];
          index += 2;
        } else {
          index += 1;
        }
        result.put(key, value);
      } else {
        index += 1;
      }
    }

    return result;
  }

  private static String toParameterString(LinkedHashMap<String, String> params) {
    String result;
    StringBuilder builder;
    LinkedHashSet<String> orderedKeys;

    builder = new StringBuilder();
    orderedKeys = new LinkedHashSet<>(PARAMETER_STRING_ORDER);
    orderedKeys.addAll(params.keySet());
    for (String key : orderedKeys) {
      if (params.containsKey(key)) {
        String value;

        value = params.get(key);
        if ((builder.length() > 0) && !builder.toString().endsWith(" ")) {
          builder.append(' ');
        }
        builder.append("--").append(key);
        if ((value != null) && !value.isBlank()) {
          builder.append(' ').append(value);
        }
      }
    }
    result = builder.toString();

    return result;
  }

  private static void removeKeys(LinkedHashMap<String, String> params, String... keys) {
    for (String key : keys) {
      params.remove(key);
    }
  }

  private static List<String> repairConditionalParameters(
      LinkedHashMap<String, String> candidateParams,
      LinkedHashMap<String, String> targetParams,
      String changedParameter) {
    List<String> result;

    result = new ArrayList<>();
    result.add(changedParameter);

    for (ParameterDefinition definition : FORWARD_PARAMETER_DEFINITIONS) {
      boolean active;
      boolean present;

      if (!definition.dependent()) {
        continue;
      }

      active = definition.activeCondition().test(candidateParams);
      present = candidateParams.containsKey(definition.name());
      if (active && !present && targetParams.containsKey(definition.name())) {
        candidateParams.put(definition.name(), targetParams.get(definition.name()));
        result.add(definition.name());
      } else if (!active && present) {
        candidateParams.remove(definition.name());
        result.add(definition.name());
      }
    }

    return result;
  }

  private static boolean sameParameterStrings(String first, String second) {
    boolean result;

    result = parseParameterString(first).equals(parseParameterString(second));

    return result;
  }

  private static String forwardCandidateTag(
      String targetTag, int stepIndex, List<String> changedParameters) {
    String result;
    String suffix;

    suffix =
        changedParameters.stream()
            .map(RepresentativeConfigurationCatalog::sanitizeTagComponent)
            .reduce((left, right) -> left + "+" + right)
            .orElse("change");
    result = targetTag + "-Forward-S" + String.format("%02d", stepIndex) + "-" + suffix;

    return result;
  }

  private static String sanitizeTagComponent(String component) {
    String result;

    result = component.replaceAll("[^A-Za-z0-9]+", "_");

    return result;
  }

  /**
   * Returns a CSV-friendly representation of the catalog entries.
   *
   * @param specs configurations to serialize
   * @return rows containing tag, description, and parameter string
   */
  public static List<Map<String, String>> asRows(List<ConfigurationSpec> specs) {
    List<Map<String, String>> result;

    result = new ArrayList<>();
    for (ConfigurationSpec spec : specs) {
      Map<String, String> row;

      row = new LinkedHashMap<>();
      row.put("tag", spec.tag());
      row.put("description", spec.description());
      row.put("parameter_string", spec.parameterString());
      result.add(row);
    }

    return result;
  }
}
