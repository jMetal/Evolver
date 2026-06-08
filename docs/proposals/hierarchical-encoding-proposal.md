# Codificación jerárquica para meta-optimización de MOEAs: Representación gramatical con operadores de Genetic Programming

**Proyecto:** Evolver (jMetal)  
**Fecha:** 4 de mayo de 2026  
**Target journals:** IEEE Transactions on Evolutionary Computation (TEVC), Swarm and Evolutionary Computation (SWEVO)  
**Relación:** Complementaria a la propuesta CASH-MO (ver `CASH-MO-proposal.md`)

---

## 1. Motivación

### El problema de la codificación plana

En Evolver, los espacios de parámetros de los algoritmos multi-objetivo son intrínsecamente jerárquicos: la elección de un operador de cruce (e.g., SBX vs BLX-alpha) determina qué sub-parámetros son relevantes (distribution index vs alpha). Sin embargo, la codificación actual aplana toda la jerarquía en un vector homogéneo `[0,1]^n`, donde:

- **Todas las variables ocupan una posición**, independientemente de si están activas o no.
- **Los operadores de variación estándar** (SBX crossover, polynomial mutation) operan sobre el vector completo sin distinguir entre variables activas e inactivas.
- **Las variables inactivas** (aquellas cuyos parámetros padre categóricos no las activan) son mutadas y cruzadas sin efecto sobre el fitness.

### Cuantificación del problema

Para un espacio NSGAIIDouble (32 variables, ~18 activas): **~40% del esfuerzo de variación se desperdicia** en variables irrelevantes.

Para un espacio CASH unificado (5 algoritmos, ~100 variables, ~20 activas): **~80% del esfuerzo es ruido**.

### Consecuencias sobre la búsqueda

1. **Inflación dimensional:** El meta-optimizador percibe un espacio de dimensión n cuando la dimensionalidad real es k < n.
2. **Herencia de ruido:** Cuando un categórico muta y activa una nueva rama, los valores heredados en esas posiciones son aleatorios (no fueron optimizados previamente).
3. **Crossover semánticamente incoherente:** SBX entre dos soluciones con diferente estructura mezcla parámetros incompatibles (e.g., `sbxDistIndex` con `blxAlpha`).
4. **Epistasis artificial:** El fitness de una variable depende completamente de otra variable categórica que la activa/desactiva, creando paisajes rugosos innecesarios.

### Observación clave

El espacio de parámetros definido por un fichero YAML **es una gramática**, y una configuración concreta **es un árbol de derivación** de esa gramática. Este isomorfismo nos permite aplicar directamente los operadores de **Grammar-Guided Genetic Programming (GGGP)** y **Strongly Typed GP (STGP)**, que llevan más de 30 años probados en la literatura para manipular exactamente este tipo de estructuras.

---

## 2. Preguntas de investigación

> **RQ principal:** ¿Mejora la convergencia del meta-optimizador una representación gramatical con operadores de GP frente a la codificación plana estándar con operadores de GA?

- **RQ1:** ¿Cuánto esfuerzo de búsqueda se desperdicia con la codificación plana frente a la representación arbórea?
- **RQ2:** ¿Qué impacto tiene el cruce tipado (subtree crossover homólogo) en la calidad de las configuraciones encontradas respecto al SBX estándar sobre el vector plano?
- **RQ3:** ¿Cómo afecta la proporción activas/totales (ratio k/n) al beneficio relativo de la representación gramatical?

---

## 3. Estado del arte

| Trabajo | Enfoque | Limitación |
|---------|---------|------------|
| SMAC (Hutter et al., 2011) | Surrogate sobre espacio condicional con random forests | No usa EA; no escala a many-objective |
| irace (López-Ibáñez et al., 2016) | Racing sobre espacio condicional | Single-objective; no explota estructura en operadores |
| BOHB (Falkner et al., 2018) | Bayesian optimization + bandit en espacio jerárquico | No multi-objetivo; costoso para dimensionalidades altas |
| Bezerra et al. (2016) | Configuración de MOEAs con irace | Codificación plana sin operadores conscientes |
| Auto-WEKA/sklearn | CASH con SMAC/TPE | No usa operadores evolutivos |
| Montana (1995) | Strongly Typed GP | Operadores tipados para árboles, no aplicado a algorithm configuration |
| McKay et al. (2010) | Grammar-Guided GP survey | Operadores sobre gramáticas BNF, no aplicado a meta-optimización |

