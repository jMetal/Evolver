package org.uma.evolver.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.auto.parameter.CategoricalParameter;
import org.uma.jmetal.auto.parameter.RealParameter;
import org.uma.jmetal.util.errorchecking.exception.ValueOutOfRangeException;

class ParameterManagementTest {

  @Test
  void parameterValueIsNegative() {
    CategoricalParameter parameter = new CategoricalParameter("parameter", List.of("Element"));
    assertThrows(ValueOutOfRangeException.class, () -> ParameterManagement.decodeParameter(parameter, -0.9));
  }

  @Test
  void parameterValueIsHigherThanOne() {
    CategoricalParameter parameter = new CategoricalParameter("parameter", List.of("Element"));
    assertThrows(ValueOutOfRangeException.class, () -> ParameterManagement.decodeParameter(parameter, 1.001));
  }

  @Nested
  @DisplayName("Unit tests for method decodeParameter()")
  class DecodeParameterTests {

    @Nested
    @DisplayName("Tests for categorical parameters")
    class categoricalParameterTests {
      @Test
      void decodeAParameterWithASingleValidElementAndAValueOfZeroReturnTheElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter", List.of("Element"));

        String expectedResult = "Element";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithASingleValidElementAndAValueOfOneReturnTheElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter", List.of("Element"));

        String expectedResult = "Element";
        String actualResult = ParameterManagement.decodeParameter(parameter, 1.0);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithTwoElementsAndValueOfZeroReturnTheFirstElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2"));

        String expectedResult = "Element1";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.0);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithTwoElementsAndValueLowerThanZeroPointFiveReturnTheSecondElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2"));

        String expectedResult = "Element1";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.499999);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithTwoElementsAndValueHigherThanZeroPointFiveReturnTheFirstElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2"));

        String expectedResult = "Element2";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.500001);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithThreeElementsAndValueLowerThanZeroPoint33eReturnTheFirstElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2", "Element3"));

        String expectedResult = "Element1";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.3333332);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithThreeElementsAndValueOfZeroPoint665eReturnTheSecondElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2", "Element3"));

        String expectedResult = "Element2";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.665);

        assertEquals(expectedResult, actualResult);
      }

      @Test
      void decodeAParameterWithThreeElementsAndValueHigherThanZeroPoint66eReturnTheThirdElement() {
        CategoricalParameter parameter = new CategoricalParameter("parameter",
            List.of("Element1", "Element2", "Element3"));

        String expectedResult = "Element3";
        String actualResult = ParameterManagement.decodeParameter(parameter, 0.6667);

        assertEquals(expectedResult, actualResult);
      }
    }

    @Nested
    @DisplayName("Tests for real parameters")
    class realParameterTests{
      RealParameter parameter ;
      double lowerBound = 0.5 ;
      double upperBound = 6.2 ;
      @BeforeEach
      void setup() {
        parameter = new RealParameter("ParameterName", lowerBound, upperBound) ;
      }

      @Test
      void decodeAParameterWithValueZeroReturnTheLowerBound() {
        String actualValue = ParameterManagement.decodeParameter(parameter, 0.0) ;

        assertEquals(""+lowerBound, actualValue) ;
      }

      @Test
      void decodeAParameterWithValueOneReturnTheUpperBound() {
        String actualValue = ParameterManagement.decodeParameter(parameter, 1.0) ;

        assertEquals(""+upperBound, actualValue) ;
      }

      @Test
      void decodeAParameterWithValueZeroPointFiveReturnTheRightValue() {
        String actualValue = ParameterManagement.decodeParameter(parameter, 0.5) ;
        double meanValue = (lowerBound + upperBound) /2.0 ;

        assertEquals(""+meanValue, actualValue) ;
      }
    }
  }

}