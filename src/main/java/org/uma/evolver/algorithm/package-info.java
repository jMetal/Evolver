/**
 * Evolver's configurable multi-objective algorithms: NSGA-II, NSGA-III, MOEA/D, SMS-EMOA, MOPSO,
 * RDEMOEA, RVEA, AGE-MOEA, SSMOEA and PAES, most of them for Double, Binary and Permutation
 * encodings.
 *
 * <p>Each algorithm is a {@link org.uma.evolver.algorithm.BaseLevelAlgorithm}: its components
 * (initialization, variation, selection, replacement, archive, …) are chosen from a {@link
 * org.uma.evolver.parameter.ParameterSpace}, usually loaded from a YAML file, and fixed by parsing
 * a configuration such as {@code --crossover SBX --crossoverProbability 0.9 …}; {@code build()}
 * then returns a ready-to-run jMetal algorithm.
 *
 * <p>Together with {@code org.uma.evolver.parameter} and {@code org.uma.evolver.util}, this package
 * forms Evolver's configurable core. The core can be used on its own, as an alternative to jMetal
 * for configuring and running algorithms, and never depends on Evolver's meta level (the {@code
 * meta} package), which uses these algorithms as the base level it tunes.
 */
package org.uma.evolver.algorithm;
