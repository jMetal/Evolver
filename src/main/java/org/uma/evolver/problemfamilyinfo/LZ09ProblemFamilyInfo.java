package org.uma.evolver.problemfamilyinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.lz09.*;

public class LZ09ProblemFamilyInfo implements ProblemFamilyInfo {
    private static final int DEFAULT_NUMBER_OF_EVALUATIONS = 150000 ;

  private static final List<DoubleProblem> problemList =
      List.of(
          //new LZ09F1(),
          new LZ09F2(),
              new LZ09F3(),
              new LZ09F6(),
              new LZ09F7(),
              new LZ09F9()
      );

    private static final List<String> referenceFrontFileName =
            IntStream.range(1, problemList.size()+1)
                    .mapToObj(id -> "resources/referenceFrontsCSV/LZ09_F" + id + ".csv")
                    .toList();

    private static final List<Integer> evaluationsToOptimize =
            new ArrayList<>(Collections.nCopies(problemList.size(), DEFAULT_NUMBER_OF_EVALUATIONS));

    @Override
    public List<DoubleProblem> problemList() {return problemList;}
    @Override
    public List<String> referenceFronts() {return referenceFrontFileName;}
    @Override
    public List<Integer> evaluationsToOptimize() {return evaluationsToOptimize ;}
    @Override
    public String name() {return "LZ09";}
}
