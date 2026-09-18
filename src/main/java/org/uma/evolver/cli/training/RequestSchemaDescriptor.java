package org.uma.evolver.cli.training;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Describes the shape of a {@code request.yaml}/{@code baseLevel}/{@code metaSearch} record by
 * reflecting over its {@link RecordComponent}s, instead of a second, hand-maintained copy of the
 * same fields — see {@code docs/proposals/cli-describe-manifest.md}. Reflection alone cannot tell
 * which components are optional or what their default is (that information lives in the loader
 * that applies it, not in the record itself), so callers pass it in explicitly as {@code
 * defaultValues}.
 */
final class RequestSchemaDescriptor {

  /**
   * @param name the record component's name, exactly as it appears in the YAML file
   * @param javaType the record component's simple type name (e.g. {@code "String"}, {@code
   *     "int"}, {@code "List"})
   * @param required false when {@code defaultValues} has an entry for this field
   * @param defaultValue the value applied when the field is absent, or null when {@code required}
   *     is true or the default is itself "absent" (e.g. {@code frontPlotFrequency})
   */
  record FieldDescriptor(String name, String javaType, boolean required, String defaultValue) {}

  private RequestSchemaDescriptor() {}

  static List<FieldDescriptor> describe(
      Class<? extends Record> recordType, Map<String, String> defaultValues) {
    return describe(recordType, defaultValues, Map.of());
  }

  /**
   * Like {@link #describe(Class, Map)}, but with {@code typeOverrides} replacing a component's
   * reflected Java type for fields whose YAML shape does not match the record's own field type —
   * {@code TrainingRequest.baseLevel()}/{@code metaSearch()} are the case this exists for: by the
   * time {@link TrainingRequestYamlLoader} builds a {@link TrainingRequest}, it has already
   * resolved the file name written in {@code request.yaml} into the loaded {@link BaseLevelConfig}/
   * {@link MetaSearchConfig}, so reflecting on {@link TrainingRequest} alone would describe the
   * post-load object, not what a caller must actually write in the YAML file (a {@code String}).
   */
  static List<FieldDescriptor> describe(
      Class<? extends Record> recordType,
      Map<String, String> defaultValues,
      Map<String, String> typeOverrides) {
    return Arrays.stream(recordType.getRecordComponents())
        .map(
            component ->
                new FieldDescriptor(
                    component.getName(),
                    typeOverrides.getOrDefault(
                        component.getName(), component.getType().getSimpleName()),
                    !defaultValues.containsKey(component.getName()),
                    defaultValues.get(component.getName())))
        .toList();
  }
}
