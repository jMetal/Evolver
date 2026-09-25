package org.uma.evolver.cli.training;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.cli.IndicatorRegistry;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.solving.SolveRequest;
import org.uma.evolver.cli.solving.SolveRequestYamlLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Prints a single, machine-readable YAML manifest to stdout describing everything {@code
 * cli.training} can resolve today: registered base algorithms, meta-optimizer algorithms,
 * training problems, indicators, the names available under each reusable resource directory, and
 * the shape of {@code request.yaml}/{@code baseLevel}/{@code metaSearch} themselves — plus the
 * shape of a {@code cli.solving} request ({@code solveRequest}), which uses the same registries.
 *
 * <p>Not a run: takes no arguments, writes no {@code status.yaml}/{@code results.yaml}, and exits
 * as soon as the manifest is written. Exists so an external tool (e.g. Evolver-Studio) can
 * discover what is runnable without reading this package's Java source — see {@code
 * docs/proposals/cli-describe-manifest.md} for the design rationale (the data behind this comes
 * from the registries' own {@code registeredAlgorithms()}/{@code registeredNames()} and from
 * reflection over the request records, not a second hand-maintained copy).
 *
 * <p>Usage: {@code java -cp Evolver-*-jar-with-dependencies.jar
 * org.uma.evolver.cli.training.DescribeMain}
 */
public final class DescribeMain {

  private DescribeMain() {}

  public static void main(String[] args) {
    System.out.print(new Yaml(dumperOptions()).dump(manifest()));
  }

  private static DumperOptions dumperOptions() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return options;
  }

  /** Package-private for {@link DescribeMainTest}; {@link #main} is the only real entry point. */
  static Map<String, Object> manifest() {
    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("baseAlgorithms", baseAlgorithms());
    manifest.put("metaAlgorithms", metaAlgorithms());
    manifest.put("problems", ProblemRegistry.registeredNames().stream().sorted().toList());
    manifest.put("indicators", IndicatorRegistry.registeredNames().stream().sorted().toList());
    manifest.put("resourceDirectories", resourceDirectories());
    manifest.put("schemas", schemas());
    return manifest;
  }

  private static List<Object> baseAlgorithms() {
    return BaseAlgorithmRegistry.registeredAlgorithms().stream()
        .map(
            algorithm -> {
              Map<String, Object> data = new LinkedHashMap<>();
              data.put("name", algorithm.name());
              data.put("encoding", algorithm.encoding());
              data.put("requiredExtraConfigKeys", algorithm.requiredExtraConfigKeys());
              return (Object) data;
            })
        .toList();
  }

  private static List<Object> metaAlgorithms() {
    return MetaAlgorithmRegistry.registeredAlgorithms().stream()
        .map(
            algorithm -> {
              Map<String, Object> data = new LinkedHashMap<>();
              data.put("name", algorithm.name());
              data.put("family", algorithm.family().name());
              data.put("supportsFlat", true);
              data.put("supportsTree", algorithm.supportsTree());
              data.put("operatorParameterSpaceFile", algorithm.operatorParameterSpaceFile());
              data.put(
                  "hardcodedOperatorFlags",
                  algorithm.hardcodedOperatorFlags().stream()
                      .map(
                          flag -> {
                            Map<String, Object> flagData = new LinkedHashMap<>();
                            flagData.put("name", flag.name());
                            flagData.put("type", flag.type());
                            flagData.put("required", flag.required());
                            return (Object) flagData;
                          })
                      .toList());
              return (Object) data;
            })
        .toList();
  }

  private static Map<String, Object> resourceDirectories() {
    Map<String, Object> directories = new LinkedHashMap<>();
    directories.put("parameterSpaces", ResourceDirectoryLister.list("parameterSpaces"));
    directories.put(
        "baseLevelConfigurations", ResourceDirectoryLister.list("baseLevelConfigurations"));
    directories.put(
        "metaOptimizerConfigurations",
        ResourceDirectoryLister.list("metaOptimizerConfigurations"));
    directories.put("defaultConfigurations", ResourceDirectoryLister.list("defaultConfigurations"));
    return directories;
  }

  private static Map<String, Object> schemas() {
    Map<String, String> requestDefaults = new HashMap<>();
    requestDefaults.put("writeFrequency", "100");
    requestDefaults.put("statusFrequency", "100");
    requestDefaults.put("frontPlotFrequency", null); // optional, no default: absent means no plot
    // In request.yaml these are file names, resolved by TrainingRequestYamlLoader before
    // TrainingRequest is built — see RequestSchemaDescriptor#describe(Class, Map, Map).
    Map<String, String> requestTypeOverrides =
        Map.of("baseLevel", "String", "metaSearch", "String");

    Map<String, String> baseLevelDefaults = new HashMap<>();
    baseLevelDefaults.put("extraConfig", null); // optional; only required by algorithms that need it

    // Optional in both encodings: falls back to the registry's default meta population size.
    Map<String, String> metaSearchDefaults =
        Map.of(
            "metaPopulationSize", String.valueOf(MetaAlgorithmRegistry.DEFAULT_POPULATION_SIZE));

    // Optional fields of a solve request (cli.solving); exactly one of configuration and
    // configurationFile must be given, and indicatorNames requires referenceFrontFileName.
    Map<String, String> solveRequestDefaults = new HashMap<>();
    solveRequestDefaults.put("encoding", SolveRequestYamlLoader.DEFAULT_ENCODING);
    solveRequestDefaults.put("extraConfig", null);
    solveRequestDefaults.put("configuration", null);
    solveRequestDefaults.put("configurationFile", null);
    solveRequestDefaults.put("referenceFrontFileName", null);
    solveRequestDefaults.put(
        "numberOfIndependentRuns",
        String.valueOf(SolveRequestYamlLoader.DEFAULT_NUMBER_OF_INDEPENDENT_RUNS));
    solveRequestDefaults.put("seed", null); // absent: drawn at random
    solveRequestDefaults.put("indicatorNames", null);

    Map<String, Object> schemas = new LinkedHashMap<>();
    schemas.put(
        "request",
        fieldDescriptorsAsMaps(
            RequestSchemaDescriptor.describe(
                TrainingRequest.class, requestDefaults, requestTypeOverrides)));
    schemas.put(
        "baseLevel",
        fieldDescriptorsAsMaps(
            RequestSchemaDescriptor.describe(BaseLevelConfig.class, baseLevelDefaults)));
    schemas.put(
        "metaSearchFlat",
        fieldDescriptorsAsMaps(
            RequestSchemaDescriptor.describe(FlatMetaSearchConfig.class, metaSearchDefaults)));
    schemas.put(
        "metaSearchTree",
        fieldDescriptorsAsMaps(
            RequestSchemaDescriptor.describe(TreeMetaSearchConfig.class, metaSearchDefaults)));
    schemas.put(
        "solveRequest",
        fieldDescriptorsAsMaps(
            RequestSchemaDescriptor.describe(SolveRequest.class, solveRequestDefaults)));
    return schemas;
  }

  private static List<Object> fieldDescriptorsAsMaps(
      List<RequestSchemaDescriptor.FieldDescriptor> fields) {
    return fields.stream()
        .map(
            field -> {
              Map<String, Object> data = new LinkedHashMap<>();
              data.put("name", field.name());
              data.put("javaType", field.javaType());
              data.put("required", field.required());
              data.put("defaultValue", field.defaultValue());
              return (Object) data;
            })
        .toList();
  }
}
