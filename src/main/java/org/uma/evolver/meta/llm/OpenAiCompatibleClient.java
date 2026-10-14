package org.uma.evolver.meta.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A model behind the OpenAI chat completions API ({@code POST <baseUrl>/chat/completions}), the one
 * that OpenAI, vLLM, LM Studio, the llama.cpp server, OpenRouter and Ollama itself ({@code
 * http://localhost:11434/v1}) offer. The JSON schema of the answer goes in {@code response_format}
 * ({@code json_schema}, strict). The key, when the server needs one, is sent as a bearer token;
 * {@code options} are added to the request as they are.
 */
public final class OpenAiCompatibleClient implements LlmClient {

  private final URI uri;
  private final String model;
  private final String apiKey;
  private final double temperature;
  private final Map<String, Object> options;
  private final HttpJsonClient http;

  /**
   * @param baseUrl the address of the API, with its version, e.g. {@code https://api.openai.com/v1}
   * @param model the name of the model
   * @param apiKey the key of the API, or null if the server needs none
   * @param temperature the sampling temperature
   * @param timeout the time to wait for an answer
   * @param options more fields of the request, added as they are (may be empty)
   */
  public OpenAiCompatibleClient(
      String baseUrl,
      String model,
      String apiKey,
      double temperature,
      Duration timeout,
      Map<String, Object> options) {
    this.uri = URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions");
    this.model = model;
    this.apiKey = apiKey;
    this.temperature = temperature;
    this.options = Map.copyOf(options);
    this.http = new HttpJsonClient(timeout);
  }

  @Override
  public LlmResponse chat(List<LlmMessage> messages, String jsonSchema) throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    body.put("temperature", temperature);
    if (jsonSchema != null) {
      body.put(
          "response_format",
          Map.of(
              "type",
              "json_schema",
              "json_schema",
              Map.of(
                  "name", "answer",
                  "strict", true,
                  "schema", HttpJsonClient.JSON.readTree(jsonSchema))));
    }
    body.putAll(options);

    Map<String, String> headers = apiKey == null ? Map.of() : Map.of("Authorization", "Bearer " + apiKey);
    long start = System.currentTimeMillis();
    JsonNode answer = http.post(uri, body, headers);
    long millis = System.currentTimeMillis() - start;
    return new LlmResponse(
        answer.path("choices").path(0).path("message").path("content").asText(),
        answer.hasNonNull("model") ? answer.get("model").asText() : model,
        answer.path("usage").path("prompt_tokens").asInt(0),
        answer.path("usage").path("completion_tokens").asInt(0),
        millis);
  }

  @Override
  public String model() {
    return model;
  }
}
