package org.uma.evolver.problemfamilyinfo;

import java.util.List;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;

public class WFG2DProblemFamilyInfo implements ProblemFamilyInfo {
    private static final List<DoubleProblem> problemList =
            List.of(new WFG1(), new WFG2(), new WFG3(), new WFG4(),
                    new WFG5(), new WFG6(), new WFG7(), new WFG8(), new WFG9());

    private static final List<String> referenceFrontFileName =
            List.of(
                    "resources/referenceFronts/WFG1.2D.csv",
                    "resources/referenceFronts/WFG2.2D.csv",
                    "resources/referenceFronts/WFG3.2D.csv",
                    "resources/referenceFronts/WFG4.2D.csv",
                    "resources/referenceFronts/WFG5.2D.csv",
                    "resources/referenceFronts/WFG6.2D.csv",
                    "resources/referenceFronts/WFG7.2D.csv",
                    "resources/referenceFronts/WFG8.2D.csv",
                    "resources/referenceFronts/WFG9.2D.csv");

    private static final List<Integer> evaluationsToOptimize = List.of(25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000, 25000) ;

    public List<DoubleProblem> problemList() {return problemList;}
    public List<String> referenceFronts() {return referenceFrontFileName;}
    public List<Integer> evaluationsToOptimize() {return evaluationsToOptimize ;}
}
