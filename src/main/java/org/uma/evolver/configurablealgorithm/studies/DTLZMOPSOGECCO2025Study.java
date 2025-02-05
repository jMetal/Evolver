package org.uma.evolver.configurablealgorithm.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.studies.DTLZStudy;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
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
public class DTLZMOPSOGECCO2025Study {
  private static final int INDEPENDENT_RUNS = 30;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList =
        List.of(
                new ExperimentProblem<>(new DTLZ1()).setReferenceFront("DTLZ1.3D.csv"),
                new ExperimentProblem<>(new DTLZ2()).setReferenceFront("DTLZ2.3D.csv"),
                new ExperimentProblem<>(new DTLZ3()).setReferenceFront("DTLZ3.3D.csv"),
                new ExperimentProblem<>(new DTLZ4()).setReferenceFront("DTLZ4.3D.csv"),
                new ExperimentProblem<>(new DTLZ5()).setReferenceFront("DTLZ5.3D.csv"),
                new ExperimentProblem<>(new DTLZ6()).setReferenceFront("DTLZ6.3D.csv"),
                new ExperimentProblem<>(new DTLZ7()).setReferenceFront("DTLZ7.3D.csv")
        ) ;

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("DTLZMOPSOGECCO2025Study")
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

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
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
                + "--swarmSize 36 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation linkedPolynomial --mutationProbabilityFactor 0.5556554981951717 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 383.5375082577989 --linkedPolynomialMutationDistributionIndex 33.665553533949804 --uniformMutationPerturbation 0.7972144174449137 --nonUniformMutationPerturbation 0.4845653182411224 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy constantValue --weight 0.12278123298098295 --weightMin 0.3244791765342142 --weightMax 0.6995703904346786 --weightMin 0.38760495254461824 --weightMax 0.8949200112492842 --weightMin 0.2514896053894131 --weightMax 0.6196513196706517 --velocityUpdate constrainedVelocityUpdate --c1Min 1.958129816370224 --c1Max 2.275037987118082 --c2Min 1.4942073742585265 --c2Max 2.6362437472002167 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 2 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.09927534203237265 --velocityChangeWhenUpperLimitIsReached -0.5524667173937896 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
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
                + "--swarmSize 194 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 0.8450976113926573 --mutationRepairStrategy round --polynomialMutationDistributionIndex 8.173536788051226 ---swarmSize 98 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 1.4294902197369999 --mutationRepairStrategy round --polynomialMutationDistributionIndex 54.98402801221375 --linkedPolynomialMutationDistributionIndex 89.87198020458874 --uniformMutationPerturbation 0.7125244842785644 --nonUniformMutationPerturbation 0.8871849258718437 --frequencyOfApplicationOfMutationOperator 7 --inertiaWeightComputingStrategy constantValue --weight 0.10706760455944732 --weightMin 0.2010434559533029 --weightMax 0.5532692527073955 --weightMin 0.154470203528214 --weightMax 0.9170445965735178 --weightMin 0.38598198134135353 --weightMax 0.5319535611189163 --velocityUpdate constrainedVelocityUpdate --c1Min 1.4209173794875878 --c1Max 2.74967494275624 --c2Min 1.6573208964540873 --c2Max 2.4911160115135975 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 3 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.33079914505498453 --velocityChangeWhenUpperLimitIsReached 0.329532731116321 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
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
                + "--swarmSize 44 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2007VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation polynomial --mutationProbabilityFactor 0.3918230320721349 --mutationRepairStrategy round --polynomialMutationDistributionIndex 331.328390948196 --linkedPolynomialMutationDistributionIndex 16.529012196167223 --uniformMutationPerturbation 0.48197818873531495 --nonUniformMutationPerturbation 0.1843292692519437 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy constantValue --weight 0.1912261836620765 --weightMin 0.40866803012285213 --weightMax 0.7416114813808212 --weightMin 0.17526130024078374 --weightMax 0.832414661216603 --weightMin 0.25742370582631313 --weightMax 0.8408198306633659 --velocityUpdate constrainedVelocityUpdate --c1Min 1.1401773001862519 --c1Max 2.4813946251449974 --c2Min 1.9506820584394193 --c2Max 2.932777163796552 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 7 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.06425201436499761 --velocityChangeWhenUpperLimitIsReached 0.06638029636706855 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
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
                + "--swarmSize 78 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization latinHypercubeSampling --velocityInitialization defaultVelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation linkedPolynomial --mutationProbabilityFactor 0.33644797509129987 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 190.20924310480223 --linkedPolynomialMutationDistributionIndex 374.3335982316379 --uniformMutationPerturbation 0.12694697695037221 --nonUniformMutationPerturbation 0.14956795895089525 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy constantValue --weight 0.10478061816458673 --weightMin 0.11816982688291236 --weightMax 0.7354618219503289 --weightMin 0.2621208154617437 --weightMax 0.7176963613275431 --weightMin 0.2240885536094285 --weightMax 0.6152519975936961 --velocityUpdate constrainedVelocityUpdate --c1Min 1.5794561833369 --c1Max 2.0665227780557687 --c2Min 1.9631410232540714 --c2Max 2.574139279070546 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.0011528938361706764 --velocityChangeWhenUpperLimitIsReached -0.12700540497081358 \n")
            .split("\\s+");

    var autoMopso = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
    autoMopso.parse(parameters);

    ParticleSwarmOptimizationAlgorithm mopso = autoMopso.build();

    algorithms.add(new ExperimentAlgorithm<>(mopso, "MOPSO3k", experimentProblem, run));
  }
}
