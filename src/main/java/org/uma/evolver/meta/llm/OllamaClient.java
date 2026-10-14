package org.uma.evolver.meta.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A model served by <a href="https://ollama.com">Ollama</a>, usually on the same computer, through
 * its chat API ({@code POST /api/chat}). The JSON schema of the answer goes in {@code format}, which
 * Ollama enforces while it generates. {@code options} are added to the request as they are, which
 * covers Ollama's own settings: {@code think} (reasoning on or off, or its level), {@code
 * keep_alive}, and the {@code options} of the model ({@code num_ctx}...).
 */
public final class OllamaClient implements LlmClient {

  /** The address of a local Ollama. */
  public static final String DEFAULT_BASE_URL = "http://localhost:11434";

  private final URI uri;
  private final String model;
  private final double temperature;
  private final Map<String, Object> options;
  private final HttpJsonClient http;

  /**
   * @param baseUrl the address of the server, e.g. {@link #DEFAULT_BASE_URL}
   * @param model the name of the model, e.g. {@code qwen3.5:35b-mlx}
   * @param temperature the sampling temperature
   * @param timeout the time to wait for an answer
   * @param options more fields of the request, added as they are (may be empty)
   */
  public OllamaClient(
      String baseUrl, String model, double temperature, Duration timeout, Map<String, Object> options) {
    this.uri = URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat");
    this.model = model;
    this.temperature = temperature;
    this.options = Map.copyOf(options);
    this.http = new HttpJsonClient(timeout);
  }

  @Override
  public LlmResponse chat(List<LlmMessage> messages, String jsonSchema) throws IOException {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    body.put("stream", false);
    if (jsonSchema != null) {
      body.put("format", HttpJsonClient.JSON.readTree(jsonSchema));
    }
    Map<String, Object> modelOptions = new LinkedHashMap<>();
    modelOptions.put("temperature", temperature);
    for (Map.Entry<String, Object> option : options.entrySet()) {
      if (option.getKey().equals("options") && option.getValue() instanceof Map<?, ?> extra) {
        extra.forEach((key, value) -> modelOptions.put(String.valueOf(key), value));
      } else {
        body.put(option.getKey(), option.getValue());
      }
    }
    body.put("options", modelOptions);

    long start = System.currentTimeMillis();
    JsonNode answer = http.post(uri, body, Map.of());
    long millis = System.currentTimeMillis() - start;
    return new LlmResponse(
        answer.path("message").path("content").asText(),
        answer.hasNonNull("model") ? answer.get("model").asText() : model,
        answer.path("prompt_eval_count").asInt(0),
        answer.path("eval_count").asInt(0),
        millis);
  }

  @Override
  public String model() {
    return model;
  }
}
