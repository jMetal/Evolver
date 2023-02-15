package org.uma.evolver.component;

import java.util.List;
import org.uma.evolver.referencefrontupdate.ReferenceFrontUpdate;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.solution.Solution;

public class ReplacementUpdatingReferenceFront<S extends Solution<?>> implements Replacement<S> {
  private Replacement<S> alreadyInstantiatedReplacement ;
  private ReferenceFrontUpdate<S> referenceFrontUpdate ;

  public ReplacementUpdatingReferenceFront(Replacement<S> alreadyInstantiatedReplacement, ReferenceFrontUpdate<S> referenceFrontUpdate) {
    this.alreadyInstantiatedReplacement = alreadyInstantiatedReplacement ;
    this.referenceFrontUpdate = referenceFrontUpdate ;
  }


  @Override
  public List<S> replace(List<S> currentList, List<S> offspringList) {
    List<S> currentPopulation = alreadyInstantiatedReplacement.replace(currentList, offspringList) ;

    referenceFrontUpdate.update(currentPopulation);

    return currentPopulation;
  }
}
