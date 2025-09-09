package org.uma.evolver.parameter.type;

import java.util.function.Function;
import org.uma.evolver.parameter.Parameter;

public class StringParameter extends Parameter<String> {

  public StringParameter(String name) {
    super(name);
  }

  @Override
  public void parse(String[] arguments) {
    super.parse(Function.identity(), arguments);
  }
}
