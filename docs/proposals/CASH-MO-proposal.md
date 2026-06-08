# CASH-MO: Combined Algorithm Selection and Hyperparameter Optimization for Multi-Objective Evolutionary Algorithms

**Proyecto:** Evolver (jMetal)  
**Fecha:** 2 de mayo de 2026  
**Target journals:** IEEE Transactions on Evolutionary Computation (TEVC), Swarm and Evolutionary Computation (SWEVO)

---

## 1. Motivación

En Machine Learning, los enfoques CASH (Combined Algorithm Selection and Hyperparameter optimization) como Auto-sklearn y Auto-WEKA han demostrado que seleccionar conjuntamente el algoritmo y sus hiperparámetros supera sistemáticamente a la configuración de un algoritmo fijo. Sin embargo, en optimización multi-objetivo este paradigma no se ha explorado de forma rigurosa.

El estado del arte en configuración automática de MOEAs (irace, ParamILS, SMAC aplicados a algoritmos individuales) asume que el usuario ha pre-seleccionado el algoritmo más adecuado para su problema, una decisión que en la práctica requiere expertise significativo y que puede ser subóptima.

**Gap identificado:** No existe un framework que aborde simultáneamente la selección del algoritmo multi-objetivo y la optimización de sus hiperparámetros como un problema unificado de meta-optimización multi-objetivo.

---

## 2. Pregunta de investigación principal

> **RQ:** ¿Puede un enfoque CASH multi-objetivo encontrar automáticamente la combinación (algoritmo, configuración) que domina a las configuraciones optimizadas por separado para cada algoritmo individual?

### Preguntas secundarias

- **RQ1:** ¿Cuál es la estructura del espacio de configuraciones unificado? ¿Existen regiones dominadas por un algoritmo específico según las características del problema?
- **RQ2:** ¿Cómo escala el coste computacional del CASH frente a optimizar N algoritmos independientemente?
- **RQ3:** ¿Las configuraciones encontradas por CASH generalizan mejor a problemas no vistos (out-of-sample) que las configuraciones per-algorithm?

---

## 3. Contribuciones esperadas

1. **Formalización del problema CASH-MO** como un problema de meta-optimización multi-objetivo de segundo nivel, donde las variables de decisión incluyen la selección del algoritmo y sus hiperparámetros condicionales.

2. **Representación unificada** del espacio de configuraciones mediante parámetros condicionales jerárquicos, permitiendo tratar selección de algoritmo y configuración en un único espacio continuo [0,1]^n.

3. **Evidencia empírica** de que CASH-MO domina o iguala al per-algorithm tuning con un presupuesto computacional comparable, evaluado sobre benchmarks estándar.

4. **Análisis de dominancia algorítmica:** mapeo de qué algoritmos emergen como óptimos según las características del problema (número de objetivos, separabilidad, landscape).

5. **Framework open-source** implementado como extensión de Evolver para reproducibilidad completa.

---

## 4. Diseño técnico

### 4.1 Espacio de configuración jerárquico unificado

El espacio se define como un YAML con un parámetro categórico raíz `algorithm` que condiciona todos los demás:

```yaml
algorithm:
  type: categorical
  values:
    NSGAII:
      conditionalParameters:
        populationSize:
          type: integer
          range: [10, 200]
        offspringPopulationSize:
          type: integer
          range: [1, 200]
        variation:
          type: categorical
          values:
            crossoverAndMutationVariation:
              conditionalParameters:
                crossover: ...
                mutation: ...
    MOEAD:
      conditionalParameters:
        populationSize: ...
        neighborhoodSize: ...
        aggregationFunction: ...
    SMSEMOA:
      conditionalParameters: ...
    MOPSO:
      conditionalParameters: ...
    RVEA:
      conditionalParameters: ...
```

La infraestructura de Evolver (`ConditionalParameter`, `getActiveParameterIndices`, `YAMLParameterSpace`) ya soporta este patrón sin modificaciones al parser.

### 4.2 Extensión arquitectural

```
MetaOptimizador (NSGA-II)
  │
  ▼
CASHMetaOptimizationProblem
  ├─ algorithmPool: Map<String, BaseLevelAlgorithm>
  ├─ unifiedParameterSpace: ParameterSpace (YAML unificado)
  │
  ▼ evaluate(DoubleSolution):
  1. Decode variables → extraer algorithmName
  2. Filtrar parámetros activos para ese algoritmo
  3. Instanciar algoritmo seleccionado
  4. Ejecutar sobre training set
  5. Calcular indicadores de calidad
```

### 4.3 Manejo de dimensionalidad variable

El espacio unificado tiene ~80-100 variables (suma de parámetros de todos los algoritmos), pero solo ~15-25 están activas para una configuración concreta. Las variables inactivas se gestionan mediante `getActiveParameterIndices()`, ya implementado en Evolver.

**Mitigación de alta dimensionalidad:** Usar los parameter spaces reducidos existentes (`NSGAIIDoubleReduced.yaml`, etc.) como variante del experimento.

---

## 5. Diseño experimental

### 5.1 Setup

