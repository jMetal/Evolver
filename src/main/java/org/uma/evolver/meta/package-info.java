/**
 * Evolver's meta level: the automatic configuration of the algorithms in {@code
 * org.uma.evolver.algorithm}, formulated as a multi-objective optimization problem.
 *
 * <p>A meta-optimizer searches configurations of a base-level algorithm; each configuration is
 * evaluated by running the algorithm on a training set of problems, and its quality indicator
 * values are the objectives to minimize.
 *
 * <ul>
 *   <li>{@code problem}: the meta-optimization problems, for the flat [0,1]^n encoding and the
 *       derivation tree encoding
 *   <li>{@code algorithm} and {@code builder}: the meta-optimizers
 *   <li>{@code encoding}: the derivation tree encoding
 *   <li>{@code strategy}: evaluation budgets of the base-level runs
 *   <li>{@code trainingset}: training sets of benchmark problems
 *   <li>{@code output}: writing of the training results
 * </ul>
 *
 * <p>The meta level depends on the configurable core, never the other way round.
 */
package org.uma.evolver.meta;
