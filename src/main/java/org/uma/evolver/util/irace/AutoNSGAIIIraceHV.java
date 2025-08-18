  package org.uma.evolver.util.irace;


import java.io.IOException;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.parameter.type.StringParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

public class AutoNSGAIIIraceHV {
  public static void main(String[] args) throws IOException {
    StringParameter problemNameParameter = new StringParameter("problemName");
    problemNameParameter.parse(args);
    Problem<DoubleSolution> problem = ProblemFactory.loadProblem(problemNameParameter.value()) ;

    StringParameter referenceFrontFilenameParameter = new StringParameter("referenceFrontFileName");
    referenceFrontFilenameParameter.parse(args);
    String referenceFrontFilename = referenceFrontFilenameParameter.value();

    StringParameter populationSizeParameter = new StringParameter("populationSize");
    populationSizeParameter.parse(args);
    int populationSize = Integer.parseInt(populationSizeParameter.value());

    StringParameter maximumNumberOfEvaluationsParameter = new StringParameter("maximumNumberOfEvaluations");
    maximumNumberOfEvaluationsParameter.parse(args);
    int maximumNumberOfEvaluations = Integer.parseInt(maximumNumberOfEvaluationsParameter.value());

    var baseNSGAII = new DoubleNSGAII(problem,
            populationSize,
            maximumNumberOfEvaluations,
            new NSGAIIDoubleParameterSpace()) ;

    baseNSGAII.parse(args);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();
    nsgaII.run();

    String referenceFrontFile =
        "resources/referenceFrontsCSV/" + referenceFrontFilename;

    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFile, ",");
    double[][] front = getMatrixWithObjectiveValues(nsgaII.result()) ;

    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
            NormalizeUtils.normalize(
                    front,
                    NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
                    NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    var qualityIndicator = new PISAHypervolume(normalizedReferenceFront) ;
    System.out.println(qualityIndicator.compute(normalizedFront) * -1.0) ;
  }
}
