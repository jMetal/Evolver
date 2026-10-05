package org.uma.evolver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class JMetalExceptions")
class JMetalExceptionsTest {

  @Test
  @DisplayName("given a message and a cause, when created, then the exception keeps both")
  void givenAMessageAndACause_whenCreated_thenTheExceptionKeepsBoth() {
    // Arrange
    IOException cause = new IOException("disk full");

    // Act
    JMetalException exception = JMetalExceptions.withCause("Error writing metadata", cause);

    // Assert
    assertEquals("Error writing metadata", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  @DisplayName("given only a cause, when created, then the exception takes its message")
  void givenOnlyACause_whenCreated_thenTheExceptionTakesItsMessage() {
    // Arrange
    IOException cause = new IOException("disk full");

    // Act
    JMetalException exception = JMetalExceptions.withCause(cause);

    // Assert
    assertEquals("disk full", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
