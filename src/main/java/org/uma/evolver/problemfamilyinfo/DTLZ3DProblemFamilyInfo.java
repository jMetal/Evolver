package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;

public class DTLZ3DProblemFamilyInfo implements ProblemFamilyInfo {
    private static int defaultNumberOfEvaluations = 40000 ;

    private static final List<DoubleProblem> problemList =
            List.of(new DTLZ1(), new DTLZ2(), new DTLZ3(), new DTLZ4(),
                    new DTLZ5(), new DTLZ6(), new DTLZ7());

    private static final List<String> referenceFrontFileName =
            IntStream.range(0, problemList.size())
                    .mapToObj(id -> "resources/referenceFronts/DTLZ" + id + ".3D.csv")
                    .toList();

    private static final List<Integer> evaluationsToOptimize =
            new ArrayList<>(Collections.nCopies(problemList.size(), defaultNumberOfEvaluations));
    @Override
    public List<DoubleProblem> problemList() {return problemList;}
    @Override
    public List<String> referenceFronts() {return referenceFrontFileName;}
    @Override
    public List<Integer> evaluationsToOptimize() {return evaluationsToOptimize ;}
    @Override
    public String name() {return "DTLZ3D";}
}
