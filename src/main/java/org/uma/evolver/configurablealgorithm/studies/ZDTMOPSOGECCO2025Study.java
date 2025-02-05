package org.uma.evolver.configurablealgorithm.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.studies.DTLZStudy;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Example of experimental study based on solving the ZDT problems with the algorithms NSGAII,
 * MOEA/D, and SMPSO.
 *
 * <p>This experiment assumes that the reference Pareto front are known and that, given a problem
 * named P, there is a corresponding file called P.csv containing its corresponding Pareto front. If
 * this is not the case, please refer to class {@link DTLZStudy} to see an example of how to
 * explicitly indicate the name of those files.
 *
 * <p>Five quality indicators are used for performance assessment: {@link Epsilon}, {@link Spread},
 * {@link GenerationalDistance}, {@link PISAHypervolume}, and {@link
 * InvertedGenerationalDistancePlus}.
 *
 * <p>The steps to carry out are: 1. Configure the experiment 2. Execute the algorithms 3. Compute
 * que quality indicators 4. Generate Latex tables reporting means and medians, and tables with
 * statistical tests 5. Generate HTML pages with tables, boxplots, and fronts.
 *
 * @author Antonio J. Nebro
 */
public class ZDTMOPSOGECCO2025Study {
  private static final int INDEPENDENT_RUNS = 30;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList =
        List.of(
            new ExperimentProblem<>(new ZDT1()),
            new ExperimentProblem<>(new ZDT2()),
            new ExperimentProblem<>(new ZDT3()),
            new ExperimentProblem<>(new ZDT4()),
            new ExperimentProblem<>(new ZDT6()));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ZDTMOPSOGECCO2025Study")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                List.of(
                    new Epsilon(),
                    new PISAHypervolume(),
                    new NormalizedHypervolume(),
                    new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(8)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateFriedmanHolmTestTables<>(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(2).run();
    // new GenerateHtmlPages<>(experiment, StudyVisualizer.TYPE_OF_FRONT_TO_SHOW.MEDIAN).run() ;
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (var experimentProblem : problemList) {
        smpso(algorithms, run, experimentProblem);
        mopso500(algorithms, run, experimentProblem);
        mopso1000(algorithms, run, experimentProblem);
        mopso2000(algorithms, run, experimentProblem);
        mopso3000(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void smpso(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--algorithmResult leaderArchive "
                + "--swarmSize 100 "
                + "--archiveSize 100 "
                + "--swarmInitialization random "
                + "--velocityInitialization defaultVelocityInitialization "
                + "--leaderArchive crowdingDistanceArchive "
                + "--localBestInitialization defaultLocalBestInitialization "
                + "--globalBestInitialization defaultGlobalBestInitialization "
                + "--globalBestSelection tournament "
                + "--selectionTournamentSize 2 "
                + "--perturbation frequencySelectionMutationBasedPerturbation "
                + "--frequencyOfApplicationOfMutationOperator 7 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--positionUpdate defaultPositionUpdate "
                + "--velocityChangeWhenLowerLimitIsReached -1.0 "
                + "--velocityChangeWhenUpperLimitIsReached -1.0 "
                + "--globalBestUpdate defaultGlobalBestUpdate "
                + "--localBestUpdate defaultLocalBestUpdate "
                + "--velocityUpdate constrainedVelocityUpdate "
                + "--inertiaWeightComputingStrategy randomSelectedValue "
                + "--c1Min 1.5 "
                + "--c1Max 2.5 "
                + "--c2Min 1.5 "
                + "--c2Max 2.5 "
                + "--weightMin 0.1 "
                + "--weightMax 0.5 ")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "SMPSO", experimentProblem, run));
  }

