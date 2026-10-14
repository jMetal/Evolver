package org.uma.evolver.meta.llm;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.uma.evolver.meta.encoding.util.ConfigurationEncoding;
import org.uma.evolver.parameter.ParameterSpace;

/**
 * Asks a language model for configurations of a parameter space, and keeps only the valid ones.
 *
 * <p>The prompt gives the model the parameter space (its YAML text), a description of the task
 * (the algorithm, the problems, the budget) and, optionally, example configurations; the answer
 * is forced to a JSON list of configurations in Evolver's {@code --name value} format, each with a
 * short rationale. Each configuration is checked against the space by building its derivation tree
 * ({@link ConfigurationEncoding#toTree}), which also rewrites it with only the parameters it
 * activates; the invalid ones are sent back once, with the error of each, to be corrected.
 *
 * <p>Every call is appended to a CSV log (time, model, tokens, milliseconds, and each proposal with
 * whether it was valid, its error and its rationale): language models are not deterministic, so a
 * study can only be audited from its log.
 *
 * <p>The proposals are a starting point, not a result: they can seed a meta-optimizer
 * ({@code initialConfigurations}) or be validated, and whether they help is a question to study.
 */
public final class ConfigurationProposer {

  /** The schema of the answer: a list of configurations with their rationale. */
  static final String ANSWER_SCHEMA =
      """
      {"type": "object",
       "properties": {"configurations": {"type": "array", "items": {"type": "object",
         "properties": {"configuration": {"type": "string"}, "rationale": {"type": "string"}},
         "required": ["configuration", "rationale"], "additionalProperties": false}}},
       "required": ["configurations"], "additionalProperties": false}""";

  private static final String SYSTEM_PROMPT =
      """
      You configure multi-objective metaheuristics of the Evolver framework. A configuration is a \
      single line of "--name value" pairs, one for each parameter that the configuration activates. \
      In the parameter space (YAML), every top-level parameter is always active; the \
      globalSubParameters of a parameter are active whenever that parameter is active, whatever its \
      value; and the conditionalParameters of a value are active only when that value is chosen. \
      Give a value to every active parameter and to no other. Use only names and values of the \
      parameter space, with numbers inside their ranges. Answer with JSON only.""";

  /** A valid configuration proposed by the model. */
  public record Proposal(String configuration, String rationale) {}

  private final LlmClient client;
  private final ParameterSpace parameterSpace;
  private final String parameterSpaceText;
  private final Path logFile;

  /**
   * @param client the model
   * @param parameterSpace the parameter space, to check the proposals
   * @param parameterSpaceText its YAML text, given to the model
   * @param logFile the CSV log of the calls (appended), or null for no log
   */
  public ConfigurationProposer(
      LlmClient client, ParameterSpace parameterSpace, String parameterSpaceText, Path logFile) {
    this.client = client;
    this.parameterSpace = parameterSpace;
    this.parameterSpaceText = parameterSpaceText;
    this.logFile = logFile;
  }

  /**
   * Asks for configurations.
   *
   * @param count how many configurations to ask for
   * @param task what they are for: the algorithm, the problems, the budget...
   * @param examples example configurations (may be empty)
   * @return the valid, distinct configurations, at most {@code count}
   * @throws IOException if the model cannot be reached or the log cannot be written
   */
  public List<Proposal> propose(int count, String task, List<String> examples) throws IOException {
    List<Proposal> valid = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    List<String> errors = new ArrayList<>();
    collect(ask(firstPrompt(count, task, examples), 1), valid, seen, errors);
    if (!errors.isEmpty() && valid.size() < count) {
      collect(ask(retryPrompt(count - valid.size(), task, errors), 2), valid, seen, new ArrayList<>());
    }
    return valid.size() > count ? valid.subList(0, count) : valid;
  }

  private String firstPrompt(int count, String task, List<String> examples) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Task: ").append(task).append("\n\nParameter space (YAML):\n").append(parameterSpaceText);
    if (!examples.isEmpty()) {
      prompt.append("\n\nExample configurations:\n");
      examples.forEach(example -> prompt.append(example).append('\n'));
    }
    prompt
        .append("\nPropose ")
        .append(count)
        .append(" different configurations that you expect to perform well for this task, each")
        .append(" with a one-sentence rationale.");
    return prompt.toString();
  }

  private String retryPrompt(int count, String task, List<String> errors) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Task: ").append(task).append("\n\nParameter space (YAML):\n").append(parameterSpaceText);
    prompt.append("\n\nThese configurations you proposed are not valid:\n");
    errors.forEach(error -> prompt.append("- ").append(error).append('\n'));
    prompt
        .append("\nPropose ")
        .append(count)
        .append(" valid configurations instead, correcting those errors, each with a rationale.");
    return prompt.toString();
  }

  private record Call(LlmResponse response, int round) {}

  private Call ask(String prompt, int round) throws IOException {
    LlmResponse response =
        client.chat(List.of(LlmMessage.system(SYSTEM_PROMPT), LlmMessage.user(prompt)), ANSWER_SCHEMA);
    return new Call(response, round);
  }

  private void collect(Call call, List<Proposal> valid, Set<String> seen, List<String> errors)
      throws IOException {
    List<String[]> rows = new ArrayList<>();
    JsonNode answer;
    try {
      answer = HttpJsonClient.JSON.readTree(call.response().content());
    } catch (IOException e) {
      log(call, List.<String[]>of(new String[] {"", "false", "the answer is not JSON", ""}));
      return;
    }
    for (JsonNode item : answer.path("configurations")) {
      String configuration = item.path("configuration").asText().trim();
      String rationale = item.path("rationale").asText();
      try {
        String canonical =
            String.join(
                " ",
                ConfigurationEncoding.toTree(configuration.split("\\s+"), parameterSpace, 2)
                    .toParameterArray());
        boolean isNew = seen.add(canonical);
        if (isNew) {
          valid.add(new Proposal(canonical, rationale));
        }
        rows.add(new String[] {canonical, "true", isNew ? "" : "repeated", rationale});
      } catch (RuntimeException e) {
        errors.add(configuration + " -> " + e.getMessage());
        rows.add(new String[] {configuration, "false", e.getMessage(), rationale});
      }
    }
    log(call, rows);
  }

  private void log(Call call, List<String[]> rows) throws IOException {
    if (logFile == null) {
      return;
    }
    Path logDirectory = logFile.getParent();
    if (logDirectory != null) {
      Files.createDirectories(logDirectory);
    }
    StringBuilder text = new StringBuilder();
    if (!Files.exists(logFile)) {
      text.append(
          "Time,Model,Round,PromptTokens,CompletionTokens,Millis,Valid,Configuration,Error,Rationale\n");
    }
    LlmResponse response = call.response();
    for (String[] row : rows) {
      text.append(Instant.now())
          .append(',')
          .append(csv(response.model()))
          .append(',')
          .append(call.round())
          .append(',')
          .append(response.promptTokens())
          .append(',')
          .append(response.completionTokens())
          .append(',')
          .append(response.millis())
          .append(',')
          .append(row[1])
          .append(',')
          .append(csv(row[0]))
          .append(',')
          .append(csv(row[2]))
          .append(',')
          .append(csv(row[3]))
          .append('\n');
    }
    Files.writeString(logFile, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  private static String csv(String value) {
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
