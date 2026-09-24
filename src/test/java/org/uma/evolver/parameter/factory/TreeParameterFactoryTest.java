package org.uma.evolver.parameter.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsTreeParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.TreeCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.TreeMutationParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.TreeVariationParameter;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class TreeParameterFactory")
class TreeParameterFactoryTest {

  private static final String[] NSGAII_META_TREE_FLAGS = {
    "--algorithmResult", "population",
    "--createInitialSolutions", "default",
    "--variation", "crossoverAndMutationVariation",
    "--offspringPopulationSize", "10",
    "--crossover", "subtree",
    "--crossoverProbability", "0.9",
    "--mutation", "tree",
    "--mutationProbability", "1.0",
    "--mutationDistributionIndex", "20.0",
    "--selection", "tournament",
    "--selectionTournamentSize", "2"
  };

  private static ParameterSpace parsedNSGAIIMetaTreeSpace() {
    ParameterSpace parameterSpace =
        new YAMLParameterSpace("NSGAIIMetaTree.yaml", new TreeParameterFactory());
    for (Parameter<?> parameter : parameterSpace.topLevelParameters()) {
      parameter.parse(NSGAII_META_TREE_FLAGS);
    }
    return parameterSpace;
  }

  private static TreeSolutionGenerator baseLevelTreeSolutionGenerator() {
    return new TreeSolutionGenerator(
        new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
  }

  @Nested
  @DisplayName("When creating a parameter by name: ")
  class CreateParameterTestCases {

    @Test
    @DisplayName(
        "given the operator parameter names, when createParameter is called, then the"
            + " tree-specific parameters are returned")
    void givenOperatorParameterNames_whenCreateParameterCalled_thenTreeParametersAreReturned() {
      // Arrange
      var factory = new TreeParameterFactory();

      // Act & Assert
      assertInstanceOf(
          CreateInitialSolutionsTreeParameter.class,
          factory.createParameter("createInitialSolutions", List.of("default")));
      assertInstanceOf(
          TreeCrossoverParameter.class, factory.createParameter("crossover", List.of("subtree")));
      assertInstanceOf(
          TreeMutationParameter.class, factory.createParameter("mutation", List.of("tree")));
      assertInstanceOf(
          TreeVariationParameter.class,
          factory.createParameter("variation", List.of("crossoverAndMutationVariation")));
    }

    @Test
    @DisplayName(
        "given a non-tree crossover name, when createParameter is called, then it fails naming"
            + " the supported operator")
    void givenNonTreeCrossover_whenCreateParameterCalled_thenItFails() {
      // Arrange
      var factory = new TreeParameterFactory();

      // Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> factory.createParameter("crossover", List.of("SBX")));
      assertEquals(true, exception.getMessage().contains("subtree"));
    }
  }

  @Nested
  @DisplayName("When building operators from NSGAIIMetaTree.yaml: ")
  class OperatorTestCases {

    @Test
    @DisplayName(
        "given a parsed space and a tree solution generator, when the operators are built, then"
            + " they are subtree crossover and tree mutation")
    void givenParsedSpaceAndGenerator_whenOperatorsBuilt_thenTheyAreTreeOperators() {
      // Arrange
      ParameterSpace parameterSpace = parsedNSGAIIMetaTreeSpace();
      var crossoverParameter = (TreeCrossoverParameter) parameterSpace.get("crossover");
      var mutationParameter = (TreeMutationParameter) parameterSpace.get("mutation");
      mutationParameter.addNonConfigurableSubParameter(
          "treeSolutionGenerator", baseLevelTreeSolutionGenerator());

      // Act & Assert
      assertInstanceOf(SubtreeCrossover.class, crossoverParameter.getCrossover());
      assertInstanceOf(TreeMutation.class, mutationParameter.getMutation());
    }

    @Test
    @DisplayName(
        "given a parsed space, a generator and an offspring size, when the variation is built,"
            + " then it is a crossover-and-mutation variation of that offspring size")
    void givenParsedSpaceAndGenerator_whenVariationBuilt_thenItHasTheOffspringSize() {
      // Arrange
      ParameterSpace parameterSpace = parsedNSGAIIMetaTreeSpace();
      var variationParameter = (TreeVariationParameter) parameterSpace.get("variation");
      variationParameter.addNonConfigurableSubParameter("offspringPopulationSize", 10);
      parameterSpace
          .get("mutation")
          .addNonConfigurableSubParameter("treeSolutionGenerator", baseLevelTreeSolutionGenerator());

      // Act
      var variation = variationParameter.getVariation();

      // Assert
      assertInstanceOf(CrossoverAndMutationVariation.class, variation);
      assertEquals(10, ((CrossoverAndMutationVariation<?>) variation).offspringPopulationSize());
    }

    @Test
    @DisplayName(
        "given a parsed space without a tree solution generator, when the mutation is built, then"
            + " it fails")
    void givenParsedSpaceWithoutGenerator_whenMutationBuilt_thenItFails() {
      // Arrange
      ParameterSpace parameterSpace = parsedNSGAIIMetaTreeSpace();
      var mutationParameter = (TreeMutationParameter) parameterSpace.get("mutation");

      // Act & Assert
      assertThrows(JMetalException.class, mutationParameter::getMutation);
    }
  }
}
