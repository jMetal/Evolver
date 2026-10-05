package org.uma.evolver.util;

import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Creates {@link JMetalException}s that keep their message and their cause.
 *
 * <p>In jMetal 7.6, the constructors {@code JMetalException(String, Exception)} and {@code
 * JMetalException(Exception)} only log the error: the exception they create has neither a message
 * nor a cause, so whatever catches it, or the status file of a run, gets {@code null} instead of
 * what went wrong. The methods of this class build the exception with {@code
 * JMetalException(String)}, which keeps the message, and attach the cause. Fixed in jMetal 7.7.
 */
public final class JMetalExceptions {

  private JMetalExceptions() {}

  /**
   * A {@link JMetalException} with the given message and cause.
   *
   * @param message what went wrong
   * @param cause the exception that caused it
   * @return the exception, to be thrown by the caller
   */
  public static JMetalException withCause(String message, Throwable cause) {
    JMetalException exception = new JMetalException(message);
    exception.initCause(cause);
    return exception;
  }

  /**
   * A {@link JMetalException} caused by the given exception, with its message.
   *
   * @param cause the exception that caused it
   * @return the exception, to be thrown by the caller
   */
  public static JMetalException withCause(Throwable cause) {
    return withCause(String.valueOf(cause.getMessage()), cause);
  }
}
