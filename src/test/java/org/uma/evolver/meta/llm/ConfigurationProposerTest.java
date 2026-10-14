package org.uma.evolver.meta.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;

@DisplayName("Unit tests for class ConfigurationProposer")
class ConfigurationProposerTest {

  /** A model that gives fixed answers, in order, and records the prompts it receives. */
  private static final class ScriptedClient implements LlmClient {
    private final Deque<String> answers;
    final List<String> prompts = new ArrayList<>();

    ScriptedClient(String... answers) {
      this.answers = new ArrayDeque<>(List.of(answers));
    }

    @Override
    public LlmResponse chat(List<LlmMessage> messages, String jsonSchema) {
      prompts.add(messages.get(messages.size() - 1).content());
      return new LlmResponse(answers.removeFirst(), "scripted", 10, 5, 1);
    }

    @Override
    public String model() {
      return "scripted";
    }
  }

  private static String defaultConfiguration() throws IOException {
    return new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt").getConfiguration(1).trim();
  }

  private static String answer(String... configurations) {
    StringBuilder json = new StringBuilder("{\"configurations\": [");
    for (int i = 0; i < configurations.length; i++) {
      json.append(i > 0 ? ", " : "")
          .append("{\"configuration\": \"")
          .append(configurations[i])
          .append("\", \"rationale\": \"r")
          .append(i)
          .append("\"}");
    }
    return json.append("]}").toString();
  }

  private static ConfigurationProposer proposer(LlmClient client, Path log) {
    YAMLParameterSpace space = BaseAlgorithmRegistry.resolveParameterSpace("Double", "NSGAIIDouble.yaml");
    return new ConfigurationProposer(client, space, "the space", log);
  }

  @Test
  @DisplayName("given valid and repeated proposals, when proposing, then the distinct valid ones are kept")
  void givenValidAndRepeatedProposals_whenProposing_thenDistinctValidOnesAreKept(@TempDir Path dir)
      throws IOException {
    // Arrange
    String valid = defaultConfiguration();
    var client = new ScriptedClient(answer(valid, valid));

    // Act
    var proposals = proposer(client, dir.resolve("LLM_CALLS.csv")).propose(2, "a task", List.of());

    // Assert
    assertEquals(1, proposals.size());
    assertEquals(1, client.prompts.size());
    assertTrue(client.prompts.get(0).contains("a task"));
    List<String> log = Files.readAllLines(dir.resolve("LLM_CALLS.csv"));
    assertEquals(3, log.size());
    assertTrue(log.get(2).contains("repeated"));
  }

  @Test
  @DisplayName("given an invalid proposal, when proposing, then it is sent back once with its error")
  void givenInvalidProposal_whenProposing_thenItIsRetriedWithTheError(@TempDir Path dir)
      throws IOException {
    // Arrange
    String valid = defaultConfiguration();
    var client = new ScriptedClient(answer("--crossover unknown"), answer(valid));

    // Act
    var proposals = proposer(client, dir.resolve("LLM_CALLS.csv")).propose(1, "a task", List.of());

    // Assert
    assertEquals(1, proposals.size());
    assertEquals(2, client.prompts.size());
    assertTrue(client.prompts.get(1).contains("--crossover unknown"));
  }

  @Test
  @DisplayName("given an answer that is not JSON, when proposing, then nothing is kept")
  void givenAnswerThatIsNotJson_whenProposing_thenNothingIsKept() throws IOException {
    // Arrange
    var client = new ScriptedClient("not json");

    // Act
    var proposals = proposer(client, null).propose(3, "a task", List.of());

    // Assert
    assertTrue(proposals.isEmpty());
  }
}
