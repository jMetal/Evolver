package org.uma.evolver.meta.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/**
 * Which language model to use, read from a YAML file:
 *
 * <pre>{@code
 * provider: ollama              # or openai, for any server of the OpenAI chat API
 * baseUrl: http://localhost:11434
 * model: qwen3.5:35b-mlx
 * apiKeyEnv: OPENAI_API_KEY     # the environment variable with the key, if the server needs one
 * temperature: 0.7
 * timeoutSeconds: 120
 * options:                      # more fields of the request, as they are
 *   think: false
 * }</pre>
 *
 * <p>The key itself is never written in the file: {@code apiKeyEnv} names the environment variable
 * that holds it.
 *
 * @param provider {@code ollama} or {@code openai}
 * @param baseUrl the address of the server (for {@code openai}, with the version, e.g. {@code /v1})
 * @param model the name of the model
 * @param apiKeyEnv the environment variable with the key, or null
 * @param temperature the sampling temperature
 * @param timeoutSeconds the time to wait for an answer
 * @param options more fields of the request
 */
public record LlmSettings(
    String provider,
    String baseUrl,
    String model,
    String apiKeyEnv,
    double temperature,
    int timeoutSeconds,
    Map<String, Object> options) {

  public LlmSettings {
    options = Map.copyOf(options);
  }

  /**
   * Reads the settings of a YAML file.
   *
   * @param file the file
   * @return the settings
   * @throws IOException if the file cannot be read
   * @throws JMetalException if a field is missing or not valid
   */
  @SuppressWarnings("unchecked")
  public static LlmSettings load(Path file) throws IOException {
    Map<String, Object> data;
    try (InputStream stream = Files.newInputStream(file)) {
      data = new Yaml().load(stream);
    }
    if (data == null) {
      throw new JMetalException("Empty settings file: " + file);
    }
    String provider = String.valueOf(data.getOrDefault("provider", "ollama"));
    if (!provider.equals("ollama") && !provider.equals("openai")) {
      throw new JMetalException("provider must be ollama or openai, not " + provider);
    }
    Object model = data.get("model");
    if (model == null) {
      throw new JMetalException("The settings file " + file + " has no model");
    }
    String baseUrl =
        String.valueOf(
            data.getOrDefault(
                "baseUrl", provider.equals("ollama") ? OllamaClient.DEFAULT_BASE_URL : ""));
    if (baseUrl.isEmpty()) {
      throw new JMetalException("The settings file " + file + " has no baseUrl");
    }
    Object options = data.getOrDefault("options", Map.of());
    return new LlmSettings(
        provider,
        baseUrl,
        String.valueOf(model),
        data.get("apiKeyEnv") == null ? null : String.valueOf(data.get("apiKeyEnv")),
        ((Number) data.getOrDefault("temperature", 0.7)).doubleValue(),
        ((Number) data.getOrDefault("timeoutSeconds", 120)).intValue(),
        new LinkedHashMap<>((Map<String, Object>) options));
  }

  /**
   * The client of these settings.
   *
   * @return an {@link OllamaClient} or an {@link OpenAiCompatibleClient}
   * @throws JMetalException if {@code apiKeyEnv} names an environment variable that is not set
   */
  public LlmClient client() {
    String apiKey = null;
    if (apiKeyEnv != null) {
      apiKey = System.getenv(apiKeyEnv);
      if (apiKey == null || apiKey.isBlank()) {
        throw new JMetalException("The environment variable " + apiKeyEnv + " is not set");
      }
    }
    Duration timeout = Duration.ofSeconds(timeoutSeconds);
    return provider.equals("ollama")
        ? new OllamaClient(baseUrl, model, temperature, timeout, options)
        : new OpenAiCompatibleClient(baseUrl, model, apiKey, temperature, timeout, options);
  }
}
