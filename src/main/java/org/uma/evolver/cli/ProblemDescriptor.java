package org.uma.evolver.cli;

import java.util.List;

/**
 * What {@link ProblemRegistry} knows about a registered problem, for {@code DescribeMain}: enough
 * for an external tool to filter the problems by encoding, to show their dimensions and to build the
 * {@code args} of a {@link ProblemSpec}.
 *
 * @param name the name a request uses ({@code "DTLZ2"})
 * @param family the family it belongs to ({@code "DTLZ"}); the problems of a family share the
 *     meaning of their arguments
 * @param encoding the encoding of its solutions: {@code "Double"}, {@code "Binary"} or {@code
 *     "Permutation"}
 * @param numberOfObjectives the number of objectives of the problem built with no arguments, or
 *     {@code null} when it cannot be built (a problem that reads files that are not there)
 * @param numberOfVariables the number of variables of that problem, or {@code null}
 * @param arguments the arguments of its constructor with arguments, in order: a request gives all
 *     of them, or none (empty for a problem with no such constructor)
 */
public record ProblemDescriptor(
    String name,
    String family,
    String encoding,
    Integer numberOfObjectives,
    Integer numberOfVariables,
    List<Argument> arguments) {

  public ProblemDescriptor {
    arguments = List.copyOf(arguments);
  }

  /**
   * An argument of a problem's constructor.
   *
   * @param name what it is ({@code "numberOfObjectives"})
   * @param type {@code "integer"}, {@code "number"} (a decimal) or {@code "boolean"}
   * @param defaultValue the value of the problem built with no arguments, or {@code null} when it
   *     is not known
   */
  public record Argument(String name, String type, Object defaultValue) {}
}