  private static void mopso500(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--swarmSize 25 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2007VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 1.4340057641399753 --mutationRepairStrategy round --polynomialMutationDistributionIndex 93.99310146679427 --linkedPolynomialMutationDistributionIndex 197.97568800767945 --uniformMutationPerturbation 0.4694337805205716 --nonUniformMutationPerturbation 0.23043405376841963 --frequencyOfApplicationOfMutationOperator 6 --inertiaWeightComputingStrategy constantValue --weight 0.34930081315041916 --weightMin 0.40499358424621035 --weightMax 0.7379587766602353 --weightMin 0.49154017170811015 --weightMax 0.8561325829002088 --weightMin 0.2590408069674177 --weightMax 0.7867296026280648 --velocityUpdate constrainedVelocityUpdate --c1Min 1.5310388766698741 --c1Max 2.5419194089100845 --c2Min 1.0539283751738657 --c2Max 2.5083574596361085 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 8 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.7365767291318479 --velocityChangeWhenUpperLimitIsReached 0.3952351444280946 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "MOPSO500", experimentProblem, run));
  }

  private static void mopso1000(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--swarmSize 194 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 0.8450976113926573 --mutationRepairStrategy round --polynomialMutationDistributionIndex 8.173536788051226 --linkedPolynomialMutationDistributionIndex 281.49317593832365 --uniformMutationPerturbation 0.9751536350846386 --nonUniformMutationPerturbation 0.13891156687516015 --frequencyOfApplicationOfMutationOperator 6 --inertiaWeightComputingStrategy linearIncreasingValue --weight 0.25227311237386296 --weightMin 0.14928096534059906 --weightMax 0.529435537814853 --weightMin 0.2858254954102476 --weightMax 0.5359536789479347 --weightMin 0.3924289252207619 --weightMax 0.8097721607669123 --velocityUpdate constrainedVelocityUpdate --c1Min 1.2423450532833966 --c1Max 2.268685540076807 --c2Min 1.5567150591325247 --c2Max 2.104811117110571 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.4424052668754501 --velocityChangeWhenUpperLimitIsReached -0.11826131132606821 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "MOPSO1K", experimentProblem, run));
  }

  private static void mopso2000(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--swarmSize 168 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation uniform --mutationProbabilityFactor 1.6054602977224606 --mutationRepairStrategy round --polynomialMutationDistributionIndex 31.725788146733997 --linkedPolynomialMutationDistributionIndex 157.85658280378465 --uniformMutationPerturbation 0.160000627214195 --nonUniformMutationPerturbation 0.3467151487234166 --frequencyOfApplicationOfMutationOperator 9 --inertiaWeightComputingStrategy linearIncreasingValue --weight 0.6150556396098367 --weightMin 0.12339067871654703 --weightMax 0.8234892552851494 --weightMin 0.27792727174207355 --weightMax 0.9644336950423879 --weightMin 0.2221108995783994 --weightMax 0.597074401045635 --velocityUpdate constrainedVelocityUpdate --c1Min 1.1900308466649065 --c1Max 2.2084703465249764 --c2Min 1.4082262600910138 --c2Max 2.0951886443856513 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 7 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.5606725989053125 --velocityChangeWhenUpperLimitIsReached -0.8910962021669863 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "MOPSO2K", experimentProblem, run));
  }

  private static void mopso3000(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);\n--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--swarmSize 181 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation polynomial --mutationProbabilityFactor 0.19069412855255297 --mutationRepairStrategy round --polynomialMutationDistributionIndex 7.758061257634148 --linkedPolynomialMutationDistributionIndex 100.99139967525899 --uniformMutationPerturbation 0.9744046358659886 --nonUniformMutationPerturbation 0.26372431605768554 --frequencyOfApplicationOfMutationOperator 7 --inertiaWeightComputingStrategy linearIncreasingValue --weight 0.30099218460615174 --weightMin 0.10679707827611325 --weightMax 0.8566203360264136 --weightMin 0.39225034906624645 --weightMax 0.5317700754305962 --weightMin 0.22795150323770352 --weightMax 0.8096420345181755 --velocityUpdate constrainedVelocityUpdate --c1Min 1.2861170359243448 --c1Max 2.2464706648804946 --c2Min 1.4942195383508226 --c2Max 2.0538224562528304 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 7 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.369107758225796 --velocityChangeWhenUpperLimitIsReached -0.5034630907597963 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "MOPSO3k", experimentProblem, run));
  }
}
