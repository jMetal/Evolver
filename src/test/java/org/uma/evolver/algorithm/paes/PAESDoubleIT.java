package org.uma.evolver.algorithm.paes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;

@DisplayName("Integration tests for class DoublePAES")
class PAESDoubleIT {

  private EvolutionaryAlgorithm<DoubleSolution> buildAndRun(String[] args, int maxEvals) {
    var paes =
        new DoublePAES(
            new ZDT1(),
            100,
            maxEvals,
            new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));
    paes.parse(args);
    var algorithm = paes.build();
    algorithm.run();
    return algorithm;
  }

  @Test
  @Tag("integration")
  @DisplayName("given default config with crowding distance archive when running on ZDT1 then HV exceeds threshold")
  void givenDefaultConfig_whenRunningOnZDT1_thenHVExceedsThreshold() {
    // Arrange
    String[] args = ("--paesArchiveType crowdingDistanceArchive "
        + "--algorithmResult paesArchive "
        + "--archiveSelectionProbability 0.0 "
        + "--createInitialSolutions default "
        + "--mutation polynomial "
        + "--mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds "
        + "--polynomialMutationDistributionIndex 20.0").split("\\s+");
    int maxEvals = 25000;

    // Act
    EvolutionaryAlgorithm<DoubleSolution> algorithm = buildAndRun(args, maxEvals);
    List<DoubleSolution> result = algorithm.result();

    // Assert
    double[][] referenceFront = {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    double hv = hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(result));
    assertTrue(hv > 0.55,
        "Expected HV > 0.55 but got: " + hv);
  }
}
