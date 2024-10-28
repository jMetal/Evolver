package org.uma.evolver.configurablealgorithm.validator;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.uma.evolver.problemfamilyinfo.DTLZ3DProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.ZDTProblemFamilyInfo;

public class DTLZValidation {
  public static void main(String[] args) throws IOException {
    List<Integer> evaluations = List.of(200, 400, 600, 800, 1000);
    List<String> configurations =
        List.of(
            "--algorithmResult population --populationSizeWithArchive 135 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.46200412860529044 --crossoverRepairStrategy round --sbxDistributionIndex 278.0915735074215 --blxAlphaCrossoverAlphaValue 0.2623849880931365 --mutation uniform --mutationProbabilityFactor 0.4168613858285186 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 198.34763230261584 --linkedPolynomialMutationDistributionIndex 65.7460065418232 --uniformMutationPerturbation 0.4118887660567375 --nonUniformMutationPerturbation 0.6304308957337127 --selection tournament --selectionTournamentSize 4 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 135 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.46941446652813545 --crossoverRepairStrategy round --sbxDistributionIndex 278.0915735074215 --blxAlphaCrossoverAlphaValue 0.21030509129383512 --mutation uniform --mutationProbabilityFactor 1.0898481699233071 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 198.34763230261584 --linkedPolynomialMutationDistributionIndex 65.7460065418232 --uniformMutationPerturbation 0.4118887660567375 --nonUniformMutationPerturbation 0.6304308957337127 --selection tournament --selectionTournamentSize 4 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 135 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.636535186055123 --crossoverRepairStrategy round --sbxDistributionIndex 278.0915735074215 --blxAlphaCrossoverAlphaValue 0.21030509129383512 --mutation uniform --mutationProbabilityFactor 1.0909797354182236 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 198.34763230261584 --linkedPolynomialMutationDistributionIndex 205.19928709168857 --uniformMutationPerturbation 0.4118887660567375 --nonUniformMutationPerturbation 0.8068755193411988 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 134 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6399661586268514 --crossoverRepairStrategy bounds --sbxDistributionIndex 216.08043572622572 --blxAlphaCrossoverAlphaValue 0.08326753121134181 --mutation uniform --mutationProbabilityFactor 1.091126206820895 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 199.64053240605043 --linkedPolynomialMutationDistributionIndex 219.1407891419901 --uniformMutationPerturbation 0.43563374519988174 --nonUniformMutationPerturbation 0.8160032279502352 --selection tournament --selectionTournamentSize 7 \n",
            "--algorithmResult externalArchive --populationSizeWithArchive 194 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8969522374607675 --crossoverRepairStrategy bounds --sbxDistributionIndex 109.64863620529424 --blxAlphaCrossoverAlphaValue 0.20623456440325344 --mutation uniform --mutationProbabilityFactor 1.0911484852094242 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 206.8491021014326 --linkedPolynomialMutationDistributionIndex 219.65904143455126 --uniformMutationPerturbation 0.5595373600783109 --nonUniformMutationPerturbation 0.6389316586948646 --selection tournament --selectionTournamentSize 7 \n");

    ConfigurableNSGAIIValidatorV2 validator = new ConfigurableNSGAIIValidatorV2();
    for (int i : IntStream.range(0, evaluations.size()).toArray()) {
          validator.validate(configurations.get(i).split("\\s+"), new DTLZ3DProblemFamilyInfo(), "DTLZValidation"+evaluations.get(i)+".csv");
    }
  }
}
