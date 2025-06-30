package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.VelocityUpdate;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.impl.ConstrainedVelocityUpdate;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.impl.DefaultVelocityUpdate;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.impl.SPS2011VelocityUpdate;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different velocity update strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring various velocity update mechanisms used in PSO algorithms.
 * 
 * <p>The available velocity update strategies are:
 * <ul>
 *   <li>defaultVelocityUpdate: Standard velocity update with cognitive and social components</li>
 *   <li>constrainedVelocityUpdate: Velocity update that handles constrained optimization problems</li>
 *   <li>SPSO2011VelocityUpdate: Velocity update following the Standard PSO 2011 specifications</li>
 * </ul>
 * 
 * <p>Each strategy requires specific parameters (c1Min, c1Max, c2Min, c2Max) that control the
 * cognitive and social components of the velocity update.
 */
public class VelocityUpdateParameter extends CategoricalParameter {
  /**
   * Creates a new VelocityUpdateParameter with the specified valid values.
   * 
   * @param validValues A list of valid velocity update strategy names. Typical values include:
   *                   "defaultVelocityUpdate", "constrainedVelocityUpdate", "SPSO2011VelocityUpdate"
   * @throws IllegalArgumentException if validValues is null or empty
   */
  public VelocityUpdateParameter(List<String> validValues) {
    super("velocityUpdate", validValues);
  }

  /**
   * Creates and returns a VelocityUpdate instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Required sub-parameters for each strategy:
   * <ul>
   *   <li>For all strategies: c1Min, c1Max, c2Min, c2Max (double values)</li>
   *   <li>For constrainedVelocityUpdate and SPSO2011VelocityUpdate: problem (DoubleProblem)</li>
   * </ul>
   * 
   * @return A configured VelocityUpdate implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known velocity update strategy
   * @throws ClassCastException if any required sub-parameter has an incorrect type
   * @throws NullPointerException if any required sub-parameter is not found
   */
  public VelocityUpdate getVelocityUpdate() {
    double c1Min;
    double c1Max;
    double c2Min;
    double c2Max;
    DoubleProblem problem;
    switch (value()) {
      case "defaultVelocityUpdate" -> {
        c1Min = (double) findGlobalSubParameter("c1Min").value();
        c1Max = (double) findGlobalSubParameter("c1Max").value();
        c2Min = (double) findGlobalSubParameter("c2Min").value();
        c2Max = (double) findGlobalSubParameter("c2Max").value();
        return new DefaultVelocityUpdate(c1Min, c1Max, c2Min, c2Max);
      }
      case "constrainedVelocityUpdate" -> {
        c1Min = (double) findGlobalSubParameter("c1Min").value();
        c1Max = (double) findGlobalSubParameter("c1Max").value();
        c2Min = (double) findGlobalSubParameter("c2Min").value();
        c2Max = (double) findGlobalSubParameter("c2Max").value();
        problem = (DoubleProblem) nonConfigurableSubParameters().get("problem");
        return new ConstrainedVelocityUpdate(c1Min, c1Max, c2Min, c2Max, problem) {};
      }
      case "SPSO2011VelocityUpdate" -> {
        c1Min = (double) findGlobalSubParameter("c1Min").value();
        c1Max = (double) findGlobalSubParameter("c1Max").value();
        c2Min = (double) findGlobalSubParameter("c2Min").value();
        c2Max = (double) findGlobalSubParameter("c2Max").value();
        problem = (DoubleProblem) nonConfigurableSubParameters().get("problem");
        return new SPS2011VelocityUpdate(c1Min, c1Max, c2Min, c2Max, problem) {};
      }
      default -> throw new JMetalException(value() + " is not a valid velocity update strategy");
    }
  }
}
