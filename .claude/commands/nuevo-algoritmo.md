Añade un nuevo algoritmo configurable al proyecto Evolver siguiendo el patrón establecido.

El algoritmo a añadir es: $ARGUMENTS

## Pasos

### 1. Determinar el patrón de referencia

Antes de crear ningún fichero, lee el algoritmo más similar ya implementado:
- `src/main/java/org/uma/evolver/algorithm/agemoea/BaseAGEMOEA.java`
- `src/main/java/org/uma/evolver/algorithm/agemoea/DoubleAGEMOEA.java`

Identifica qué hace específico ese algoritmo en `createReplacement()` y `setNonConfigurableParameters()`, para replicarlo con la lógica del nuevo algoritmo.

### 2. Crear las clases Java

**Paquete:** `org.uma.evolver.algorithm.<nombre_en_minúsculas>`

Crea dos ficheros:

**`Base<Nombre>.java`** — clase abstracta que implementa `BaseLevelAlgorithm<S>`:
- Dos constructores: `(int populationSize, ParameterSpace)` y `(Problem<S>, int populationSize, int maximumNumberOfEvaluations, ParameterSpace)`
- Implementa `parameterSpace()`, `build()` y los métodos protegidos: `createInitialSolutions()`, `createVariation()`, `createSelection()`, `createEvaluation()`, `createReplacement()`, `createTermination()`
- Declara `setNonConfigurableParameters()` como abstracto

**`Double<Nombre>.java`** — clase concreta para `DoubleSolution`:
- Extiende `Base<Nombre><DoubleSolution>`
- Implementa `createInstance(Problem<DoubleSolution>, int)` retornando `new Double<Nombre>(..., parameterSpace.createInstance())`
- Implementa `setNonConfigurableParameters()` inyectando `numberOfProblemVariables` en la mutación

### 3. Crear el YAML de parameter space

**Ruta:** `src/main/resources/parameterSpaces/<Nombre>Double.yaml`

Copia la estructura de `AGEMOEADouble.yaml` como punto de partida. Incluye siempre:
- `algorithmResult` (population / externalArchive con sub-parámetros)
- `createInitialSolutions`
- `offspringPopulationSize`
- `variation` (con `crossover` y `mutation` como parámetros condicionales)
- `selection`

Añade los parámetros específicos del nuevo algoritmo al inicio del fichero.

### 4. Crear la configuración por defecto

**Ruta:** `src/main/resources/defaultConfigurations/<Nombre>DoubleDefault.txt`

Una sola línea con la configuración estándar:
```
--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2
```
Añade al principio los parámetros específicos del algoritmo con sus valores por defecto recomendados.

### 5. Crear el test unitario

**Ruta:** `src/test/java/org/uma/evolver/algorithm/<nombre_minúsculas>/<Nombre>DoubleTest.java`

Sigue exactamente §13 de `JAVA_CODING_GUIDELINES.md`. El test debe tener:

- `@DisplayName("Unit tests for class Double<Nombre>")`
- `@BeforeEach void setUp()` que instancie con `ZDT1`, populationSize=100, maxEvals=20000
- `@Nested` "When the class constructor is called":
  - Verifica el número total de parámetros aplanados
  - Verifica el número de parámetros top-level
- `@Nested` "When calling the parse() method":
  - Verifica el parsing de la configuración por defecto
  - Verifica los parámetros específicos del algoritmo
  - Si hay variantes, prueba cada una

Para saber el número correcto de parámetros, ejecuta el test con un valor provisional y corrígelo con el real.

### 6. Crear el test de integración

**Ruta:** `src/test/java/org/uma/evolver/algorithm/<nombre_minúsculas>/<Nombre>DoubleIT.java`

- `@DisplayName("Integration tests for class Double<Nombre>")`
- Al menos un `@Test` con `@Tag("integration")` que ejecute el algoritmo sobre ZDT1 con la configuración por defecto
- Verifica que el hypervolume supera un mínimo razonable (usa 0.62 como referencia para ZDT1)
- Extrae la ejecución a un método privado si hay múltiples variantes

### 7. Crear el ejemplo runner

**Ruta:** `src/main/java/org/uma/evolver/example/configuration/<Nombre>ForZDT1Example.java`

Sigue la estructura de `AGEMOEAForZDT1Example.java`:
1. Declara `yamlParameterSpaceFile` y `referenceFrontFileName`
2. Define `parameters` como text block (triple comillas)
3. Crea instancia con `ZDT1`, populationSize=100, maxEvals=20000
4. `.parse(parameters)` → `.build()` → `.run()`
5. Guarda resultados en `VAR.csv` y `FUN.csv`
6. Imprime indicadores de calidad con `QualityIndicatorUtils.printQualityIndicators`

### 8. Verificar

Ejecuta `mvn test -Dtest=<Nombre>DoubleTest` y corrige cualquier error antes de declarar el trabajo terminado.
