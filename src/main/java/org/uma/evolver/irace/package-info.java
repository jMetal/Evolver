/**
 * Integration with <a href="https://cran.r-project.org/package=irace">irace</a>, an alternative to
 * Evolver's meta-optimizers for tuning its algorithms.
 *
 * <ul>
 *   <li>{@link org.uma.evolver.irace.IraceParameterDescriptionGenerator}: writes irace's parameter
 *       file from a YAML parameter space
 *   <li>{@link org.uma.evolver.irace.AutoNSGAIIIraceHVEP} and {@link
 *       org.uma.evolver.irace.AutoNSGAIIIraceHV}: target runners that run NSGA-II with the
 *       configuration sampled by irace on a problem, and print the value irace minimizes (−HV + EP
 *       and −HV of the front found)
 * </ul>
 *
 * <p>The tutorial <a
 * href="https://evolver.readthedocs.io/en/latest/tutorials/tuning_with_irace.html">E14. Tuning with
 * irace</a> shows the whole process; the files it uses (scenario, instances and run script) are in
 * {@code src/main/resources/irace/}.
 */
package org.uma.evolver.irace;