**Gap:** Aunque los operadores de GGGP/STGP son canónicos para manipular árboles de derivación tipados, no se han aplicado al problema de meta-optimización multi-objetivo de algoritmos evolutivos.

---

## 4. Formalización: El espacio de parámetros como gramática

### 4.1 Del YAML a la gramática BNF

Un fichero YAML de espacio de parámetros (e.g., `NSGAIIDouble.yaml`) se traduce directamente a una gramática BNF:

**YAML (fragmento):**
```yaml
crossover:
  type: categorical
  globalSubParameters:
    crossoverProbability:
      type: double
      range: [0.0, 1.0]
    crossoverRepairStrategy:
      type: categorical
      values: [random, round, bounds]
  values:
    SBX:
      conditionalParameters:
        sbxDistributionIndex:
          type: double
          range: [5.0, 400.0]
    blxAlpha:
      conditionalParameters:
        blxAlphaCrossoverAlpha:
          type: double
          range: [0.0, 1.0]
    wholeArithmetic: {}
```

**Gramática BNF equivalente:**
```
<crossover>         ::= <crossoverType> <crossoverProbability> <crossoverRepairStrategy>
<crossoverType>     ::= SBX <sbxDistIndex> | blxAlpha <blxAlpha> | wholeArithmetic
<crossoverProbability> ::= DOUBLE[0.0, 1.0]
<crossoverRepairStrategy> ::= random | round | bounds
<sbxDistIndex>      ::= DOUBLE[5.0, 400.0]
<blxAlpha>          ::= DOUBLE[0.0, 1.0]
```

### 4.2 La configuración como árbol de derivación

Una configuración concreta de un algoritmo es un árbol de derivación completo de la gramática:

```
<NSGAIIConfig>
├── <algorithmResult> → population
├── <createInitialSolutions> → default
├── <offspringPopulationSize> → 100
├── <variation> → crossoverAndMutationVariation
│   ├── <crossover>
│   │   ├── <crossoverType> → SBX
│   │   │   └── <sbxDistIndex> → 20.0
│   │   ├── <crossoverProbability> → 0.9
│   │   └── <crossoverRepairStrategy> → random
│   └── <mutation>
│       ├── <mutationType> → polynomial
│       │   └── <polyDistIndex> → 20.0
│       ├── <mutProbFactor> → 1.0
│       └── <mutRepairStrategy> → bounds
└── <selection> → tournament
    └── <tournamentSize> → 5
```

### 4.3 Tipos de nodos

| Tipo de nodo | Rol en gramática | Ejemplo |
|---|---|---|
| No-terminal con alternativas | Producción con opciones | `<crossoverType> ::= SBX ... \| blxAlpha ...` |
| Terminal numérico | Terminal con rango continuo | `<sbxDistIndex> → 20.0` |
| Terminal categórico sin hijos | Producción sin derivación | `<crossoverRepairStrategy> → random` |

---

## 5. Operadores: GP estándar aplicado a árboles de derivación

### 5.1 Cruce: Subtree crossover tipado (STGP)

Se aplica directamente el cruce de subárboles de Strongly Typed GP (Montana, 1995):

1. Seleccionar un nodo aleatorio en parent1 con no-terminal `<N>`.
2. En parent2, seleccionar un nodo aleatorio **del mismo no-terminal `<N>`**.
3. Intercambiar los subárboles enraizados en esos nodos.
4. Ambos hijos son configuraciones válidas por construcción (la gramática lo garantiza).

**Ejemplo:**

```
Parent1:                                Parent2:
<variation>                             <variation>
├── <crossover>                         ├── <crossover>
│   ├── <crossoverType> → SBX          │   ├── <crossoverType> → blxAlpha
│   │   └── <sbxDistIndex> → 20.0      │   │   └── <blxAlpha> → 0.5
│   ├── <crossProb> → 0.9              │   ├── <crossProb> → 0.7
│   └── <repairStrat> → random         │   └── <repairStrat> → bounds
└── <mutation>                          └── <mutation>
    ├── <mutType> → polynomial              ├── <mutType> → uniform
    │   └── <polyDist> → 20.0              │   └── <uniformPert> → 0.3
    ├── <mutProbFactor> → 1.0              ├── <mutProbFactor> → 0.5
    └── <mutRepair> → bounds               └── <mutRepair> → random
```

**Nodo seleccionado en ambos padres: `<crossover>` (mismo no-terminal)**

