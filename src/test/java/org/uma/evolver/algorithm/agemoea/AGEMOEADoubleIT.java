package org.uma.evolver.algorithm.agemoea;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;

@DisplayName("Integration tests for class DoubleAGEMOEA")
class AGEMOEADoubleIT {

  @Tag("integration")
  @Test
  @DisplayName("DoubleAGEMOEA (agemoea2) should reach a minimum hypervolume on ZDT1 with standard settings")
  void shouldTheHypervolumeHaveAMinimumValueWhenSolvingZDT1WithAgeMoea2() {
    double hv = runAgeMoeaOnZdt1("agemoea2");
    double expectedHypervolume = 0.62;
    assertTrue(hv > expectedHypervolume, "Expected HV > " + expectedHypervolume + " but got " + hv);
  }

  @Tag("integration")
  @Test
  @DisplayName("DoubleAGEMOEA (agemoea original) should reach a minimum hypervolume on ZDT1 with standard settings")
  void shouldTheHypervolumeHaveAMinimumValueWhenSolvingZDT1WithOriginalAgeMoea() {
    double hv = runAgeMoeaOnZdt1("agemoea");
    double expectedHypervolume = 0.62;
    assertTrue(hv > expectedHypervolume, "Expected HV > " + expectedHypervolume + " but got " + hv);
  }

  private double runAgeMoeaOnZdt1(String variant) {
    var problem = new ZDT1();
    int populationSize = 100;
    int maximumNumberOfEvaluations = 20000;

    var agemoea =
        new DoubleAGEMOEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace("AGEMOEADouble.yaml", new DoubleParameterFactory()));

    var parameters =
        ("--agemoeaVariant " + variant + " "
                + "--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 100 "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split("\\s+");

    var algorithm = agemoea.parse(parameters).build();
    algorithm.run();

    List<DoubleSolution> population = algorithm.result();

    double[][] referenceFront = new double[][] {{0.0, 1.0}, {1.0, 0.0}};
    QualityIndicator hypervolume = new PISAHypervolume(referenceFront);
    return hypervolume.compute(SolutionListUtils.getMatrixWithObjectiveValues(population));
  }
}
