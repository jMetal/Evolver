package org.uma.evolver.factory;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class ProblemFactory {
    public static DoubleProblem getProblem(String name) {
        Problem<DoubleSolution> problem = org.uma.jmetal.problem.ProblemFactory.loadProblem(name);
        return (DoubleProblem) problem;
    }

    public static DoubleProblem[] getProblems(String names) {
        return ProblemFactory.getProblems(names, ",");
    }

    public static DoubleProblem[] getProblems(String names, String separator) {
        String[] namesArray = names.split(separator);

        return ProblemFactory.getProblems(namesArray);
    }

    public static DoubleProblem[] getProblems(String[] names) {
        DoubleProblem[] problems = new DoubleProblem[names.length];

        for (int i = 0; i < names.length; i++) {
            String problem = names[i];
            problems[i] = ProblemFactory.getProblem(problem);
        }

        return problems;
    }
}
