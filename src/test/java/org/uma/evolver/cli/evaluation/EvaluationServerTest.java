package org.uma.evolver.cli.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for class EvaluationServer")
class EvaluationServerTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @TempDir Path tempDir;
  private EvaluationServer server;
  private Path log;
  private String configuration;

  @BeforeEach
  void setUp() throws IOException {
    Path baseLevel = tempDir.resolve("base-level.yaml");
    Files.writeString(
        baseLevel,
        """
        algorithmName: NSGA-II
        populationSize: 100
        numberOfIndependentRuns: 1
        yamlParameterSpaceFile: NSGAIIDouble.yaml
        trainingProblemNames: [ZDT1, ZDT4]
        trainingReferenceFrontFileNames:
          - resources/referenceFronts/ZDT1.csv
          - resources/referenceFronts/ZDT4.csv
        trainingEvaluations: [1000, 1000]
        indicatorNames: [Epsilon, NormalizedHypervolume]
        """);
    log = tempDir.resolve("log.csv");
    server = new EvaluationServer(baseLevel.toString(), 0, 1, log);
    server.start();
    configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);
  }

  @AfterEach
  void tearDown() {
    server.stop(Duration.ofSeconds(5));
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return HTTP.send(
        HttpRequest.newBuilder(EvaluationServer.uri(server.port(), path))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return HTTP.send(
        HttpRequest.newBuilder(EvaluationServer.uri(server.port(), path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private String request(String problem) throws IOException {
    return JSON.writeValueAsString(
        JSON.createObjectNode()
            .put("configuration", configuration)
            .put("problem", problem)
            .put("tool", "test")
            .put("seed", 7));
  }

  @Test
  @DisplayName(
      "given a configuration, when evaluated through the API, then the indicators are those of a"
          + " direct evaluation with the same random sequence")
  void givenAConfiguration_whenEvaluatedThroughTheApi_thenTheValuesAreThoseOfADirectEvaluation()
      throws Exception {
    // Arrange: the same seed before each evaluation, with a single evaluation thread
    JMetalRandom.getInstance().setSeed(1);
    var direct = server.evaluate(configuration, "ZDT1");
    JMetalRandom.getInstance().setSeed(1);

    // Act
    HttpResponse<String> response = post("/evaluate", request("ZDT1"));

    // Assert
    assertEquals(200, response.statusCode(), response.body());
    JsonNode body = JSON.readTree(response.body());
    assertEquals("ZDT1", body.get("problem").asText());
    assertEquals(direct.values()[0], body.get("EP").asDouble(), 1e-12);
    assertEquals(direct.values()[1], body.get("NHV").asDouble(), 1e-12);
    assertTrue(body.has("timeMs"));
  }

  @Test
  @DisplayName("given cost=sum, when evaluated, then the response is the sum of the indicators")
  void givenCostSum_whenEvaluated_thenTheResponseIsTheSum() throws Exception {
    // Arrange: the indicators of the same evaluation (same seed, a single evaluation thread)
    JMetalRandom.getInstance().setSeed(1);
    JsonNode indicators = JSON.readTree(post("/evaluate", request("ZDT4")).body());
    JMetalRandom.getInstance().setSeed(1);

    // Act
    HttpResponse<String> response = post("/evaluate?cost=sum", request("ZDT4"));

    // Assert: EP is not bounded by 1, so the sum is not either
    assertEquals(200, response.statusCode());
    assertEquals(
        indicators.get("EP").asDouble() + indicators.get("NHV").asDouble(),
        Double.parseDouble(response.body()),
        1e-12);
  }

  @Test
  @DisplayName(
      "given evaluations, when they finish, then the log has a header and one line per evaluation")
  void givenEvaluations_whenTheyFinish_thenTheLogHasALinePerEvaluation() throws Exception {
    // Act
    post("/evaluate", request("ZDT1"));
    post("/evaluate", request("ZDT4"));

    // Assert
    List<String> lines = Files.readAllLines(log);
    assertEquals("Index,ElapsedMs,Tool,Problem,Seed,EP,NHV,TimeMs,Configuration", lines.get(0));
    assertEquals(3, lines.size());
    assertTrue(lines.get(1).startsWith("1,"), lines.get(1));
    assertTrue(lines.get(2).contains(",test,ZDT4,7,"), lines.get(2));
  }

  @Test
  @DisplayName(
      "given an invalid configuration, when evaluated, then it fails with 422 and is counted as"
          + " failed")
  void givenAnInvalidConfiguration_whenEvaluated_thenItFailsWith422() throws Exception {
    // Act
    HttpResponse<String> response =
        post("/evaluate", "{\"configuration\": \"--crossover NOPE\", \"problem\": \"ZDT1\"}");

    // Assert
    assertEquals(422, response.statusCode());
    JsonNode status = JSON.readTree(get("/status").body());
    assertEquals(1, status.get("evaluationsFailed").asInt());
    assertEquals(0, status.get("evaluationsDone").asInt());
  }

  @Test
  @DisplayName("given a bad request, when sent, then it is rejected with 400 or 405")
  void givenABadRequest_whenSent_thenItIsRejected() throws Exception {
    assertEquals(400, post("/evaluate", request("DTLZ2")).statusCode());
    assertEquals(400, post("/evaluate", "{\"problem\": \"ZDT1\"}").statusCode());
    assertEquals(400, post("/evaluate", "not json").statusCode());
    assertEquals(405, get("/evaluate").statusCode());
  }

  @Test
  @DisplayName(
      "given more concurrent requests than threads, when they run, then no more evaluations run at"
          + " the same time than the threads and all of them finish")
  void givenMoreRequestsThanThreads_whenTheyRun_thenTheThreadsAreNotExceeded() throws Exception {
    // Arrange: a server with 2 threads and 6 concurrent requests
    server.stop(Duration.ofSeconds(5));
    server = new EvaluationServer(tempDir.resolve("base-level.yaml").toString(), 0, 2, log);
    server.start();
    List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      responses.add(
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return post("/evaluate", request("ZDT4"));
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              }));
    }

    // Act: watch the status while they run
    long maxInProgress = 0;
    while (!responses.stream().allMatch(CompletableFuture::isDone)) {
      maxInProgress =
          Math.max(
              maxInProgress,
              JSON.readTree(get("/status").body()).get("evaluationsInProgress").asLong());
    }

    // Assert
    assertTrue(maxInProgress <= 2, "evaluations in progress: " + maxInProgress);
    for (var response : responses) {
      assertEquals(200, response.get().statusCode());
    }
    JsonNode status = JSON.readTree(get("/status").body());
    assertEquals(6, status.get("evaluationsDone").asInt());
    assertEquals(0, status.get("evaluationsInProgress").asInt());
    assertEquals(0, status.get("evaluationsWaiting").asInt());
  }

  @Test
  @DisplayName("given a running server, when asked for its status, then it describes itself")
  void givenARunningServer_whenAskedForItsStatus_thenItDescribesItself() throws Exception {
    // Act
    JsonNode status = JSON.readTree(get("/status").body());

    // Assert
    assertEquals("running", status.get("status").asText());
    assertEquals(server.port(), status.get("port").asInt());
    assertEquals(1, status.get("threads").asInt());
    assertEquals(0, status.get("evaluationsWaiting").asInt());
    assertEquals("[\"ZDT1\",\"ZDT4\"]", status.get("problems").toString());
    assertEquals("NSGA-II", status.get("algorithm").asText());
    assertEquals("Double", status.get("encoding").asText());
    assertEquals("NSGAIIDouble.yaml", status.get("parameterSpace").asText());
    assertEquals(100, status.get("populationSize").asInt());
    assertEquals("[1000,1000]", status.get("evaluationsPerRun").toString());
    assertEquals(1, status.get("runsPerProblem").asInt());
    assertTrue(server.description().startsWith("NSGA-II (Double encoding"), server.description());
    assertEquals("ok", get("/health").body());
  }

  @Test
  @DisplayName("given a running server, when asked to shut down, then it stops")
  void givenARunningServer_whenAskedToShutDown_thenItStops() throws Exception {
    // Act
    HttpResponse<String> response = post("/shutdown", "");
    server.awaitStop();

    // Assert
    assertEquals(200, response.statusCode());
    assertTrue(EvaluationServerMain.status(server.port()).isEmpty());
  }
}
