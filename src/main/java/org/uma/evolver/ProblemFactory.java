package org.uma.evolver;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.uf.*;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.glt.*;
import org.uma.jmetal.problem.multiobjective.lz09.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.IOException;

public class ProblemFactory {
    public static DoubleProblem getProblem(String name) {
        DoubleProblem problem = switch (name) {
            case "DTLZ1_2D" -> new DTLZ1_2D();
            case "DTLZ2_2D" -> new DTLZ2_2D();
            case "DTLZ3_2D" -> new DTLZ3_2D();
            case "DTLZ4_2D" -> new DTLZ4_2D();
            case "DTLZ5_2D" -> new DTLZ5_2D();
            case "DTLZ6_2D" -> new DTLZ6_2D();
            case "DTLZ7_2D" -> new DTLZ7_2D();
            case "GLT1" -> new GLT1();
            case "GLT2" -> new GLT2();
            case "GLT3" -> new GLT3();
            case "GLT4" -> new GLT4();
            case "GLT5" -> new GLT5();
            case "GLT6" -> new GLT6();
            case "LZ09F1" -> new LZ09F1();
            case "LZ09F2" -> new LZ09F2();
            case "LZ09F3" -> new LZ09F3();
            case "LZ09F4" -> new LZ09F4();
            case "LZ09F5" -> new LZ09F5();
            case "LZ09F6" -> new LZ09F6();
            case "LZ09F7" -> new LZ09F7();
            case "LZ09F8" -> new LZ09F8();
            case "LZ09F9" -> new LZ09F9();
            case "UF1" -> new UF1();
            case "UF2" -> new UF2();
            case "UF3" -> new UF3();
            case "UF4" -> new UF4();
            case "UF5" -> new UF5();
            case "UF6" -> new UF6();
            case "UF7" -> new UF7();
            case "UF8" -> new UF8();
            case "UF9" -> new UF9();
            case "UF10" -> new UF10();
            case "WFG1" -> new WFG1();
            case "WFG2" -> new WFG2();
            case "WFG3" -> new WFG3();
            case "WFG4" -> new WFG4();
            case "WFG5" -> new WFG5();
            case "WFG6" -> new WFG6();
            case "WFG7" -> new WFG7();
            case "WFG8" -> new WFG8();
            case "WFG9" -> new WFG9();
            case "ZDT1" -> new ZDT1();
            case "ZDT2" -> new ZDT2();
            case "ZDT3" -> new ZDT3();
            case "ZDT4" -> new ZDT4();
            case "ZDT6" -> new ZDT6();
            default -> throw new RuntimeException("Problem not found");
        };
        return problem;
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
