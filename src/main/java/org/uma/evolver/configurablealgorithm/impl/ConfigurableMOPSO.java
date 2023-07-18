package org.uma.evolver.configurablealgorithm.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestInitializationParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestSelectionParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestUpdateParameter;
import org.uma.evolver.parameter.catalogue.InertiaWeightComputingParameter;
import org.uma.evolver.parameter.catalogue.LocalBestInitializationParameter;
import org.uma.evolver.parameter.catalogue.LocalBestUpdateParameter;
import org.uma.evolver.parameter.catalogue.MutationParameter;
import org.uma.evolver.parameter.catalogue.PerturbationParameter;
import org.uma.evolver.parameter.catalogue.PositionUpdateParameter;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.catalogue.VelocityInitializationParameter;
import org.uma.evolver.parameter.catalogue.VelocityUpdateParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.evolver.parameter.impl.IntegerParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.pso.globalbestinitialization.GlobalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.globalbestselection.GlobalBestSelection;
import org.uma.jmetal.component.catalogue.pso.globalbestupdate.GlobalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.InertiaWeightComputingStrategy;
import org.uma.jmetal.component.catalogue.pso.localbestinitialization.LocalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.localbestupdate.LocalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.perturbation.Perturbation;
import org.uma.jmetal.component.catalogue.pso.positionupdate.PositionUpdate;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.VelocityInitialization;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.VelocityUpdate;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.archive.impl.BestSolutionsArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

/**
 * Class to configure a generic MOPSO with an argument string using class
 * {@link ParticleSwarmOptimizationAlgorithm}
 *
 * @autor Antonio J. Nebro
 */
public class ConfigurableMOPSO implements ConfigurableAlgorithmBuilder {
  private List<Parameter<?>> configurableParameterList = new ArrayList<>();
  public ExternalArchiveParameter<DoubleSolution> leaderArchiveParameter;
  private CategoricalParameter algorithmResultParameter;
  public VelocityInitializationParameter velocityInitializationParameter;
  private IntegerParameter swarmSizeParameter;
  private CreateInitialSolutionsParameter swarmInitializationParameter;
  private LocalBestInitializationParameter localBestInitializationParameter;
  private GlobalBestInitializationParameter globalBestInitializationParameter;
  private GlobalBestSelectionParameter globalBestSelectionParameter;
  private PerturbationParameter perturbationParameter;
  private PositionUpdateParameter positionUpdateParameter;
  private GlobalBestUpdateParameter globalBestUpdateParameter;
  private LocalBestUpdateParameter localBestUpdateParameter;
  private VelocityUpdateParameter velocityUpdateParameter;
  private RealParameter c1MinParameter;
  private RealParameter c1MaxParameter;
  private RealParameter c2MinParameter;
  private RealParameter c2MaxParameter;
  private RealParameter wMinParameter;
  private RealParameter wMaxParameter;
  private RealParameter weightParameter;
  private MutationParameter mutationParameter;
  private InertiaWeightComputingParameter inertiaWeightComputingParameter;

  @Override
  public List<Parameter<?>> configurableParameterList() {
    return configurableParameterList;
  }
  private int archiveSize ;
  private int maximumNumberOfEvaluations ;

  private DoubleProblem problem ;

  public ConfigurableMOPSO() {
    this.configure() ;
  }

  public ConfigurableMOPSO(DoubleProblem problem, int archiveSize, int maximumNumberOfEvaluations) {
    this.problem = problem ;
    this.archiveSize = archiveSize ;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations ;
    this.configure();
  }

  @Override
  public ConfigurableAlgorithmBuilder createBuilderInstance() {
    return new ConfigurableMOPSO(problem, archiveSize, maximumNumberOfEvaluations) ;
  }

  public ConfigurableAlgorithmBuilder createBuilderInstance(DoubleProblem problem, int maximumNumberOfEvaluations) {
    return new ConfigurableMOPSO(problem, archiveSize, maximumNumberOfEvaluations) ;
  }

  @Override
  public ConfigurableAlgorithmBuilder parse(String[] arguments) {
    for (Parameter<?> parameter : configurableParameterList()) {
      parameter.parse(arguments).check();
    }

    return this ;
  }

