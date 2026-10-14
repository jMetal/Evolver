package org.uma.evolver.meta.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for the clients of language models")
class LlmClientsTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private HttpServer server;
  private final List<JsonNode> bodies = new ArrayList<>();
  private final List<String> authorizations = new ArrayList<>();

  /** A server that answers every request on a path with a fixed body and status. */
  private String start(String path, int status, String answer) throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        path,
        exchange -> {
          bodies.add(JSON.readTree(exchange.getRequestBody().readAllBytes()));
          authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Nested
  @DisplayName("When using Ollama's chat API")
  class Ollama {

    @Test
    @DisplayName("given a schema and options, when chatting, then the request has them and the answer is read")
    void givenSchemaAndOptions_whenChatting_thenRequestAndAnswerAreRight() throws IOException {
      // Arrange
      String url =
          start(
              "/api/chat",
              200,
              "{\"model\": \"m1\", \"message\": {\"content\": \"{}\"}, \"prompt_eval_count\": 12,"
                  + " \"eval_count\": 3}");
      var client =
          new OllamaClient(
              url, "m1", 0.5, Duration.ofSeconds(5), Map.of("think", false, "options", Map.of("num_ctx", 8192)));

      // Act
      LlmResponse response =
          client.chat(List.of(LlmMessage.user("hello")), "{\"type\": \"object\"}");

      // Assert
      JsonNode body = bodies.get(0);
      assertEquals("m1", body.get("model").asText());
      assertEquals("object", body.get("format").get("type").asText());
      assertFalse(body.get("think").asBoolean());
      assertEquals(0.5, body.get("options").get("temperature").asDouble());
      assertEquals(8192, body.get("options").get("num_ctx").asInt());
      assertEquals("hello", body.get("messages").get(0).get("content").asText());
      assertEquals("{}", response.content());
      assertEquals(12, response.promptTokens());
      assertEquals(3, response.completionTokens());
    }

    @Test
    @DisplayName("given a server error, when chatting, then it fails with the status")
    void givenServerError_whenChatting_thenItFails() throws IOException {
      // Arrange
      String url = start("/api/chat", 404, "model not found");
      var client = new OllamaClient(url, "m1", 0.5, Duration.ofSeconds(5), Map.of());

      // Act & Assert
      IOException exception =
          assertThrows(IOException.class, () -> client.chat(List.of(LlmMessage.user("x")), null));
      assertTrue(exception.getMessage().contains("404"));
    }
  }

  @Nested
  @DisplayName("When using the OpenAI chat API")
  class OpenAi {

    @Test
    @DisplayName("given a key and a schema, when chatting, then they are sent and the answer is read")
    void givenKeyAndSchema_whenChatting_thenTheyAreSent() throws IOException {
      // Arrange
      String url =
          start(
              "/v1/chat/completions",
              200,
              "{\"model\": \"gpt\", \"choices\": [{\"message\": {\"content\": \"ok\"}}],"
                  + " \"usage\": {\"prompt_tokens\": 7, \"completion_tokens\": 2}}");
      var client =
          new OpenAiCompatibleClient(
              url + "/v1", "gpt", "secret", 0.2, Duration.ofSeconds(5), Map.of("reasoning_effort", "low"));

      // Act
      LlmResponse response = client.chat(List.of(LlmMessage.user("x")), "{\"type\": \"object\"}");

      // Assert
      JsonNode body = bodies.get(0);
      assertEquals("Bearer secret", authorizations.get(0));
      assertEquals("json_schema", body.get("response_format").get("type").asText());
      assertEquals("object", body.get("response_format").get("json_schema").get("schema").get("type").asText());
      assertEquals("low", body.get("reasoning_effort").asText());
      assertEquals("ok", response.content());
      assertEquals(7, response.promptTokens());
    }
  }

  @Nested
  @DisplayName("When reading the settings")
  class Settings {

    @Test
    @DisplayName("given an Ollama file, when loaded, then its client is an Ollama client")
    void givenOllamaFile_whenLoaded_thenOllamaClient(@TempDir Path dir) throws IOException {
      // Arrange
      Path file = dir.resolve("llm.yaml");
      Files.writeString(file, "provider: ollama\nmodel: qwen\noptions:\n  think: false\n");

      // Act
      LlmSettings settings = LlmSettings.load(file);

      // Assert
      assertEquals(OllamaClient.DEFAULT_BASE_URL, settings.baseUrl());
      assertTrue(settings.client() instanceof OllamaClient);
      assertEquals(Map.of("think", false), settings.options());
    }

    @Test
    @DisplayName("given a key variable that is not set, when building the client, then it fails")
    void givenUnsetKeyVariable_whenBuildingClient_thenItFails(@TempDir Path dir) throws IOException {
      // Arrange
      Path file = dir.resolve("llm.yaml");
      Files.writeString(
          file,
          "provider: openai\nbaseUrl: https://api.example.com/v1\nmodel: m\n"
              + "apiKeyEnv: EVOLVER_TEST_KEY_THAT_IS_NOT_SET\n");
      LlmSettings settings = LlmSettings.load(file);

      // Act & Assert
      assertThrows(JMetalException.class, settings::client);
    }

    @Test
    @DisplayName("given an unknown provider, when loaded, then it fails")
    void givenUnknownProvider_whenLoaded_thenItFails(@TempDir Path dir) throws IOException {
      // Arrange
      Path file = dir.resolve("llm.yaml");
      Files.writeString(file, "provider: other\nmodel: m\n");

      // Act & Assert
      assertThrows(JMetalException.class, () -> LlmSettings.load(file));
    }
  }
}