| Aspecto | Configuración |
|---------|--------------|
| Portfolio de algoritmos | NSGA-II, MOEA/D, SMS-EMOA, RVEA, MOPSO |
| Encoding | Double |
| Training sets | DTLZ (3 obj.), WFG (2 obj.), RE (3 obj.) |
| Test sets | Problemas del mismo benchmark no usados en training |
| Indicadores (objetivos del meta-nivel) | Hypervolume normalizado, Epsilon aditivo |
| Meta-optimizador | NSGA-II, 10,000 evaluaciones |
| Runs independientes por configuración | 3 |
| Repeticiones del meta-optimizador | 25 |

### 5.2 Baselines

| Baseline | Descripción |
|----------|-------------|
| **Per-algorithm tuning** | Evolver actual para cada algoritmo por separado; se reporta el mejor resultado individual |
| **Default configurations** | Configuraciones por defecto (`defaultConfigurations/`) |
| **Random Search CASH** | Selección aleatoria de (algoritmo, configuración) en el espacio unificado |
| **Oracle (upper bound)** | Para cada problema de test, la mejor configuración de cualquier algoritmo |

### 5.3 Métricas de evaluación

- **Rendimiento:** Hypervolume y Epsilon en problemas de test.
- **Generalización:** Delta de rendimiento entre training y test (detección de overfitting).
- **Eficiencia:** Evaluaciones del meta-nivel necesarias para alcanzar 95% del rendimiento del per-algorithm tuning.
- **Dominancia algorítmica:** Proporción de soluciones Pareto-óptimas del meta-nivel que corresponden a cada algoritmo.

### 5.4 Análisis estadístico

- Test de Wilcoxon con corrección de Holm para comparaciones pareadas.
- Critical Difference diagrams (Demšar) para rankings globales.
- Heatmaps de dominancia algorítmica por familia de problemas.
- An��lisis de contribución por algoritmo al frente Pareto del meta-nivel.

---

## 6. Estructura sugerida del artículo

1. **Introduction** — CASH en ML, gap en MOEAs, contribución
2. **Background and Related Work** — Algorithm Configuration, Algorithm Selection, CASH, AutoML for optimization
3. **CASH-MO: Problem Formulation** — Definición formal, representación, propiedades del espacio
4. **Implementation** — Arquitectura en Evolver, espacio unificado, evaluación
5. **Experimental Setup** — Algoritmos, benchmarks, indicadores, protocolo estadístico
6. **Results and Analysis**
   - 6.1 CASH-MO vs. Per-Algorithm Tuning
   - 6.2 Generalization to Unseen Problems
   - 6.3 Algorithm Dominance Maps
   - 6.4 Computational Cost Analysis
   - 6.5 Sensitivity to Meta-optimizer Budget
7. **Discussion and Threats to Validity**
8. **Conclusions and Future Work**

---

## 7. Viabilidad de implementación

| Componente | Estado en Evolver | Esfuerzo |
|-----------|-------------------|----------|
| YAML unificado | Crear combinando YAMLs existentes | Bajo |
| `CASHMetaOptimizationProblem` | Extender `MetaOptimizationProblem` | Medio |
| Pool de algoritmos | `BaseLevelAlgorithm` ya es interfaz común | Bajo |
| Active parameter handling | Funciona con condicionales existentes | Ninguno |
| Training/test split | Infraestructura de `TrainingSet` existente | Bajo |
| Evaluación experimental | Infraestructura de examples disponible | Medio |
| Análisis estadístico | jMetal-lab proporciona tests y diagramas | Bajo |

**Riesgo principal:** La alta dimensionalidad del espacio unificado (~100 variables) puede requerir un presupuesto de meta-evaluaciones mayor que el per-algorithm tuning. Esto se mitiga con:
- Parameter spaces reducidos como variante experimental
- Warm-starting con configuraciones default
- Análisis del landscape del espacio CASH (posible extensión a línea 6)

---

## 8. Trabajos relacionados clave

- Thornton et al. (2013) — Auto-WEKA: CASH original
- Feurer et al. (2015) — Auto-sklearn: CASH con meta-learning
- López-Ibáñez et al. (2016) — irace: configuración iterativa
- Bezerra et al. (2016, 2020) — Configuración automática de MOEAs (per-algorithm)
- Huang et al. (2019) — Algorithm selection para MOEAs
- de Nobel et al. (2021) — IOHprofiler para algorithm configuration landscapes

---

## 9. Extensiones futuras

- **Meta-learning:** Usar features del problema para predecir la mejor región del espacio CASH sin evaluar.
- **Surrogate-assisted CASH:** Modelos surrogados para reducir el coste del meta-nivel.
- **Dynamic CASH:** Seleccionar y reconfigurar el algoritmo durante la ejecución.
- **Portfolio construction:** Extraer un portfolio mínimo del frente Pareto del meta-nivel.

---

## 10. Timeline estimado

| Fase | Actividad | Duración |
|------|-----------|----------|
| 1 | Implementación del YAML unificado y CASHMetaOptimizationProblem | 4 semanas |
| 2 | Validación con experimentos piloto (DTLZ, 2 algoritmos) | 2 semanas |
| 3 | Experimentos completos (5 algoritmos, 3 training sets) | 6 semanas |
| 4 | Análisis estadístico y visualizaciones | 2 semanas |
| 5 | Redacción del artículo | 4 semanas |
| 6 | Revisión interna y envío | 2 semanas |
