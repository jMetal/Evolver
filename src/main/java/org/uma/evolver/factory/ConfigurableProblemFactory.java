package org.uma.evolver.factory;

import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAIIWithDE;
import org.uma.evolver.algorithm.impl.ConfigurableSMSEMOA;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;

public class ConfigurableProblemFactory {
    public static ConfigurableAlgorithmBuilder getProblem(String name, DoubleProblem problem, int population, int maxNumberOfEvaluations) {
        ConfigurableAlgorithmBuilder configurableAlgorithmProblem = switch (name) {
            case "NSGAII" -> new ConfigurableNSGAII(
                    problem, population, maxNumberOfEvaluations);
            case "NSGAIIDE" -> new ConfigurableNSGAIIWithDE(
                    problem, population, maxNumberOfEvaluations);
            case "MOPSO" -> new ConfigurableMOPSO(
                    problem, population, maxNumberOfEvaluations);
            // TODO: Deal with extra parameter in configurable MOEAD
            //case "MOEAD" -> new ConfigurableMOEAD(
            //problem, population, maxNumberOfEvaluations);
            case "SMSEMOA" -> new ConfigurableSMSEMOA(
                    problem, population, maxNumberOfEvaluations);
            default -> throw new RuntimeException("Configurable problem not found");
        };
        return configurableAlgorithmProblem;
    }

}
