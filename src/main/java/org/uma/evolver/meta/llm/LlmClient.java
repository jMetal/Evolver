package org.uma.evolver.meta.llm;

import java.io.IOException;
import java.util.List;

/**
 * A language model behind an HTTP API, local or remote. Evolver uses it only through this
 * interface, so the same code works with a model served by Ollama on the computer
 * ({@link OllamaClient}) or with any server of the OpenAI chat API, local or external ({@link
 * OpenAiCompatibleClient}).
 *
 * <p>Language models are not deterministic, even with a fixed seed: whoever uses them should log
 * every call (see {@link ConfigurationProposer}).
 */
public interface LlmClient {

  /**
   * Sends a chat to the model.
   *
   * @param messages the messages, in order
   * @param jsonSchema a JSON schema that the answer must follow, or null for free text
   * @return the answer
   * @throws IOException if the server cannot be reached or answers with an error
   */
  LlmResponse chat(List<LlmMessage> messages, String jsonSchema) throws IOException;

  /** The name of the model, as configured. */
  String model();
}