  private void configure() {
    algorithmResultParameter =
        new CategoricalParameter("algorithmResult",
            List.of("leaderArchive"));
            //List.of("unboundedArchive", "leaderArchive"));

    swarmSizeParameter = new IntegerParameter("swarmSize", 10, 200);

    swarmInitializationParameter =
        new CreateInitialSolutionsParameter("swarmInitialization",
            Arrays.asList("random", "latinHypercubeSampling", "scatterSearch"));

    velocityInitializationParameter = new VelocityInitializationParameter(
        List.of("defaultVelocityInitialization",
            "SPSO2007VelocityInitialization", "SPSO2011VelocityInitialization"));

    velocityUpdateParameter = configureVelocityUpdate();

    localBestInitializationParameter = new LocalBestInitializationParameter(
        List.of("defaultLocalBestInitialization"));
    localBestUpdateParameter = new LocalBestUpdateParameter(
        List.of("defaultLocalBestUpdate"));
    globalBestInitializationParameter = new GlobalBestInitializationParameter(
        List.of("defaultGlobalBestInitialization"));

    globalBestSelectionParameter = new GlobalBestSelectionParameter(
        List.of("tournament", "random"));
    IntegerParameter selectionTournamentSize =
        new IntegerParameter("selectionTournamentSize", 2, 10);
    globalBestSelectionParameter.addSpecificParameter("tournament",selectionTournamentSize);

    globalBestUpdateParameter = new GlobalBestUpdateParameter(
        List.of("defaultGlobalBestUpdate"));

    positionUpdateParameter = new PositionUpdateParameter(
        List.of("defaultPositionUpdate"));
    var velocityChangeWhenLowerLimitIsReachedParameter = new RealParameter(
        "velocityChangeWhenLowerLimitIsReached", -1.0, 1.0);
    var velocityChangeWhenUpperLimitIsReachedParameter = new RealParameter(
        "velocityChangeWhenUpperLimitIsReached", -1.0, 1.0);
    positionUpdateParameter.addSpecificParameter("defaultPositionUpdate",
        velocityChangeWhenLowerLimitIsReachedParameter);
    positionUpdateParameter.addSpecificParameter("defaultPositionUpdate",
        velocityChangeWhenUpperLimitIsReachedParameter);

    perturbationParameter = configurePerturbation();

    leaderArchiveParameter = new ExternalArchiveParameter<>("leaderArchive",
        List.of("crowdingDistanceArchive", "hypervolumeArchive", "spatialSpreadDeviationArchive"));

    inertiaWeightComputingParameter = new InertiaWeightComputingParameter(
        List.of("constantValue", "randomSelectedValue", "linearIncreasingValue",
            "linearDecreasingValue"));

    weightParameter = new RealParameter("weight", 0.1, 1.0);
    wMinParameter = new RealParameter("weightMin", 0.1, 0.5);
    wMaxParameter = new RealParameter("weightMax", 0.5, 1.0);
    inertiaWeightComputingParameter.addSpecificParameter("constantValue", weightParameter);
    inertiaWeightComputingParameter.addSpecificParameter("randomSelectedValue", wMinParameter);
    inertiaWeightComputingParameter.addSpecificParameter("randomSelectedValue", wMaxParameter);
    inertiaWeightComputingParameter.addSpecificParameter("linearIncreasingValue", wMinParameter);
    inertiaWeightComputingParameter.addSpecificParameter("linearIncreasingValue", wMaxParameter);
    inertiaWeightComputingParameter.addSpecificParameter("linearDecreasingValue", wMinParameter);
    inertiaWeightComputingParameter.addSpecificParameter("linearDecreasingValue", wMaxParameter);

    configurableParameterList.add(swarmSizeParameter);
    configurableParameterList.add(leaderArchiveParameter);
    configurableParameterList.add(algorithmResultParameter);
    configurableParameterList.add(swarmInitializationParameter);
    configurableParameterList.add(velocityInitializationParameter);
    configurableParameterList.add(perturbationParameter);
    configurableParameterList.add(inertiaWeightComputingParameter);
    configurableParameterList.add(velocityUpdateParameter);
    configurableParameterList.add(localBestInitializationParameter);
    configurableParameterList.add(globalBestInitializationParameter);
    configurableParameterList.add(globalBestSelectionParameter);
    configurableParameterList.add(globalBestUpdateParameter);
    configurableParameterList.add(localBestUpdateParameter);
    configurableParameterList.add(positionUpdateParameter);
  }