```
Child1:                                 Child2:
<variation>                             <variation>
├── <crossover>  ← de Parent2          ├── <crossover>  ← de Parent1
│   ├── <crossoverType> → blxAlpha     │   ├── <crossoverType> → SBX
│   │   └── <blxAlpha> → 0.5           │   │   └── <sbxDistIndex> → 20.0
│   ├── <crossProb> → 0.7              │   ├── <crossProb> → 0.9
│   └── <repairStrat> → bounds         │   └── <repairStrat> → random
└── <mutation>  ← de Parent1           └── <mutation>  ← de Parent2
    ├── <mutType> → polynomial              ├── <mutType> → uniform
    │   └── <polyDist> → 20.0              │   └── <uniformPert> → 0.3
    ├── <mutProbFactor> → 1.0              ├── <mutProbFactor> → 0.5
    └── <mutRepair> → bounds               └── <mutRepair> → random
```

**Propiedades:**
- Nunca mezcla parámetros de operadores incompatibles (SBX con BLX).
- Preserva subárboles completos y coherentes.
- No requiere diseño de operadores nuevos — es el operador estándar de STGP.
- La restricción de tipos (mismo no-terminal) se obtiene directamente de la gramática/YAML.

### 5.2 Mutación: Point mutation + Subtree mutation (GP estándar)

Se aplican los dos operadores de mutación canónicos de GP (Koza, 1992; Poli et al., 2008):

**Paso 1:** Seleccionar un nodo aleatorio del árbol.

**Paso 2:** Según el tipo de nodo:

- **Si es terminal numérico** → **Point mutation:** Perturbar el valor con polynomial mutation (o mutación gaussiana) dentro de su rango.
  ```
  Antes:   <sbxDistIndex> → 20.0
  Después: <sbxDistIndex> → 27.3
  ```

- **Si es no-terminal con alternativas** → **Subtree mutation:** Seleccionar una producción alternativa de la gramática y generar aleatoriamente el nuevo subárbol derivado.
  ```
  Antes:   <crossoverType> → SBX
           └── <sbxDistIndex> → 20.0
  
  Después: <crossoverType> → blxAlpha        ← nueva producción
           └── <blxAlpha> → 0.42             ← subárbol regenerado
  ```

- **Si es terminal categórico sin hijos** → **Point mutation discreta:** Seleccionar un valor alternativo.
  ```
  Antes:   <repairStrategy> → random
  Después: <repairStrategy> → bounds
  ```

**Nota sobre sub-parámetros globales:** Cuando se muta un no-terminal con alternativas, solo se regenera la rama condicional. Los sub-parámetros globales (que son independientes de la producción elegida) se conservan intactos. En el ejemplo anterior, si `<crossover>` muta su tipo de SBX a blxAlpha, los sub-parámetros `<crossoverProbability>` y `<crossoverRepairStrategy>` no se modifican.

### 5.3 Probabilidades estándar

Siguiendo las recomendaciones canónicas de GP (Poli et al., 2008, *A Field Guide to GP*):

| Evento | Probabilidad |
|--------|-------------|
| Crossover (subtree crossover tipado) | 80-90% |
| Mutación | 10-20% |

Cuando se aplica mutación, se selecciona **un nodo** del árbol (selección uniforme entre todos los nodos activos) y se muta según su tipo. Un único evento de mutación por individuo.

---

## 6. Diseño experimental

### 6.1 Setup

| Aspecto | Configuración |
|---------|--------------|
| Codificaciones comparadas | (1) Flat `[0,1]^n` + SBX/PM (baseline actual), (2) Gramática + operadores GP |
| Algoritmos base a configurar | NSGA-II (todo categórico en raíz), MOEA/D (mezcla numéricos y categóricos) |
| Meta-optimizador | NSGA-II adaptado para árboles de derivación |
| Training sets | DTLZ (3 obj.), WFG (2 obj.) |
| Indicadores meta-nivel | Hypervolume normalizado, Epsilon aditivo |
| Presupuesto meta-nivel | 5,000 y 10,000 evaluaciones |
| Repeticiones | 30 ejecuciones independientes |

### 6.2 Métricas

- **Convergencia:** Hypervolume del frente Pareto del meta-nivel vs. número de evaluaciones.
- **Eficiencia:** Evaluaciones necesarias para alcanzar 90%/95%/99% del hypervolume final.
- **Calidad final:** Indicadores sobre problemas de test (generalización).
- **Análisis de desperdicio:** Proporción de operaciones de variación que producen cambio fenotípico (100% en representación gramatical por construcción, <100% en flat).
- **Diversidad estructural:** Distribución de operadores/componentes en el frente Pareto final.

