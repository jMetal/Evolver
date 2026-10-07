package org.uma.evolver.cli.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Starts, inspects and stops an {@link EvaluationServer}, the server that evaluates configurations
 * for external configurators (SMAC3, irace).
 *
 * <pre>{@code
 * java -cp Evolver-*-jar-with-dependencies.jar \
 *     org.uma.evolver.cli.evaluation.EvaluationServerMain \
 *     start TutorialZdtBaseLevel.yaml --threads 12
 * java -cp ... EvaluationServerMain status
 * java -cp ... EvaluationServerMain stop
 * }</pre>
 *
 * <ul>
 *   <li>{@code start <baseLevel.yaml> [--port 8765] [--threads N] [--log file] [--state-dir dir]}:
 *       starts the server as a background process (a new Java virtual machine with the same
 *       classpath), waits until it answers, and writes a state file ({@code
 *       server-<port>.yaml}: pid,
 *       port, start time, base-level file, log) in the state directory;
 *   <li>{@code status [--port 8765] [--state-dir dir]}: prints what the server is doing, or that
 *       it is
 *       not running (exit code 1), removing a state file left by a server that is no longer there;
 *   <li>{@code stop [--port 8765] [--force] [--state-dir dir]}: asks the server to stop after the
 *       evaluations in progress; with {@code --force}, kills its process if it does not answer;
 *   <li>{@code run <baseLevel.yaml> [--port 8765] [--threads N] [--log file]}: the server in the
 *       foreground (what {@code start} launches), until it is stopped.
 * </ul>
 *
 * <p>The defaults: port 8765, 12 threads (the evaluations that run at the same time; a client that
 * sends more requests than that has them wait), the state directory {@code
 * results/evaluation-server} and the log {@code evaluations-<port>.csv} in it.
 */
public final class EvaluationServerMain {

  static final int DEFAULT_PORT = 8765;
  static final int DEFAULT_THREADS = 12;
  static final String DEFAULT_STATE_DIR = "results/evaluation-server";
  private static final Duration START_TIMEOUT = Duration.ofSeconds(120);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  private static final String USAGE =
      """
      Usage: EvaluationServerMain <command> ...
        start  <baseLevel.yaml> [--port 8765] [--threads N] [--log file] [--state-dir dir]
        status [--port 8765] [--state-dir dir]
        stop   [--port 8765] [--force] [--state-dir dir]
        run    <baseLevel.yaml> [--port 8765] [--threads N] [--log file]""";

  private EvaluationServerMain() {}

  public static void main(String[] args) throws Exception {
    System.exit(execute(args));
  }

