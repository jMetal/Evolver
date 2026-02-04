package org.uma.evolver.analysis.ablation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ConfigurationParser Tests")
class ConfigurationParserTest {

  @Nested
  @DisplayName("Parse")
  class Parse {

    @Test
    @DisplayName("Given valid config, when parsing, then returns map")
    void givenValidConfig_whenParse_thenReturnsMap() {
      String config = "--a 1 --b two --c=three";

      Map<String, String> result = ConfigurationParser.parse(config);

      assertEquals(3, result.size());
      assertEquals("1", result.get("a"));
      assertEquals("two", result.get("b"));
      assertEquals("three", result.get("c"));
    }

    @Test
    @DisplayName("Given quoted values, when parsing, then preserves spaces")
    void givenQuotedValues_whenParse_thenPreservesSpaces() {
      String config = "--a \"value with spaces\" --b 'x y'";

      Map<String, String> result = ConfigurationParser.parse(config);

      assertEquals("value with spaces", result.get("a"));
      assertEquals("x y", result.get("b"));
    }

    @Test
    @DisplayName("Given missing value, when parsing, then throws")
    void givenMissingValue_whenParse_thenThrows() {
      String config = "--a 1 --b";

      assertThrows(IllegalArgumentException.class, () -> ConfigurationParser.parse(config));
    }
  }

  @Nested
  @DisplayName("ToArgs")
  class ToArgs {

    @Test
    @DisplayName("Given map, when converting, then returns args array")
    void givenMap_whenToArgs_thenReturnsArgsArray() {
      Map<String, String> config = new java.util.LinkedHashMap<>();
      config.put("a", "1");
      config.put("b", "two");

      String[] result = ConfigurationParser.toArgs(config);

      assertArrayEquals(new String[] {"--a", "1", "--b", "two"}, result);
    }
  }
}
