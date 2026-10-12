package org.uma.evolver.cli.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.cli.IndicatorRegistry;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;

/**
 * Evaluates configurations of a base-level algorithm on the problems of a base-level file, through
 * an HTTP API, so that external configurators (SMAC3, irace) run the same experiments as Evolver's
 * training without starting a Java virtual machine for each one.
 *
 * <p>The base-level file (the {@code baseLevel} of a training request) gives the algorithm, its
 * parameter space, the problems with their reference fronts and evaluations, the number of runs and
 * the quality indicators. Each problem gets a {@link MetaOptimizationProblem} of its own, whose
 * {@code evaluateConfiguration} computes the indicators exactly as a training does (the median over
 * the runs of the indicators of the non-dominated front, normalized with the reference front). The
 * evaluations run in a pool of threads, as Evolver's parallel meta-optimizers run them; jMetal's
 * random generator is shared by the threads, so the seed that a client sends is recorded but does
 * not make an evaluation reproducible.
 *
 * <p>The API, on the loopback interface only:
 *
 * <ul>
 *   <li>{@code POST /evaluate}, with a JSON body {@code {"configuration": "--name value ...",
 *       "problem": "ZDT4", "tool": "smac", "seed": 7}} ({@code tool} and {@code seed} are
 *       optional):
 *       the indicators of the configuration on the problem, {@code {"problem": "ZDT4", "EP": ...,
 *       "NHV": ..., "timeMs": ...}}, or, with {@code ?cost=sum}, their sum as plain text (for
 *       irace,
 *       which minimizes one value). A configuration that fails gets status 422 and the error;
 *   <li>{@code GET /health}: {@code ok};
 *   <li>{@code GET /status}: what the server is doing;
 *   <li>{@code POST /shutdown}: stops accepting evaluations, waits for those in progress and stops.
 * </ul>
 *
 * <p>Every evaluation is appended to a CSV log ({@code
 * Index,ElapsedMs,Tool,Problem,Seed,<indicators>,
 * TimeMs,Configuration}), from which the convergence of a configurator can be plotted.
 */
public final class EvaluationServer {

  /** The result of an evaluation: the values of the indicators, in the order of their names. */
  public record Evaluation(String problem, List<String> indicators, double[] values, long timeMs) {

    public Evaluation {
      indicators = List.copyOf(indicators);
      values = values.clone();
    }

    @Override
    public double[] values() {
      return values.clone();
    }
  }

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String baseLevelFile;
  private final BaseLevelConfig baseLevel;
  private final int threads;
  private final Path logFile;
  private final Map<String, MetaOptimizationProblem<?>> problems = new LinkedHashMap<>();
  private final List<String> indicatorNames = new ArrayList<>();
  private final ExecutorService evaluations;
  private final ExecutorService requests = Executors.newCachedThreadPool();
  private final HttpServer server;
  private final Instant startedAt = Instant.now();
  private final AtomicLong done = new AtomicLong();
  private final AtomicLong failed = new AtomicLong();
  private final AtomicLong inProgress = new AtomicLong();
  private final AtomicLong waiting = new AtomicLong();
  private final CountDownLatch stopped = new CountDownLatch(1);
  private final PrintWriter log;
  private volatile boolean accepting = true;