  /** Runs a command and returns its exit code (0 when it succeeded). */
  static int execute(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println(USAGE);
      return 2;
    }
    Options options = Options.parse(args);
    return switch (args[0]) {
      case "start" -> start(options);
      case "status" -> status(options);
      case "stop" -> stop(options);
      case "run" -> run(options);
      default -> {
        System.err.println("Unknown command: " + args[0] + "\n" + USAGE);
        yield 2;
      }
    };
  }

  /** The arguments of a command: the base-level file and the options. */
  record Options(
      String baseLevel, int port, int threads, Path log, Path stateDir, boolean force) {

    static Options parse(String[] args) {
      String baseLevel = null;
      int port = DEFAULT_PORT;
      int threads = DEFAULT_THREADS;
      Path log = null;
      Path stateDir = Path.of(DEFAULT_STATE_DIR);
      boolean force = false;
      for (int i = 1; i < args.length; i++) {
        switch (args[i]) {
          case "--port" -> port = Integer.parseInt(args[++i]);
          case "--threads" -> threads = Integer.parseInt(args[++i]);
          case "--log" -> log = Path.of(args[++i]);
          case "--state-dir" -> stateDir = Path.of(args[++i]);
          case "--force" -> force = true;
          default -> {
            if (args[i].startsWith("--") || baseLevel != null) {
              throw new IllegalArgumentException("Unknown argument: " + args[i] + "\n" + USAGE);
            }
            baseLevel = args[i];
          }
        }
      }
      if (log == null) {
        log = stateDir.resolve("evaluations-" + port + ".csv");
      }
      return new Options(baseLevel, port, threads, log, stateDir, force);
    }

    Path stateFile() {
      return stateDir.resolve("server-" + port + ".yaml");
    }

    Path outputFile() {
      return stateDir.resolve("server-" + port + ".out");
    }
  }

  private static int run(Options options) throws Exception {
    if (options.baseLevel() == null) {
      System.err.println("run needs a base-level file\n" + USAGE);
      return 2;
    }
    var server =
        new EvaluationServer(options.baseLevel(), options.port(), options.threads(), options.log());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(Duration.ofSeconds(10))));
    server.start();
    System.out.println(
        "Evaluation server on http://127.0.0.1:"
            + server.port()
            + " ("
            + options.threads()
            + " threads): "
            + server.description()
            + ", indicators "
            + server.indicatorNames()
            + ", log "
            + options.log());
    server.awaitStop();
    return 0;
  }

  private static int start(Options options) throws Exception {
    if (options.baseLevel() == null) {
      System.err.println("start needs a base-level file\n" + USAGE);
      return 2;
    }
    if (status(options.port()).isPresent()) {
      System.err.println("A server is already running on port " + options.port());
      return 1;
    }
    Files.createDirectories(options.stateDir());
    String java =
        ProcessHandle.current()
            .info()
            .command()
            .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    List<String> command =
        new ArrayList<>(
            List.of(
                java,
                "-cp",
                System.getProperty("java.class.path"),
                EvaluationServerMain.class.getName(),
                "run",
                options.baseLevel(),
                "--port",
                String.valueOf(options.port()),
                "--threads",
                String.valueOf(options.threads()),
                "--log",
                options.log().toString()));
    Process process =
        new ProcessBuilder(command)
            .redirectErrorStream(true)
            .redirectOutput(options.outputFile().toFile())
            .start();

    Instant deadline = Instant.now().plus(START_TIMEOUT);
    while (Instant.now().isBefore(deadline)) {
      if (!process.isAlive()) {
        System.err.println(
            "The server did not start (see " + options.outputFile() + "):\n"
                + Files.readString(options.outputFile()));
        return 1;
      }
      if (status(options.port()).isPresent()) {
        writeState(options, process.pid());
        System.out.println(
            "Evaluation server started on http://127.0.0.1:"
                + options.port()
                + " (pid "
                + process.pid()
                + "): "
                + description(options.port())
                + ", log "
                + options.log()
                + ", state "
                + options.stateFile());
        return 0;
      }
      Thread.sleep(250);
    }
    process.destroy();
    System.err.println("The server did not answer in " + START_TIMEOUT.toSeconds() + " s");
    return 1;
  }

  private static int status(Options options) throws IOException {
    Optional<JsonNode> status = status(options.port());
    if (status.isEmpty()) {
      System.out.println("No evaluation server is running on port " + options.port());
      if (Files.deleteIfExists(options.stateFile())) {
        System.out.println("Removed the stale state file " + options.stateFile());
      }
      return 1;
    }
    for (Iterator<Map.Entry<String, JsonNode>> fields = status.get().fields(); fields.hasNext(); ) {
      Map.Entry<String, JsonNode> field = fields.next();
      System.out.println(field.getKey() + ": " + field.getValue().toString().replace("\"", ""));
    }
    return 0;
  }

  private static int stop(Options options) throws Exception {
    boolean asked = false;
    try {
      HttpResponse<String> response =
          HTTP.send(
              HttpRequest.newBuilder(EvaluationServer.uri(options.port(), "/shutdown"))
                  .timeout(Duration.ofSeconds(5))
                  .POST(HttpRequest.BodyPublishers.noBody())
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      asked = response.statusCode() == 200;
    } catch (IOException e) {
      // not answering: handled below
    }
    Optional<Long> pid = readPid(options.stateFile());
    if (asked) {
      Instant deadline = Instant.now().plus(Duration.ofMinutes(5));
      while (status(options.port()).isPresent() && Instant.now().isBefore(deadline)) {
        Thread.sleep(250);
      }
      pid.flatMap(ProcessHandle::of).ifPresent(handle -> handle.onExit().join());
      Files.deleteIfExists(options.stateFile());
      System.out.println("Evaluation server on port " + options.port() + " stopped");
      return 0;
    }
    if (options.force() && pid.isPresent()) {
      Optional<ProcessHandle> handle = ProcessHandle.of(pid.get());
      if (handle.isPresent()) {
        handle.get().destroyForcibly();
        handle.get().onExit().join();
        System.out.println("Killed the evaluation server process " + pid.get());
      }
      Files.deleteIfExists(options.stateFile());
      return 0;
    }
    System.out.println(
        "No evaluation server answers on port "
            + options.port()
            + (pid.isPresent() ? " (use --force to kill process " + pid.get() + ")" : ""));
    return 1;
  }

  /** What the server on a port evaluates, from its status. */
  private static String description(int port) {
    return status(port)
        .map(
            status ->
                "%s (%s encoding, space %s) on %s"
                    .formatted(
                        status.path("algorithm").asText(),
                        status.path("encoding").asText(),
                        status.path("parameterSpace").asText(),
                        status.path("problems").toString().replace("\"", "")))
        .orElse("");
  }

  /** The status of the server on a port, or empty if no server answers. */
  static Optional<JsonNode> status(int port) {
    try {
      HttpResponse<String> response =
          HTTP.send(
              HttpRequest.newBuilder(EvaluationServer.uri(port, "/status"))
                  .timeout(Duration.ofSeconds(5))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200
          ? Optional.of(JSON.readTree(response.body()))
          : Optional.empty();
    } catch (IOException e) {
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private static void writeState(Options options, long pid) throws IOException {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("pid", pid);
    state.put("port", options.port());
    state.put("startedAt", Instant.now().toString());
    state.put("baseLevel", options.baseLevel());
    state.put("threads", options.threads());
    state.put("log", options.log().toString());
    state.put("output", options.outputFile().toString());
    StringBuilder text = new StringBuilder();
    state.forEach((key, value) -> text.append(key).append(": ").append(value).append('\n'));
    Files.writeString(options.stateFile(), text.toString());
  }

  private static Optional<Long> readPid(Path stateFile) throws IOException {
    if (!Files.exists(stateFile)) {
      return Optional.empty();
    }
    return Files.readAllLines(stateFile).stream()
        .filter(line -> line.startsWith("pid: "))
        .map(line -> Long.parseLong(line.substring("pid: ".length()).trim()))
        .findFirst();
  }
}
