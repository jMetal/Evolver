package org.uma.evolver.factory;

import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;

public class ConfigurableProblemFactory {
    public static ConfigurableAlgorithmBuilder getProblem(String name, DoubleProblem problem, int population, int maxNumberOfEvaluations) {
        ConfigurableAlgorithmBuilder configurableAlgorithmProblem = switch (name) {
            case "NSGAII" -> new ConfigurableNSGAII(
                    problem, population, maxNumberOfEvaluations);
            case "MOPSO" -> new ConfigurableMOPSO(
                    problem, population, maxNumberOfEvaluations);
            case "MOEAD" -> new ConfigurableMOEAD(problem, population, maxNumberOfEvaluations, "resources/weightVectors") ;
            case "SMSEMOA" -> new ConfigurableSMSEMOA(
                    problem, population, maxNumberOfEvaluations);
            default -> throw new RuntimeException("Configurable problem not found");
        };
        return configurableAlgorithmProblem;
    }

    public static ConfigurableAlgorithmBuilder getProblem(String name, DoubleProblem problem, int population, int maxNumberOfEvaluations, String weightVectorFilesDirectory) {
        ConfigurableAlgorithmBuilder configurableAlgorithmProblem = switch (name) {
            case "MOEAD" -> new ConfigurableMOEAD(
                    problem, population, maxNumberOfEvaluations, weightVectorFilesDirectory);
            default -> ConfigurableProblemFactory.getProblem(name, problem, population, maxNumberOfEvaluations);
        };
        return configurableAlgorithmProblem;
    }

}
