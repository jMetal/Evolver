package org.uma.evolver;

import java.util.List;

public class ParameterValues {

    public static String getFromProbability(List<String> values, double probability) {
        int index = (int) Math.floor(probability* values.size());
        return values.get(index);
    }

    public static double minMaxDoubleFromProbability(List<Double> values, double probability) {
        double min = values.get(0);
        double max = values.get(values.size()-1);
        return min + probability * (max-min);
    }

    public static int minMaxIntegerFromProbability(List<Integer> values, double probability) {
        int min = values.get(0);
        int max = values.get(values.size()-1) + 1; // Non-inclusive
        return min + (int) Math.floor(probability * (max-min));
    }

    public static List<Integer> offspringPopulationSize = List.of(
            1, 200
    );

    public static List<Integer> tournamentSize = List.of(
            2, 8
    );
    public static List<Double> sbxCrossoverDistributionIndexRange = List.of(
            5.0, 400.0
    );

    public static List<Double> blxAlphaCrossoverValueRange = List.of(
            0.0, 1.0
    );

    public static List<Double> polynomialMutationDistributionIndexRange = List.of(
            5.0, 400.0
    );

    public static List<Double> linkedPolynomialMutationDistributionIndexRange = List.of(
            5.0, 400.0
    );
    public static List<Double> uniformMutationPerturbationRange = List.of(
            0.0, 1.0
    );
    public static List<Double> nonUniformMutationPerturbationRange = List.of(
            0.0, 1.0
    );
    public static List<String> createInitialSolutions = List.of(
            "random", "latinHypercubeSampling", "scatterSearch"
    );

    public static List<String> crossover = List.of(
            "SBX", "BLX_ALPHA", "wholeArithmetic"
    );

    public static List<String> mutation = List.of(
            "uniform", "polynomial", "linkedPolynomial", "nonUniform"
    );


}
