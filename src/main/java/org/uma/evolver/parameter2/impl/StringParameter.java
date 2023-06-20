package org.uma.evolver.parameter2.impl;

import java.util.function.Function;
import org.uma.evolver.parameter2.Parameter;
import org.uma.jmetal.util.errorchecking.Check;

public class StringParameter extends Parameter<String> {

  public StringParameter(String name) {
    super(name);
  }

  @Override
  public void check() {
    Check.that(!name().equals(""), "The parameter name cannot be the empty string");
    Check.notNull(name());
  }

  @Override
  public Parameter<String> parse(String[] arguments) {
    return super.parse(Function.identity(), arguments);
  }
}
