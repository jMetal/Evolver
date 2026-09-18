# Manifiesto de introspección para `cli.training` (`DescribeMain`)

**Proyecto:** Evolver (jMetal)
**Rama:** `study/uniform-training-runner` (prototipo de estudio, no integrado en `main`)
**Motivación:** permitir que una herramienta externa (p. ej. [Evolver-Studio](https://github.com/jMetal/Evolver-Studio)) descubra en tiempo de ejecución qué algoritmos base, motores de meta-optimización, problemas de entrenamiento e indicadores admite `cli.training`, sin tener que leer el código Java fuente cada vez que cambia.

## Motivación

`cli.training` resuelve nombres (`"NSGA-II"`, `"ZDT4"`, `"Epsilon"`, ...) contra cuatro registros: `BaseAlgorithmRegistry`, `MetaAlgorithmRegistry`, `ProblemRegistry`, `IndicatorRegistry`. Todos ellos viven como `switch`/`Map` sobre literales, pensados solo para *resolver* un nombre a una instancia en tiempo de ejecución de un run — no para que un proceso externo pregunte "¿qué nombres son válidos, y con qué forma?" sin ejecutar nada.

Una herramienta externa que quiera ofrecer estos catálogos en una UI (selectores de algoritmo, formularios de configuración) no tiene hoy más opción que leer el código Java fuente de las cuatro clases y transcribir lo que encuentra a su propio lenguaje. Eso funciona una vez, pero se desincroniza en cuanto Evolver cambia — y Evolver cambia con regularidad (algoritmos nuevos, motores de meta-optimización nuevos, problemas nuevos). El mecanismo de drift-detection ya existente en ambos proyectos (`BaseAlgorithmRegistryCompletenessTest`/`TrainingRunnerMetaBuilderCompletenessTest` en Evolver, `TestCatalogueMatchesEvolverCheckout` en Evolver-Studio) solo *avisa* de que algo cambió; no evita releer Java a mano cada vez que salta.

Se llama `DescribeMain` y no un método añadido a `TrainingRunnerMain`: es una consulta puntual sin ficheros de petición/estado/resultado, con una forma de invocación e I/O completamente distinta (una única respuesta por stdout, sin polling) — mismo criterio de partición por capacidad ya usado para separar `cli.training` de futuros `cli.validation`, etc.

## Principio de diseño: el manifiesto no es un quinto sitio que mantener sincronizado

Dos de los cuatro registros (`ProblemRegistry`, `IndicatorRegistry`) ya son `Map<String, Supplier<...>>` — sus claves son directamente enumerables, sin cambios de diseño. Los otros dos (`BaseAlgorithmRegistry`, `MetaAlgorithmRegistry`) son actualmente `switch` sobre literales, con metadatos (codificación soportada, catálogo de operator-flags por algoritmo) que hoy solo existen implícitos en la lógica de cada rama y en comentarios/javadoc — no como dato en ningún sitio.

Generar el manifiesto a partir de esos dos registros sin cambiar su forma interna exigiría mantener esos metadatos escritos **por segunda vez**, en el propio `DescribeMain` — exactamente el problema que se quiere evitar, solo movido de Evolver-Studio a Evolver. La propuesta es en cambio refactorizar `BaseAlgorithmRegistry` y `MetaAlgorithmRegistry` para que su conocimiento viva como una tabla de datos (un record de metadatos por algoritmo registrado), de la que tanto la resolución (`resolve()`/`familyOf()`/`resolveFlat*`) como `DescribeMain` leen — una única fuente de verdad, no dos que puedan divergir.

## Diseño

### `ProblemRegistry`/`IndicatorRegistry`

Añadir `static Set<String> registeredNames()` a cada uno — ya son `Map`, es una línea.

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

`resolve()` puede iterar `ALGORITHMS` en vez del `switch` actual, o quedarse como está y depender del test de coherencia (más abajo) para que ambos no diverjan — decisión de implementación, no afecta a la forma del manifiesto.

### `MetaAlgorithmRegistry`

```java
record OperatorFlagDescriptor(String name, String type, boolean required) {}

record MetaAlgorithmDescriptor(
    String name,
    Family family,
    boolean supportsFlat,
    boolean supportsTree,
    String operatorParameterSpaceFile,          // null si no aplica (SPEA2, SMPSO)
    List<OperatorFlagDescriptor> hardcodedOperatorFlags) {}  // vacío salvo SPEA2

private static final List<MetaAlgorithmDescriptor> ALGORITHMS = List.of(
    new MetaAlgorithmDescriptor(
        "NSGA-II", Family.EVOLUTIONARY, true, true,
        "NSGAIIMetaDouble.yaml", List.of()),
    new MetaAlgorithmDescriptor(
        "SPEA2", Family.EVOLUTIONARY, true, false, null, List.of(
            new OperatorFlagDescriptor("offspringPopulationSize", "int", false),
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

Para `"NSGA-II"`/`"AsyncNSGA-II"`, `operatorParameterSpaceFile` apunta al mismo fichero `ParameterSpace` (`NSGAIIMetaDouble.yaml`/`AsyncNSGAIIMetaDouble.yaml`) que ya usa internamente `buildNSGAII`/`buildAsyncNSGAII` — el manifiesto no duplica ese catálogo de operadores, solo indica dónde vive, con el mismo formato que `baseLevel.yamlParameterSpaceFile` ya usa (un cliente externo que sepa parsear ese formato no necesita código nuevo). Para `"SPEA2"`/`"SMPSO"` no hay tal fichero — sus operator-flags (si los hay) se listan explícitamente en `hardcodedOperatorFlags`, capturando lo que hoy solo está en `buildSPEA2`/`requireNoOperatorFlags`.

### Directorios de recursos reusables

Listado simple (`File.list()`, sin lógica adicional) de `src/main/resources/{parameterSpaces,baseLevelConfigurations,metaOptimizerConfigurations,defaultConfigurations}/` — permite a un cliente externo saber qué nombres son válidos para `yamlParameterSpaceFile`/`baseLevel`/`metaSearch` sin tener que listarlos a mano ni asumir convenciones de nombre.

### Esquema de `request.yaml`/`baseLevel`/`metaSearch` (vía reflexión sobre records)

Los catálogos de algoritmos cubren *qué nombres* son válidos, pero no *la forma* de los tres ficheros que un cliente externo tiene que generar (`request.yaml`, el fichero de `baseLevel`, el fichero de `metaSearch`) — esa forma vive hoy solo en `TrainingRequest`/`BaseLevelConfig`/`FlatMetaSearchConfig`/`TreeMetaSearchConfig` y en `cli-training-prototype.md`. Es exactamente el tipo de cambio que ya ha roto la integración de Evolver-Studio una vez (renombrado de campos, campos movidos de nivel).

A diferencia de los registros de algoritmos (`switch` sobre literales), estos cuatro tipos son **records Java** — su forma se puede obtener por reflexión (`Class.getRecordComponents()`, que da nombre y tipo de cada campo) en vez de mantenerla escrita a mano una segunda vez. Esto evita crear un sexto sitio que sincronizar: si un record gana/pierde/renombra un campo, el manifiesto lo refleja automáticamente sin tocar `DescribeMain`.

Lo único que la reflexión pura no da es opcionalidad y valores por defecto (`writeFrequency`/`statusFrequency` = 100, `frontPlotFrequency` ausente = sin gráfico, `metaPopulationSize` opcional en `FlatMetaSearchConfig`) — eso sí se mantiene como una tabla pequeña y explícita junto al record correspondiente (p. ej. una anotación ligera o un `Map<String, Object>` de defaults en el propio loader), mucho más acotado que describir cada campo entero a mano:

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

`DescribeMain` aplicaría esto a `TrainingRequest`, `BaseLevelConfig`, `FlatMetaSearchConfig` y `TreeMetaSearchConfig`, con la tabla de defaults ya conocida (`writeFrequency=100`, `statusFrequency=100`, `frontPlotFrequency` sin default, `metaPopulationSize` sin default en `FlatMetaSearchConfig`).

### `DescribeMain`

Sin argumentos, serializa un único objeto YAML a stdout:

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
      - {name: offspringPopulationSize, type: int, required: false}
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
  metaOptimizerConfigurations: [MetaParallelNSGAIIFlatConfiguration.yaml, ...]
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
    # ... resto de BaseLevelConfig
  metaSearchFlat:
    - {name: algorithm, javaType: String, required: true, defaultValue: null}
    - {name: metaMaxEvaluations, javaType: int, required: true, defaultValue: null}
    - {name: metaPopulationSize, javaType: Integer, required: false, defaultValue: null}
    - {name: numberOfCores, javaType: int, required: true, defaultValue: null}
    - {name: operatorFlags, javaType: List, required: false, defaultValue: null}
  metaSearchTree:
    - {name: algorithm, javaType: String, required: true, defaultValue: null}
    # ... resto de TreeMetaSearchConfig
```

Invocable igual que `TrainingRunnerMain`:

```
java -cp Evolver-*-jar-with-dependencies.jar org.uma.evolver.cli.training.DescribeMain
```

No es un run: no escribe `status.yaml`/`results.yaml`, no acepta ficheros de petición, termina inmediatamente tras escribir el YAML.

### Test de coherencia

Un test que, para cada `BaseAlgorithmDescriptor`/`MetaAlgorithmDescriptor` en `ALGORITHMS`, confirme que `resolve()`/`familyOf()` no lanza `JMetalException` para ese nombre (con valores mínimos válidos de los demás parámetros) — evita que la tabla de metadatos se desincronice del `switch`/lógica de resolución si ambos coexisten en vez de que uno se derive del otro. Complementa (no sustituye necesariamente) a `BaseAlgorithmRegistryCompletenessTest`/`TrainingRunnerMetaBuilderCompletenessTest`, que siguen cubriendo el caso "hay una clase Java de algoritmo que ningún registro conoce todavía".

## Fuera de alcance de esta propuesta

- Exponer el detalle interno de cada `yamlParameterSpaceFile`/`operatorParameterSpaceFile` (qué parámetros concretos tiene cada uno) — un cliente que ya sepa parsear el formato `ParameterSpace` (como Evolver-Studio) puede leer esos ficheros directamente; el manifiesto solo necesita decir *cuáles existen y a qué corresponden*.
- Cualquier cambio al contrato `request.yaml`/`TrainingRunnerMain` ya establecido en `cli-training-prototype.md` — este documento es puramente aditivo; la sección de esquemas del manifiesto es una proyección automática de esos mismos records, no una redefinición.
- Introspección para `cli.validation` u otros paquetes hermanos futuros, si llegan a existir.

## Consumo previsto desde Evolver-Studio

- `evolver_studio/catalogue.py` deja de mantener `BASE_ALGORITHMS`/`META_ALGORITHMS` como listas literales y pasa a poblarlas invocando `DescribeMain` (mismo patrón de subprocess que ya usa para `TrainingRunnerMain`), cacheando el resultado por sesión.
- `evolver_studio/request.py` puede usar la sección `schemas` del manifiesto para **validar** (no necesariamente generar) el YAML que construye antes de lanzarlo — detectando en el momento un campo renombrado/movido de nivel en vez de descubrirlo con un `ClassNotFoundException`/`JMetalException` al ejecutar contra el jar real.
- El test de drift-detection actual (`TestCatalogueMatchesEvolverCheckout`, que compara el catálogo hardcodeado contra los ficheros reales del checkout) se sustituye por un smoke test de que la introspección sigue funcionando — ya no hay un catálogo hardcodeado que comparar contra nada.
