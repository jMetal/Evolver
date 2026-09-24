# Introspection manifest for `cli.training` (`DescribeMain`)

**Project:** Evolver (jMetal)
**Branch:** `study/uniform-training-runner` (study prototype, not merged into `main`)
**Motivation:** let an external tool (e.g. [Evolver-Studio](https://github.com/jMetal/Evolver-Studio)) discover at runtime which base algorithms, meta-optimization engines, training problems and indicators `cli.training` supports, without having to read Java source every time it changes.

## Motivation

`cli.training` resolves names (`"NSGA-II"`, `"ZDT4"`, `"Epsilon"`, ...) against four registries: `BaseAlgorithmRegistry`, `MetaAlgorithmRegistry`, `ProblemRegistry`, `IndicatorRegistry`. All of them live as `switch`/`Map` over literals, meant only to *resolve* a name to an instance at run time — not for an external process to ask "which names are valid, and what shape do they have?" without executing anything.

An external tool that wants to offer these catalogues in a UI (algorithm selectors, configuration forms) has no option today but to read the four classes' Java source and transcribe what it finds into its own language. That works once, but drifts as soon as Evolver changes — and Evolver changes regularly (new algorithms, new meta-optimization engines, new problems). The drift-detection mechanism already in place on both sides (`BaseAlgorithmRegistryCompletenessTest`/`TrainingRunnerMetaBuilderCompletenessTest` in Evolver, `TestCatalogueMatchesEvolverCheckout` in Evolver-Studio) only *warns* that something changed; it does not avoid re-reading Java by hand every time it fires.

It is called `DescribeMain`, not a method added to `TrainingRunnerMain`: it is a one-shot query with no request/status/result files, with a completely different invocation and I/O shape (a single stdout response, no polling) — the same capability-based partitioning already used to separate `cli.training` from future `cli.validation`, etc.

## Design principle: the manifest is not a fifth place to keep in sync

Two of the four registries (`ProblemRegistry`, `IndicatorRegistry`) are already `Map<String, Supplier<...>>` — their keys are directly enumerable, no design change needed. The other two (`BaseAlgorithmRegistry`, `MetaAlgorithmRegistry`) are currently a `switch` over literals, with metadata (supported encoding, per-algorithm operator-flag catalogue) that today only exists implicit in each branch's logic and in comments/javadoc — not as data anywhere.

Generating the manifest from those two registries without changing their internal shape would require writing that metadata down **a second time**, inside `DescribeMain` itself — exactly the problem this is meant to avoid, just moved from Evolver-Studio to Evolver. The proposal instead refactors `BaseAlgorithmRegistry` and `MetaAlgorithmRegistry` so their knowledge lives as a data table (one metadata record per registered algorithm), which both resolution (`resolve()`/`familyOf()`/`resolveFlat*`) and `DescribeMain` read from — a single source of truth, not two that could diverge.

## Design

### `ProblemRegistry`/`IndicatorRegistry`

Add `static Set<String> registeredNames()` to each — already a `Map`, one line.

### `BaseAlgorithmRegistry`

```java
record BaseAlgorithmDescriptor(
    String name, String encoding, List<String> requiredExtraConfigKeys) {}

private static final List<BaseAlgorithmDescriptor> ALGORITHMS = List.of(
    new BaseAlgorithmDescriptor("NSGA-II", "Double", List.of()),
    new BaseAlgorithmDescriptor("MOEAD", "Double", List.of("weightVectorFilesDirectory")));

static List<BaseAlgorithmDescriptor> registeredAlgorithms() {
  return ALGORITHMS;
}
```

`resolve()` can iterate `ALGORITHMS` instead of the current `switch`, or stay as-is and rely on the coherence test (below) to keep both from diverging — an implementation detail, doesn't affect the manifest's shape.

### `MetaAlgorithmRegistry`

```java
record OperatorFlagDescriptor(String name, String type, boolean required) {}

record MetaAlgorithmDescriptor(
    String name,
    Family family,
    boolean supportsFlat,
    boolean supportsTree,
    String operatorParameterSpaceFile,          // null when not applicable (SPEA2, SMPSO)
    List<OperatorFlagDescriptor> hardcodedOperatorFlags) {}  // empty except for SPEA2

private static final List<MetaAlgorithmDescriptor> ALGORITHMS = List.of(
    new MetaAlgorithmDescriptor(
        "NSGA-II", Family.EVOLUTIONARY, true, true,
        "NSGAIIMetaDouble.yaml", List.of()),
    new MetaAlgorithmDescriptor(
        "SPEA2", Family.EVOLUTIONARY, true, false, null, List.of(
            new OperatorFlagDescriptor("mutationProbabilityFactor", "double", false))),
    new MetaAlgorithmDescriptor(
        "AsyncNSGA-II", Family.ASYNCHRONOUS, true, false,
        "AsyncNSGAIIMetaDouble.yaml", List.of()),
    new MetaAlgorithmDescriptor(
        "SMPSO", Family.PARTICLE_SWARM, true, false, null, List.of()));

static List<MetaAlgorithmDescriptor> registeredAlgorithms() {
  return ALGORITHMS;
}
```

For `"NSGA-II"`/`"AsyncNSGA-II"`, `operatorParameterSpaceFile` points at the same `ParameterSpace` file (`NSGAIIMetaDouble.yaml`/`AsyncNSGAIIMetaDouble.yaml`) already used internally by `buildNSGAII`/`buildAsyncNSGAII` — the manifest does not duplicate that operator catalogue, it only says where it lives, in the same format `baseLevel.yamlParameterSpaceFile` already uses (an external client that already knows how to parse that format needs no new code). For `"SPEA2"`/`"SMPSO"` there is no such file — their operator flags (if any) are listed explicitly in `hardcodedOperatorFlags`, capturing what today only lives in `buildSPEA2`/`requireNoOperatorFlags`.

### Reusable resource directories

A simple listing (`File.list()`, no extra logic) of `src/main/resources/{parameterSpaces,baseLevelConfigurations,metaOptimizerConfigurations,defaultConfigurations}/` — lets an external client know which names are valid for `yamlParameterSpaceFile`/`baseLevel`/`metaSearch` without listing them by hand or assuming a naming convention.

### `request.yaml`/`baseLevel`/`metaSearch` schema (via reflection over records)

The algorithm catalogues cover *which names* are valid, but not *the shape* of the three files an external client has to produce (`request.yaml`, the `baseLevel` file, the `metaSearch` file) — that shape today only lives in `TrainingRequest`/`BaseLevelConfig`/`FlatMetaSearchConfig`/`TreeMetaSearchConfig` and in `cli-training-prototype.md`. This is exactly the kind of change that has already broken Evolver-Studio's integration once (renamed fields, fields moved to a different level).

Unlike the algorithm registries (`switch` over literals), these four types are **Java records** — their shape can be obtained via reflection (`Class.getRecordComponents()`, which gives each field's name and type) instead of writing it down by hand a second time. This avoids creating a sixth place to keep in sync: if a record gains/loses/renames a field, the manifest reflects it automatically without touching `DescribeMain`.

The one thing plain reflection does not give is optionality and default values (`writeFrequency`/`statusFrequency` = 100, `frontPlotFrequency` absent = no plot, `metaPopulationSize` optional on `FlatMetaSearchConfig`) — that is kept as a small, explicit table next to the corresponding record (e.g. a lightweight annotation or a `Map<String, Object>` of defaults in the loader itself), far more bounded than describing every field by hand:

```java
record FieldDescriptor(String name, String javaType, boolean required, String defaultValue) {}

static List<FieldDescriptor> describe(Class<? extends Record> recordType, Map<String, String> defaults) {
  return Arrays.stream(recordType.getRecordComponents())
      .map(c -> new FieldDescriptor(
          c.getName(), c.getType().getSimpleName(),
          !defaults.containsKey(c.getName()), defaults.get(c.getName())))
      .toList();
}
```

`DescribeMain` would apply this to `TrainingRequest`, `BaseLevelConfig`, `FlatMetaSearchConfig` and `TreeMetaSearchConfig`, with the already-known defaults table (`writeFrequency=100`, `statusFrequency=100`, `frontPlotFrequency` with no default, `metaPopulationSize` with no default on `FlatMetaSearchConfig`).

### `DescribeMain`

No arguments, serializes a single YAML object to stdout:

```yaml
baseAlgorithms:
  - name: NSGA-II
    encoding: Double
    requiredExtraConfigKeys: []
  - name: MOEAD
    encoding: Double
    requiredExtraConfigKeys: [weightVectorFilesDirectory]
metaAlgorithms:
  - name: NSGA-II
    family: EVOLUTIONARY
    supportsFlat: true
    supportsTree: true
    operatorParameterSpaceFile: NSGAIIMetaDouble.yaml
    hardcodedOperatorFlags: []
  - name: SPEA2
    family: EVOLUTIONARY
    supportsFlat: true
    supportsTree: false
    operatorParameterSpaceFile: null
    hardcodedOperatorFlags:
      - {name: mutationProbabilityFactor, type: double, required: false}
  - name: AsyncNSGA-II
    family: ASYNCHRONOUS
    supportsFlat: true
    supportsTree: false
    operatorParameterSpaceFile: AsyncNSGAIIMetaDouble.yaml
    hardcodedOperatorFlags: []
  - name: SMPSO
    family: PARTICLE_SWARM
    supportsFlat: true
    supportsTree: false
    operatorParameterSpaceFile: null
    hardcodedOperatorFlags: []
problems: [ZDT1, ZDT4, DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ5, DTLZ6, DTLZ7, RE31, RE32, RE33, RE34, RE35, RE36, RE37]
indicators: [Epsilon, NormalizedHypervolume, InvertedGenerationalDistancePlus]
resourceDirectories:
  parameterSpaces: [NSGAIIDouble.yaml, MOEADDouble.yaml, ...]
  baseLevelConfigurations: [Zdt4NSGAIIBaseLevel.yaml, ...]
  metaOptimizerConfigurations: [MetaNSGAIIFlatConfiguration.yaml, ...]
schemas:
  request:
    - {name: baseLevel, javaType: String, required: true, defaultValue: null}
    - {name: metaSearch, javaType: String, required: true, defaultValue: null}
    - {name: outputDirectory, javaType: String, required: true, defaultValue: null}
    - {name: writeFrequency, javaType: int, required: false, defaultValue: "100"}
    - {name: statusFrequency, javaType: int, required: false, defaultValue: "100"}
    - {name: frontPlotFrequency, javaType: Integer, required: false, defaultValue: null}
  baseLevel:
    - {name: algorithmName, javaType: String, required: true, defaultValue: null}
    - {name: populationSize, javaType: int, required: true, defaultValue: null}
    # ... rest of BaseLevelConfig
  metaSearchFlat:
    - {name: algorithm, javaType: String, required: true, defaultValue: null}
    - {name: metaMaxEvaluations, javaType: int, required: true, defaultValue: null}
    - {name: metaPopulationSize, javaType: Integer, required: false, defaultValue: null}
    - {name: numberOfCores, javaType: int, required: true, defaultValue: null}
    - {name: operatorFlags, javaType: List, required: false, defaultValue: null}
  metaSearchTree:
    - {name: algorithm, javaType: String, required: true, defaultValue: null}
    # ... rest of TreeMetaSearchConfig
```

Invoked the same way as `TrainingRunnerMain`:

```
java -cp Evolver-*-jar-with-dependencies.jar org.uma.evolver.cli.training.DescribeMain
```

Not a run: writes no `status.yaml`/`results.yaml`, accepts no request files, exits immediately after writing the YAML.

### Coherence test

A test that, for every `BaseAlgorithmDescriptor`/`MetaAlgorithmDescriptor` in `ALGORITHMS`, confirms `resolve()`/`familyOf()` does not throw `JMetalException` for that name (with minimal valid values for the other parameters) — keeps the metadata table from drifting from the `switch`/resolution logic if both coexist instead of one being derived from the other. Complements (not necessarily replaces) `BaseAlgorithmRegistryCompletenessTest`/`TrainingRunnerMetaBuilderCompletenessTest`, which still cover the case "there is a Java algorithm class no registry knows about yet".

## Out of scope for this proposal

- Exposing the internal detail of each `yamlParameterSpaceFile`/`operatorParameterSpaceFile` (which concrete parameters each one has) — a client that already knows how to parse the `ParameterSpace` format (like Evolver-Studio) can read those files directly; the manifest only needs to say *which ones exist and what they correspond to*.
- Any change to the `request.yaml`/`TrainingRunnerMain` contract already established in `cli-training-prototype.md` — this document is purely additive; the manifest's schema section is an automatic projection of those same records, not a redefinition.
- Introspection for `cli.validation` or other future sibling packages, should they come to exist.

## Expected consumption from Evolver-Studio

- `evolver_studio/catalogue.py` stops maintaining `BASE_ALGORITHMS`/`META_ALGORITHMS` as literal lists and instead populates them by invoking `DescribeMain` (the same subprocess pattern already used for `TrainingRunnerMain`), caching the result per session.
- `evolver_studio/request.py` can use the manifest's `schemas` section to **validate** (not necessarily generate) the YAML it builds before launching it — catching a renamed/relevelled field on the spot instead of discovering it via a `ClassNotFoundException`/`JMetalException` when running against the real jar.
- The current drift-detection test (`TestCatalogueMatchesEvolverCheckout`, which compares the hardcoded catalogue against the checkout's real files) is replaced by a smoke test that introspection still works — there is no longer a hardcoded catalogue to compare against anything.
