package org.uma.evolver.meta.llm;

/**
 * A message of a chat with a language model.
 *
 * @param role {@code system}, {@code user} or {@code assistant}
 * @param content the text of the message
 */
public record LlmMessage(String role, String content) {

  /** A message of the user. */
  public static LlmMessage user(String content) {
    return new LlmMessage("user", content);
  }

  /** A system message: the instructions that frame the chat. */
  public static LlmMessage system(String content) {
    return new LlmMessage("system", content);
  }
}