### 6.3 Experimentos adicionales

**Exp. 1 — Sensibilidad al ratio k/n:**
- Comparar rendimiento con parameter spaces de diferente profundidad (Reduced vs Full vs CASH).
- Hipótesis: la ventaja de la representación gramatical crece con n-k.

**Exp. 2 — Granularidad del punto de cruce:**
- Evaluar el efecto de restringir los nodos seleccionables para crossover (solo niveles superiores vs todos los niveles).
- Analizar si intercambiar subárboles grandes (alto en el árbol) vs pequeños (profundos) afecta convergencia.

**Exp. 3 — Warm-starting:**
- Inicializar la población del meta-nivel con configuraciones default (disponibles en `defaultConfigurations/`).
- Evaluar si la representación arbórea permite explotar mejor el warm-start que la plana.

### 6.4 Análisis estadístico

- Wilcoxon rank-sum test con corrección de Holm.
- Critical Difference diagrams (Demšar) para ranking global.
- Curvas de convergencia con bandas de confianza (bootstrapped 95% CI).

---

## 7. Aspectos técnicos de implementación

### 7.1 Integración con el framework jMetal

La representación gramatical requiere:
- Nueva clase `DerivationTreeSolution` que implementa `Solution<TreeNode>`.
- Operador `SubtreeCrossover<DerivationTreeSolution>` (crossover tipado estándar de STGP).
- Operador `TreeMutation<DerivationTreeSolution>` (point + subtree mutation de GP).
- Adaptación de `MetaOptimizationProblem` para generar/evaluar `DerivationTreeSolution`.

### 7.2 Construcción del árbol desde el YAML

El `YAMLParameterSpace` ya parsea la estructura jerárquica completa. La conversión a gramática BNF y la generación de árboles de derivación aleatorios es directa:

```
buildDerivationTree(parameterSpace):
  for each topLevelParameter in parameterSpace.topLevelParameters():
    node = deriveRandom(topLevelParameter)
    tree.addRoot(node)

deriveRandom(parameter):
  node = new TreeNode(parameter)
  if parameter is NUMERIC:
    node.value = randomInRange(parameter.min, parameter.max)
  if parameter is CATEGORICAL:
    node.value = randomChoice(parameter.validValues)
    // Derivar sub-parámetros globales
    for each globalSub in parameter.globalSubParameters():
      node.addChild(deriveRandom(globalSub))
    // Derivar solo la rama condicional correspondiente al valor elegido
    for each conditionalParam matching node.value:
      node.addChild(deriveRandom(conditionalParam))
  return node
```

### 7.3 Compatibilidad con la evaluación existente

La `DerivationTreeSolution` se convierte a la interfaz del nivel base sin cambios:

```
DerivationTreeSolution
    ↓ toParameterString()  (recorre el árbol, genera solo los nodos activos)
String[] args = ["--crossover", "SBX", "--sbxDistributionIndex", "20.0", ...]
    ↓
baseAlgorithm.parse(args).build().run()
```

### 7.4 Selección de nodos para crossover

Para el subtree crossover tipado, se necesita un índice de nodos por no-terminal:

```
// Pre-computado una vez por solución
Map<String, List<TreeNode>> nodesByNonTerminal = indexByGrammarSymbol(tree)

// En crossover:
// 1. Seleccionar nodo n1 en parent1
TreeNode n1 = randomNode(parent1)
String symbol = n1.grammarSymbol()

// 2. Seleccionar nodo n2 en parent2 con el mismo símbolo
List<TreeNode> candidates = parent2.nodesByNonTerminal.get(symbol)
TreeNode n2 = randomChoice(candidates)

// 3. Intercambiar subárboles
swap(n1.subtree, n2.subtree)
```

---

## 8. Contribuciones esperadas

1. **Formalización:** Primera formulación del espacio de configuración de MOEAs como gramática BNF, estableciendo el isomorfismo YAML ↔ gramática ↔ árbol de derivación.

2. **Aplicación de GGGP/STGP a meta-optimización:** Demostrar que los operadores canónicos de GP (subtree crossover tipado, point/subtree mutation) superan a los operadores de GA (SBX, polynomial mutation) sobre la codificación plana en este dominio.

3. **Estudio empírico:** Evidencia experimental de la mejora en convergencia y calidad, con análisis de cuándo el beneficio es significativo (en función del ratio k/n).

4. **Framework open-source** implementado como extensión de Evolver para reproducibilidad.

