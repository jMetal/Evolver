package org.uma.evolver.algorithm.base.nsgaii.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationDoubleParameter;
import org.uma.evolver.parameter.type.*;

/**
 * Experimental parameter space for NSGA-II algorithm variants using real-coded (double) solutions.
 *
 * <p>This class combines the functionality of {@link NSGAIICommonParameterSpace} and
 * {@link NSGAIIDoubleParameterSpace} into a single class specifically designed for
 * {@code DoubleSolution} representations.
 *
 * <p>It defines and configures all relevant parameters for the NSGA-II algorithm when
 * working with double-encoded solutions, including crossover, mutation, and selection operators.
 *
 * <p>Typical usage:
 * <pre>{@code
 * NSGAExperimentalParameterSpace parameterSpace = new NSGAExperimentalParameterSpace();
 * // Configure parameters as needed
 * }</pre>
 */
public class NSGAExperimentalParameterSpace extends ParameterSpace {
  
   public NSGAExperimentalParameterSpace() {
     super();
     setParameterSpace();
     setParameterRelationships();
     setTopLevelParameters();
   }
   
  /**
   * Defines and adds all parameters to the parameter space.
   * <p>
   * This includes both common NSGA-II parameters and double-specific parameters.
   * </p>
   */
  protected void setParameterSpace() {
    // Common NSGA-II parameters
    put(new CategoricalParameter("algorithmResult", List.of("population", "externalArchive")));
    put(new IntegerParameter("populationSizeWithArchive", 10, 200));
    put(
        new ExternalArchiveParameter<>(
                "archiveType", List.of("crowdingDistanceArchive", "unboundedArchive")));

    put(
        new CategoricalIntegerParameter(
            "offspringPopulationSize", List.of(1, 2, 5, 10, 20, 50, 100, 200, 400)));
    put(new VariationDoubleParameter(List.of("crossoverAndMutationVariation")));

    put(new SelectionParameter<>(List.of("tournament", "random")));
    put(new IntegerParameter("selectionTournamentSize", 2, 10));

    // Double-specific parameters
    put(
        new CreateInitialSolutionsDoubleParameter(
            List.of("default", "latinHypercubeSampling", "scatterSearch")));

    put(new DoubleCrossoverParameter(List.of("SBX", "blxAlpha", "wholeArithmetic", "blxAlphaBeta", 
        "arithmetic", "laplace", "fuzzyRecombination", "PCX", "UNDC")));
    put(new DoubleParameter("crossoverProbability", 0.0, 1.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            "crossoverRepairStrategy", List.of("random", "round", "bounds")));
    put(new DoubleParameter("sbxDistributionIndex", 5.0, 400.0));
    put(new DoubleParameter("pcxCrossoverZeta", 0.1, 0.5));
    put(new DoubleParameter("pcxCrossoverEta", 0.1, 0.5));
    put(new DoubleParameter("blxAlphaCrossoverAlpha", 0.0, 1.0));
    put(new DoubleParameter("blxAlphaBetaCrossoverBeta", 0.0, 1.0));
    put(new DoubleParameter("blxAlphaBetaCrossoverAlpha", 0.0, 1.0));
    put(new DoubleParameter("laplaceCrossoverScale", 0.0, 1.0));
    put(new DoubleParameter("fuzzyRecombinationCrossoverAlpha", 0.0, 1.0));
    put(new DoubleParameter("undcCrossoverZeta", 0.1, 1.0));
    put(new DoubleParameter("undcCrossoverEta", 0.1, 0.5));
    
    put(new DoubleMutationParameter(List.of("uniform", "polynomial", "linkedPolynomial", 
        "nonUniform", "levyFlight", "powerLaw")));
    put(new DoubleParameter("mutationProbabilityFactor", 0.0, 2.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            "mutationRepairStrategy", List.of("random", "round", "bounds")));
    put(new DoubleParameter("polynomialMutationDistributionIndex", 5.0, 400.0));
    put(new DoubleParameter("linkedPolynomialMutationDistributionIndex", 5.0, 400.0));
    put(new DoubleParameter("uniformMutationPerturbation", 0.0, 1.0));
    put(new DoubleParameter("nonUniformMutationPerturbation", 0.0, 1.0));
    put(new DoubleParameter("levyFlightMutationBeta", 1.0, 2.0));
    put(new DoubleParameter("levyFlightMutationStepSize", 0.01, 1.0));
    put(new DoubleParameter("powerLawMutationDelta", 0.0, 10.0));
  }

  /**
   * Establishes relationships and dependencies between parameters.
   * <p>
   * This includes both common NSGA-II relationships and double-specific relationships.
   * </p>
   */
  protected void setParameterRelationships() {
    // Common NSGA-II relationships
    get("algorithmResult")
        .addSpecificSubParameter("externalArchive", get("populationSizeWithArchive"))
        .addSpecificSubParameter("externalArchive", get("archiveType"));

    get("variation")
        .addSpecificSubParameter("crossoverAndMutationVariation", get("crossover"))
        .addSpecificSubParameter("crossoverAndMutationVariation", get("mutation"));

    get("selection").addSpecificSubParameter("tournament", get("selectionTournamentSize"));

    // Double-specific relationships
    get("crossover")
        .addGlobalSubParameter(get("crossoverProbability"))
        .addGlobalSubParameter(get("crossoverRepairStrategy"))
        .addSpecificSubParameter("SBX", get("sbxDistributionIndex"))
        .addSpecificSubParameter("PCX", get("pcxCrossoverZeta"))
        .addSpecificSubParameter("PCX", get("pcxCrossoverEta"))
        .addSpecificSubParameter("blxAlpha", get("blxAlphaCrossoverAlpha"))
        .addSpecificSubParameter("blxAlphaBeta", get("blxAlphaBetaCrossoverBeta"))
        .addSpecificSubParameter("blxAlphaBeta", get("blxAlphaBetaCrossoverAlpha"))
        .addSpecificSubParameter("laplace", get("laplaceCrossoverScale"))
        .addSpecificSubParameter("fuzzyRecombination", get("fuzzyRecombinationCrossoverAlpha"))
        .addSpecificSubParameter("UNDC", get("undcCrossoverZeta"))
        .addSpecificSubParameter("UNDC", get("undcCrossoverEta"));

    get("mutation")
        .addGlobalSubParameter(get("mutationProbabilityFactor"))
        .addGlobalSubParameter(get("mutationRepairStrategy"))
        .addSpecificSubParameter("uniform", get("uniformMutationPerturbation"))
        .addSpecificSubParameter("nonUniform", get("nonUniformMutationPerturbation"))
        .addSpecificSubParameter("polynomial", get("polynomialMutationDistributionIndex"))
        .addSpecificSubParameter(
            "linkedPolynomial", get("linkedPolynomialMutationDistributionIndex"))
        .addSpecificSubParameter("levyFlight", get("levyFlightMutationBeta"))
        .addSpecificSubParameter("levyFlight", get("levyFlightMutationStepSize"))
        .addSpecificSubParameter("powerLaw", get("powerLawMutationDelta"));
  }

  /**
   * Identifies and adds the top-level parameters to the list.
   * <p>
   * Top-level parameters are the main entry points for user configuration.
   * </p>
   */
  protected void setTopLevelParameters() {
    topLevelParameters.add(parameterSpace.get("algorithmResult"));
    topLevelParameters.add(parameterSpace.get("createInitialSolutions"));
    topLevelParameters.add(parameterSpace.get("offspringPopulationSize"));
    topLevelParameters.add(parameterSpace.get("variation"));
    topLevelParameters.add(parameterSpace.get("selection"));
  }
}