  /**
   * Builds the server, without starting it.
   *
   * @param baseLevelFile the base-level file: a name of {@code baseLevelConfigurations/} or a path
   * @param port the port, on the loopback interface (0 for any free one)
   * @param threads the number of evaluations that run at the same time
   * @param logFile the CSV log of the evaluations (appended if it exists)
   */
  public EvaluationServer(String baseLevelFile, int port, int threads, Path logFile)
      throws IOException {
    this.baseLevelFile = baseLevelFile;
    this.threads = threads;
    this.logFile = logFile;
    this.baseLevel = BaseLevelConfigurationReader.load(baseLevelFile);
    buildProblems(baseLevel);
    this.evaluations = Executors.newFixedThreadPool(threads);

    Path logDirectory = logFile.getParent();
    if (logDirectory != null) {
      Files.createDirectories(logDirectory);
    }
    boolean newLog = !Files.exists(logFile) || Files.size(logFile) == 0;
    this.log =
        new PrintWriter(
            Files.newBufferedWriter(
                logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
            true);
    if (newLog) {
      log.println(
          "Index,ElapsedMs,Tool,Problem,Seed,"
              + String.join(",", indicatorNames)
              + ",TimeMs,Configuration");
    }

    this.server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
    server.setExecutor(requests);
    server.createContext("/evaluate", this::handleEvaluate);
    server.createContext("/health", exchange -> send(exchange, 200, "text/plain", "ok"));
    server.createContext("/status", exchange -> send(exchange, 200, "application/json", status()));
    server.createContext("/shutdown", this::handleShutdown);
  }

  private void buildProblems(BaseLevelConfig baseLevel) {
    List<QualityIndicator> indicators = new ArrayList<>();
    for (String name : baseLevel.indicatorNames()) {
      QualityIndicator indicator = IndicatorRegistry.resolve(name);
      indicators.add(indicator);
      indicatorNames.add(indicator.name());
    }
    BaseLevelAlgorithm<?> algorithm =
        BaseAlgorithmRegistry.resolve(
            baseLevel.algorithmName(),
            baseLevel.encoding(),
            baseLevel.populationSize(),
            BaseAlgorithmRegistry.resolveParameterSpace(
                baseLevel.encoding(), baseLevel.yamlParameterSpaceFile()),
            baseLevel.extraConfig());
    List<ProblemSpec> specs = baseLevel.trainingProblemNames();
    for (int i = 0; i < specs.size(); i++) {
      Problem<?> problem = ProblemRegistry.resolve(specs.get(i));
      ProblemRegistry.checkEncoding(problem, baseLevel.encoding());
      problems.put(
          specs.get(i).className(),
          metaProblem(
              algorithm,
              problem,
              baseLevel.trainingReferenceFrontFileNames().get(i),
              indicators,
              baseLevel.trainingEvaluations().get(i),
              baseLevel.numberOfIndependentRuns()));
    }
  }

  @SuppressWarnings("unchecked")
  private static <S extends Solution<?>> MetaOptimizationProblem<S> metaProblem(
      BaseLevelAlgorithm<S> algorithm,
      Problem<?> problem,
      String referenceFront,
      List<QualityIndicator> indicators,
      int evaluations,
      int runs) {
    return new MetaOptimizationProblem<>(
        algorithm,
        List.of((Problem<S>) problem),
        List.of(referenceFront),
        indicators,
        new FixedEvaluationsStrategy(List.of(evaluations)),
        runs);
  }

  /** Starts listening. */
  public void start() {
    server.start();
  }

  /** The port it listens on. */
  public int port() {
    return server.getAddress().getPort();
  }

  /** The names of the indicators, as in the responses and in the log. */
  public List<String> indicatorNames() {
    return List.copyOf(indicatorNames);
  }

  /**
   * A sentence that says what the server evaluates: the algorithm, its encoding and parameter
   * space, and the problems.
   */
  public String description() {
    return "%s (%s encoding, space %s) on %s, with %s evaluations per run and %d run(s) per problem"
        .formatted(
            baseLevel.algorithmName(),
            baseLevel.encoding(),
            baseLevel.yamlParameterSpaceFile(),
            problemNames(),
            baseLevel.trainingEvaluations().stream().distinct().toList(),
            baseLevel.numberOfIndependentRuns());
  }

  /** The names of the problems that it evaluates on. */
  public List<String> problemNames() {
    return List.copyOf(problems.keySet());
  }

  /** Blocks until the server has stopped. */
  public void awaitStop() throws InterruptedException {
    stopped.await();
  }

  /**
   * Stops accepting evaluations, waits for those in progress (at most {@code timeout}) and stops.
   */
  public void stop(Duration timeout) {
    accepting = false;
    evaluations.shutdown();
    try {
      evaluations.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    evaluations.shutdownNow();
    server.stop(0);
    requests.shutdownNow();
    log.close();
    stopped.countDown();
  }

  /**
   * Evaluates a configuration on a problem, in the calling thread.
   *
   * @throws IllegalArgumentException if the problem is not one of the server's
   * @throws RuntimeException if the configuration fails
   */
  public Evaluation evaluate(String configuration, String problem) {
    MetaOptimizationProblem<?> metaProblem = problems.get(problem);
    if (metaProblem == null) {
      throw new IllegalArgumentException(
          "Unknown problem: " + problem + ". The problems of the server are " + problems.keySet());
    }
    long start = System.currentTimeMillis();
    double[] values = metaProblem.evaluateConfiguration(configuration.trim().split("\\s+"));
    return new Evaluation(problem, indicatorNames(), values, System.currentTimeMillis() - start);
  }

  private void handleEvaluate(HttpExchange exchange) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      send(exchange, 405, "text/plain", "Use POST");
      return;
    }
    if (!accepting) {
      send(exchange, 503, "text/plain", "The server is shutting down");
      return;
    }
    JsonNode body;
    try {
      body = JSON.readTree(exchange.getRequestBody());
    } catch (IOException e) {
      send(exchange, 400, "text/plain", "The body is not JSON: " + e.getMessage());
      return;
    }
    String configuration = body.path("configuration").asText(null);
    String problem = body.path("problem").asText(null);
    if (configuration == null || problem == null) {
      send(exchange, 400, "text/plain", "The body needs 'configuration' and 'problem'");
      return;
    }
    if (!problems.containsKey(problem)) {
      send(
          exchange,
          400,
          "text/plain",
          "Unknown problem: " + problem + ". The problems are " + problems.keySet());
      return;
    }
    String tool = body.path("tool").asText("");
    String seed = body.path("seed").isMissingNode() ? "" : body.path("seed").asText();

    waiting.incrementAndGet();
    Future<Evaluation> future =
        evaluations.submit(
            () -> {
              waiting.decrementAndGet();
              inProgress.incrementAndGet();
              try {
                return evaluate(configuration, problem);
              } finally {
                inProgress.decrementAndGet();
              }
            });
    Evaluation evaluation;
    try {
      evaluation = future.get();
    } catch (ExecutionException e) {
      failed.incrementAndGet();
      send(exchange, 422, "text/plain", String.valueOf(e.getCause().getMessage()));
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      send(exchange, 503, "text/plain", "Interrupted");
      return;
    }
    long index = done.incrementAndGet();
    logEvaluation(index, tool, seed, evaluation, configuration);

    if ("cost=sum".equals(exchange.getRequestURI().getQuery())) {
      double sum = 0.0;
      for (double value : evaluation.values()) {
        sum += value;
      }
      send(exchange, 200, "text/plain", String.valueOf(sum));
      return;
    }
    ObjectNode response = JSON.createObjectNode();
    response.put("problem", evaluation.problem());
    for (int i = 0; i < evaluation.indicators().size(); i++) {
      response.put(evaluation.indicators().get(i), evaluation.values()[i]);
    }
    response.put("timeMs", evaluation.timeMs());
    send(exchange, 200, "application/json", JSON.writeValueAsString(response));
  }