---

## 9. Estructura del artículo

1. **Introduction** — Meta-optimización de MOEAs, limitaciones de la codificación plana
2. **Background** — Espacios condicionales, GGGP, STGP, configuración de algoritmos
3. **Formalization: Configuration Spaces as Grammars**
   - 3.1 From YAML to BNF
   - 3.2 Configurations as Derivation Trees
   - 3.3 Relationship to GGGP/STGP
4. **Approach: GP Operators for Meta-Optimization**
   - 4.1 Typed Subtree Crossover
   - 4.2 Point and Subtree Mutation
5. **Experimental Setup**
6. **Results**
   - 6.1 Convergence Comparison
   - 6.2 Final Quality and Generalization
   - 6.3 Sensitivity to Hierarchy Depth (k/n ratio)
7. **Discussion**
8. **Conclusions and Future Work**

---

## 10. Viabilidad de implementación en Evolver

| Componente | Esfuerzo | Dependencias |
|-----------|----------|--------------|
| Conversión YAML → gramática BNF (formalización) | Bajo | Documentación |
| `DerivationTreeSolution` class | Medio | Nueva representación |
| Subtree crossover tipado | Bajo-Medio | DerivationTreeSolution |
| Point + Subtree mutation | Bajo-Medio | DerivationTreeSolution |
| Adaptación MetaOptimizationProblem | Medio | DerivationTreeSolution |
| Generación de árboles desde YAMLParameterSpace | Bajo | Infraestructura existente |
| Experiments + análisis | Alto | Todo lo anterior |

---

## 11. Relación con la propuesta CASH-MO

Esta propuesta es **sinérgica** con CASH-MO:

- En CASH-MO, el ratio k/n es extremo (~20/100), maximizando el beneficio de la representación gramatical.
- El espacio CASH unificado es naturalmente una gramática con `<algorithm>` como símbolo raíz y las configuraciones de cada algoritmo como producciones alternativas.
- Un artículo puede presentar CASH-MO con codificación plana; un segundo artículo demuestra la mejora con representación gramatical y operadores GP.

**Posible combinación en un solo artículo (TEVC):** "Grammar-Based Representation for Multi-Objective Automatic Algorithm Configuration" — contribución doble (formalización gramatical + evidencia empírica).

---

## 12. Referencias fundamentales

1. **Koza, J. R. (1992).** *Genetic Programming: On the Programming of Computers by Means of Natural Selection.* MIT Press. — Subtree crossover y subtree mutation.
2. **Montana, D. J. (1995).** "Strongly Typed Genetic Programming." *Evolutionary Computation*, 3(2), 199–230. — Crossover tipado: solo intercambiar subárboles del mismo tipo.
3. **Whigham, P. A. (1995).** "Grammatically-based Genetic Programming." In *Proceedings of the Workshop on GP*. — CFG-GP: gramáticas como restricción estructural.
4. **McKay, R. I., Hoai, N. X., Whigham, P. A., Shan, Y., & O'Neill, M. (2010).** "Grammar-based genetic programming: A survey." *Genetic Programming and Evolvable Machines*, 11(3–4), 365–396. — Survey de operadores GGGP.
5. **Poli, R., Langdon, W. B., & McPhee, N. F. (2008).** *A Field Guide to Genetic Programming.* — Referencia de libro de texto para operadores GP estándar.
6. **Hutter, F., Hoos, H. H., & Leyton-Brown, K. (2011).** "Sequential Model-Based Optimization for General Algorithm Configuration." In *LION 5*. — SMAC, espacios condicionales.
7. **López-Ibáñez, M., Dubois-Lacoste, J., Cáceres, L. P., Birattari, M., & Stützle, T. (2016).** "The irace package: Iterated racing for automatic algorithm configuration." *Operations Research Perspectives*, 3, 43–58.

---

## 13. Timeline estimado

| Fase | Actividad | Duración |
|------|-----------|----------|
| 1 | Formalización YAML → BNF + DerivationTreeSolution | 3 semanas |
| 2 | Implementar subtree crossover tipado + mutation | 3 semanas |
| 3 | Adaptar MetaOptimizationProblem | 2 semanas |
| 4 | Experimentos piloto (NSGA-II, DTLZ) | 2 semanas |
| 5 | Experimentos completos (2 codificaciones × 2 algoritmos × benchmarks) | 4 semanas |
| 6 | Análisis estadístico y visualizaciones | 2 semanas |
| 7 | Redacción | 4 semanas |
