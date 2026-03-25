package org.uma.evolver.example.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Catalog of the representative 7k NSGA-II configurations reported in the manuscript appendix.
 *
 * <p>The catalog exposes the default baseline, the four representative configurations, and helper
 * methods to derive compact real ablations by resetting archive, crossover, or mutation blocks to
 * the standard NSGA-II defaults.
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
   * Creates the three compact real ablations used in the manuscript extension.
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
    orderedKeys = new LinkedHashSet<>(params.keySet());
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
