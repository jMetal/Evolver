package org.uma.evolver.configurablealgorithm.validator;

import org.uma.evolver.problemfamilyinfo.ZDTProblemFamilyInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

public class ZDTValidation {
  public static void main(String[] args) throws IOException {
    List<Integer> evaluations = List.of(200, 400, 600, 800, 1000);
    List<String> configurations =
        List.of(
            "--algorithmResult externalArchive --populationSizeWithArchive 41 --externalArchive crowdingDistanceArchive --createInitialSolutions random --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.39796060032274116 --crossoverRepairStrategy bounds --sbxDistributionIndex 377.9949108703598 --blxAlphaCrossoverAlphaValue 0.5836350286299609 --mutation polynomial --mutationProbabilityFactor 1.2376485052098856 --mutationRepairStrategy round --polynomialMutationDistributionIndex 15.906675895745126 --linkedPolynomialMutationDistributionIndex 85.10965870313255 --uniformMutationPerturbation 0.592042223435564 --nonUniformMutationPerturbation 0.016821542123689748 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 41 --externalArchive crowdingDistanceArchive --createInitialSolutions random --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.39796060032274116 --crossoverRepairStrategy bounds --sbxDistributionIndex 377.9949108703598 --blxAlphaCrossoverAlphaValue 0.5836350286299609 --mutation polynomial --mutationProbabilityFactor 1.2376485052098856 --mutationRepairStrategy round --polynomialMutationDistributionIndex 15.906675895745126 --linkedPolynomialMutationDistributionIndex 85.10965870313255 --uniformMutationPerturbation 0.592042223435564 --nonUniformMutationPerturbation 0.016821542123689748 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 42 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.47758806164675033 --crossoverRepairStrategy bounds --sbxDistributionIndex 52.01693700349408 --blxAlphaCrossoverAlphaValue 0.02419174057226703 --mutation polynomial --mutationProbabilityFactor 0.7016252287085118 --mutationRepairStrategy round --polynomialMutationDistributionIndex 16.210981034468638 --linkedPolynomialMutationDistributionIndex 96.52339096585474 --uniformMutationPerturbation 0.6396839081438404 --nonUniformMutationPerturbation 0.01930447184833403 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 41 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.47758806164675033 --crossoverRepairStrategy bounds --sbxDistributionIndex 370.1853994830192 --blxAlphaCrossoverAlphaValue 0.5670361633660745 --mutation polynomial --mutationProbabilityFactor 0.7110174491442811 --mutationRepairStrategy round --polynomialMutationDistributionIndex 16.213815427736314 --linkedPolynomialMutationDistributionIndex 96.34361445520548 --uniformMutationPerturbation 0.5920874270423695 --nonUniformMutationPerturbation 0.31567208155788773 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 41 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.47758806164675033 --crossoverRepairStrategy bounds --sbxDistributionIndex 370.1853994830192 --blxAlphaCrossoverAlphaValue 0.5670361633660745 --mutation polynomial --mutationProbabilityFactor 0.7110174491442811 --mutationRepairStrategy round --polynomialMutationDistributionIndex 16.213815427736314 --linkedPolynomialMutationDistributionIndex 96.34361445520548 --uniformMutationPerturbation 0.5920874270423695 --nonUniformMutationPerturbation 0.31567208155788773 --selection tournament --selectionTournamentSize 7 \n");

    ConfigurableNSGAIIValidatorV2 validator = new ConfigurableNSGAIIValidatorV2();
    for (int i : IntStream.range(0, evaluations.size()).toArray()) {
      validator.validate(configurations.get(i).split("\\s+"), new ZDTProblemFamilyInfo(), "ZDTValidation"+evaluations.get(i)+".csv");
    }
  }
}