  private PerturbationParameter configurePerturbation() {
    mutationParameter =
        new MutationParameter(
            Arrays.asList("uniform", "polynomial", "nonUniform", "linkedPolynomial"));
    //ProbabilityParameter mutationProbability =
    //    new ProbabilityParameter("mutationProbability", args);
    RealParameter mutationProbabilityFactor = new RealParameter("mutationProbabilityFactor", 0.0,
        2.0);
    mutationParameter.addGlobalParameter(mutationProbabilityFactor);
    RepairDoubleSolutionStrategyParameter mutationRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "mutationRepairStrategy", Arrays.asList("random", "round", "bounds"));
    mutationParameter.addGlobalParameter(mutationRepairStrategy);

    RealParameter distributionIndexForPolynomialMutation =
        new RealParameter("polynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("polynomial", distributionIndexForPolynomialMutation);

    RealParameter distributionIndexForLinkedPolynomialMutation =
        new RealParameter("linkedPolynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("linkedPolynomial",
        distributionIndexForLinkedPolynomialMutation);
    RealParameter uniformMutationPerturbation =
        new RealParameter("uniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("uniform", uniformMutationPerturbation);

    RealParameter nonUniformMutationPerturbation =
        new RealParameter("nonUniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("nonUniform", nonUniformMutationPerturbation);

    // TODO: the upper bound  must be the swarm size
    IntegerParameter frequencyOfApplicationParameter = new IntegerParameter(
        "frequencyOfApplicationOfMutationOperator", 1, 10);

    perturbationParameter = new PerturbationParameter(
        List.of("frequencySelectionMutationBasedPerturbation"));
    perturbationParameter.addSpecificParameter("frequencySelectionMutationBasedPerturbation",
        mutationParameter);
    perturbationParameter.addSpecificParameter("frequencySelectionMutationBasedPerturbation",
        frequencyOfApplicationParameter);

    return perturbationParameter;
  }

  private VelocityUpdateParameter configureVelocityUpdate() {
    c1MinParameter = new RealParameter("c1Min", 1.0, 2.0);
    c1MaxParameter = new RealParameter("c1Max", 2.0, 3.0);
    c2MinParameter = new RealParameter("c2Min", 1.0, 2.0);
    c2MaxParameter = new RealParameter("c2Max", 2.0, 3.0);

    velocityUpdateParameter = new VelocityUpdateParameter(
        List.of("defaultVelocityUpdate", "constrainedVelocityUpdate", "SPSO2011VelocityUpdate"));
    velocityUpdateParameter.addGlobalParameter(c1MinParameter);
    velocityUpdateParameter.addGlobalParameter(c1MaxParameter);
    velocityUpdateParameter.addGlobalParameter(c2MinParameter);
    velocityUpdateParameter.addGlobalParameter(c2MaxParameter);

    return velocityUpdateParameter;
  }

  /**
   * Create an instance of MOPSO from the parsed parameters
   */
  public ParticleSwarmOptimizationAlgorithm build() {
    int swarmSize = swarmSizeParameter.value();

    Evaluation<DoubleSolution> evaluation;
    Archive<DoubleSolution> unboundedArchive = null;
    if (algorithmResultParameter.value().equals("unboundedArchive")) {
      unboundedArchive = new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(),
          swarmSize);
      evaluation = new SequentialEvaluationWithArchive<>(problem, unboundedArchive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }

    var termination = new TerminationByEvaluations(maximumNumberOfEvaluations);

    leaderArchiveParameter.setSize(archiveSize);
    BoundedArchive<DoubleSolution> leaderArchive = (BoundedArchive<DoubleSolution>) leaderArchiveParameter.getParameter();

    var velocityInitialization = velocityInitializationParameter.getParameter();

    if (velocityUpdateParameter.value().equals("constrainedVelocityUpdate") ||
        velocityUpdateParameter.value().equals("SPSO2011VelocityUpdate")) {
      velocityUpdateParameter.addNonConfigurableParameter("problem", problem);
    }

    if ((inertiaWeightComputingParameter.value().equals("linearIncreasingValue") ||
        inertiaWeightComputingParameter.value().equals("linearDecreasingValue"))) {
      inertiaWeightComputingParameter.addNonConfigurableParameter("maxIterations",
          maximumNumberOfEvaluations / swarmSizeParameter.value());
      inertiaWeightComputingParameter.addNonConfigurableParameter("swarmSize",
          swarmSizeParameter.value());
    }
    InertiaWeightComputingStrategy inertiaWeightComputingStrategy = inertiaWeightComputingParameter.getParameter();

    var velocityUpdate = velocityUpdateParameter.getParameter();

    LocalBestInitialization localBestInitialization = localBestInitializationParameter.getParameter();
    GlobalBestInitialization globalBestInitialization = globalBestInitializationParameter.getParameter();
    GlobalBestSelection globalBestSelection = globalBestSelectionParameter.getParameter(
        leaderArchive.comparator());

    mutationParameter.addNonConfigurableParameter("numberOfProblemVariables",
        problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableParameter("nonUniformMutationPerturbation", maximumNumberOfEvaluations);
      mutationParameter.addNonConfigurableParameter("maxIterations",
          maximumNumberOfEvaluations / swarmSizeParameter.value());
    }

    var perturbation = perturbationParameter.getParameter();

    if (positionUpdateParameter.value().equals("defaultPositionUpdate")) {
      positionUpdateParameter.addNonConfigurableParameter("positionBounds",
          problem.variableBounds());
    }

    PositionUpdate positionUpdate = positionUpdateParameter.getParameter();

    GlobalBestUpdate globalBestUpdate = globalBestUpdateParameter.getParameter();
    LocalBestUpdate localBestUpdate = localBestUpdateParameter.getParameter(
        new DefaultDominanceComparator<DoubleSolution>());

    SolutionsCreation<DoubleSolution> swarmInitialization =
        (SolutionsCreation<DoubleSolution>) swarmInitializationParameter.getParameter(
            problem,
            swarmSizeParameter.value());

    class ParticleSwarmOptimizationAlgorithmWithArchive extends ParticleSwarmOptimizationAlgorithm {

      private Archive<DoubleSolution> unboundedArchive;

      /**
       * Constructor
       */
      public ParticleSwarmOptimizationAlgorithmWithArchive(String name,
          SolutionsCreation<DoubleSolution> createInitialSwarm,
          Evaluation<DoubleSolution> evaluation,
          Termination termination,
          VelocityInitialization velocityInitialization,
          LocalBestInitialization localBestInitialization,
          GlobalBestInitialization globalBestInitialization,
          InertiaWeightComputingStrategy inertiaWeightComputingStrategy,
          VelocityUpdate velocityUpdate,
          PositionUpdate positionUpdate,
          Perturbation perturbation,
          GlobalBestUpdate globalBestUpdate,
          LocalBestUpdate localBestUpdate,
          GlobalBestSelection globalBestSelection,
          BoundedArchive<DoubleSolution> leaderArchive,
          Archive<DoubleSolution> unboundedArchive) {
        super(name,
            createInitialSwarm,
            evaluation,
            termination,
            velocityInitialization,
            localBestInitialization,
            globalBestInitialization,
            inertiaWeightComputingStrategy,
            velocityUpdate,
            positionUpdate,
            perturbation,
            globalBestUpdate,
            localBestUpdate,
            globalBestSelection,
            leaderArchive);
        this.unboundedArchive = unboundedArchive;
      }

      @Override
      public List<DoubleSolution> result() {
        return unboundedArchive.solutions();
      }
    }

    if (algorithmResultParameter.value().equals("unboundedArchive")) {
      return new ParticleSwarmOptimizationAlgorithmWithArchive("MOPSO",
          swarmInitialization,
          evaluation,
          termination,
          velocityInitialization,
          localBestInitialization,
          globalBestInitialization,
          inertiaWeightComputingStrategy,
          velocityUpdate,
          positionUpdate,
          perturbation,
          globalBestUpdate,
          localBestUpdate,
          globalBestSelection,
          leaderArchive,
          unboundedArchive);
    } else {
      return new ParticleSwarmOptimizationAlgorithm("MOPSO",
          swarmInitialization,
          evaluation,
          termination,
          velocityInitialization,
          localBestInitialization,
          globalBestInitialization,
          inertiaWeightComputingStrategy,
          velocityUpdate,
          positionUpdate,
          perturbation,
          globalBestUpdate,
          localBestUpdate,
          globalBestSelection,
          leaderArchive);
    }
  }

  public static void print(List<Parameter<?>> parameterList) {
    parameterList.forEach(System.out::println);
  }
}
