package org.uma.evolver.parameter.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class BooleanParameter")
class BooleanParameterTest {

  @Test
  void shouldParseTrueValueCorrectly() {
    BooleanParameter param = new BooleanParameter("enabled");
    param.parse(new String[] {"--enabled", "true"});
    assertTrue(param.value());
  }

  @Test
  void shouldParseFalseValueCorrectly() {
    BooleanParameter param = new BooleanParameter("enabled");
    param.parse(new String[] {"--enabled", "false"});
    assertFalse(param.value());
  }

  @Test
  void shouldBeCaseInsensitive() {
    BooleanParameter param1 = new BooleanParameter("enabled");
    param1.parse(new String[] {"--enabled", "TrUe"});
    assertTrue(param1.value());

    BooleanParameter param2 = new BooleanParameter("enabled");
    param2.parse(new String[] {"--enabled", "FaLsE"});
    assertFalse(param2.value());
  }

  @Test
  void shouldThrowExceptionForMissingValue() {
    BooleanParameter param = new BooleanParameter("enabled");
    assertThrows(InvalidConditionException.class, () -> param.parse(new String[] {"--enabled"}));
  }

  @Test
  void shouldThrowExceptionForNullName() {
    assertThrows(NullParameterException.class, () -> new BooleanParameter(null));
  }

  @Test
  void shouldThrowExceptionForBlankName() {
    assertThrows(InvalidConditionException.class, () -> new BooleanParameter("  "));
  }
}
