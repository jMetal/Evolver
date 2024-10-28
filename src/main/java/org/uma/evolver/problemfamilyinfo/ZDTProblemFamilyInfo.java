package org.uma.evolver.problemfamilyinfo;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.*;

import java.util.List;

public class ZDTProblemFamilyInfo implements ProblemFamilyInfo {
    private static final List<DoubleProblem> problemList =
            List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());
    private static final List<String> referenceFrontFileName =
            List.of(
                    "resources/referenceFronts/ZDT1.csv",
                    "resources/referenceFronts/ZDT2.csv",
                    "resources/referenceFronts/ZDT3.csv",
                    "resources/referenceFronts/ZDT4.csv",
                    "resources/referenceFronts/ZDT6.csv");

    private static final List<Integer> evaluationsToOptimize = List.of(25000, 25000, 25000, 25000, 25000) ;

    public List<DoubleProblem> problemList() {return problemList;}
    public List<String> referenceFronts() {return referenceFrontFileName;}
    public List<Integer> evaluationsToOptimize() {return evaluationsToOptimize ;}
}
