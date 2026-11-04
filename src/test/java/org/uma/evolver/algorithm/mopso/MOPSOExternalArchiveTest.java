package org.uma.evolver.algorithm.mopso;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for MOPSO with an external archive")
class MOPSOExternalArchiveTest {

  private static final String CONFIGURATION =
      "--swarmSize 100 --algorithmResult externalArchive --swarmSizeWithArchive 50"
          + " --externalArchiveType unboundedArchive --leaderArchive crowdingDistanceArchive"
          + " --swarmInitialization default"
          + " --velocityInitialization defaultVelocityInitialization"
          + " --velocityUpdate constrainedVelocityUpdate --c1Min 1.5 --c1Max 2.5 --c2Min 1.5"
          + " --c2Max 2.5 --globalBestInitialization defaultGlobalBestInitialization"
          + " --globalBestUpdate defaultGlobalBestUpdate"
          + " --positionUpdate defaultPositionUpdate"
          + " --globalBestSelection tournamentSelection --selectionTournamentSize 2"
          + " --perturbation frequencySelectionMutationBasedPerturbation"
          + " --frequencyOfApplicationOfMutationOperator 7 --mutation polynomial"
          + " --polynomialMutationDistributionIndex 20.0 --mutationProbabilityFactor 1.0"
          + " --mutationRepairStrategy bounds"
          + " --inertiaWeightComputingStrategy randomSelectedValue"
          + " --randomInertiaWeightMin 0.1 --randomInertiaWeightMax 0.5"
          + " --velocityChangeWhenLowerLimitIsReached -1.0"
          + " --velocityChangeWhenUpperLimitIsReached -1.0"
          + " --localBestInitialization defaultLocalBestInitialization"
          + " --localBestUpdate defaultLocalBestUpdate";

  @Test
  @DisplayName(
      "Given MOPSO.yaml, when MOPSO runs with an external archive, then it returns solutions")
  void givenTheBundledSpace_whenRunWithAnExternalArchive_thenItReturnsSolutions() {
    // Arrange
    JMetalRandom.getInstance().setSeed(1);
    var mopso =
        new BaseMOPSO(
            new ZDT1(),
            100,
            2000,
            new YAMLParameterSpace("MOPSO.yaml", new MOPSOParameterFactory()));
    mopso.parse(CONFIGURATION.split("\\s+"));

    // Act
    var algorithm = mopso.build();
    algorithm.run();

    // Assert
    assertTrue(!algorithm.result().isEmpty());
  }
}