  private synchronized void logEvaluation(
      long index, String tool, String seed, Evaluation evaluation, String configuration) {
    StringBuilder line = new StringBuilder();
    line.append(index)
        .append(',')
        .append(Duration.between(startedAt, Instant.now()).toMillis())
        .append(',')
        .append(tool)
        .append(',')
        .append(evaluation.problem())
        .append(',')
        .append(seed);
    for (double value : evaluation.values()) {
      line.append(',').append(value);
    }
    line.append(',')
        .append(evaluation.timeMs())
        .append(",\"")
        .append(configuration.trim())
        .append('"');
    log.println(line);
  }

  private String status() throws IOException {
    ObjectNode status = JSON.createObjectNode();
    status.put("status", accepting ? "running" : "stopping");
    status.put("port", port());
    status.put("startedAt", startedAt.toString());
    status.put("uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds());
    status.put("threads", threads);
    status.put("evaluationsDone", done.get());
    status.put("evaluationsFailed", failed.get());
    status.put("evaluationsInProgress", inProgress.get());
    status.put("evaluationsWaiting", waiting.get());
    status.put("baseLevel", baseLevelFile);
    status.put("algorithm", baseLevel.algorithmName());
    status.put("encoding", baseLevel.encoding());
    status.put("parameterSpace", baseLevel.yamlParameterSpaceFile());
    status.put("populationSize", baseLevel.populationSize());
    status.putPOJO("evaluationsPerRun", baseLevel.trainingEvaluations());
    status.put("runsPerProblem", baseLevel.numberOfIndependentRuns());
    status.putPOJO("problems", problemNames());
    status.putPOJO("indicators", indicatorNames());
    status.put("log", logFile.toAbsolutePath().toString());
    status.put("pid", ProcessHandle.current().pid());
    return JSON.writeValueAsString(status);
  }

  private void handleShutdown(HttpExchange exchange) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      send(exchange, 405, "text/plain", "Use POST");
      return;
    }
    accepting = false;
    send(exchange, 200, "text/plain", "Shutting down");
    Thread.ofPlatform().start(() -> stop(Duration.ofMinutes(5)));
  }

  private static void send(HttpExchange exchange, int code, String type, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
    exchange.sendResponseHeaders(code, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  /** The URI of an endpoint of a server on this machine. */
  static URI uri(int port, String path) {
    return URI.create("http://127.0.0.1:" + port + path);
  }
}
