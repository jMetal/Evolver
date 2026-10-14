package org.uma.evolver.meta.llm;

/**
 * The answer of a language model to a chat, with what it cost.
 *
 * @param content the text of the answer (JSON when a schema was given)
 * @param model the model that answered, as the server reports it
 * @param promptTokens the tokens of the prompt (0 if the server does not report them)
 * @param completionTokens the tokens of the answer (0 if the server does not report them)
 * @param millis the time of the request, in milliseconds
 */
public record LlmResponse(
    String content, String model, int promptTokens, int completionTokens, long millis) {}
