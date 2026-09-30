.. _rvea_guide:

RVEA, RVEA* and iRVEA
=====================

:Encodings: Double
:Parameter space: ``RVEADouble.yaml`` (36 parameters, 6 top-level)
:Default configurations: ``RVEADoubleDefault.txt``, ``RVEAStarDoubleDefault.txt``,
   ``IRVEADoubleDefault.txt``
:Code: `RVEAGuide <https://github.com/jMetal/Evolver/blob/develop/src/main/java/org/uma/evolver/example/algorithms/RVEAGuide.java>`_
:Time: about 2 minutes (the comparison takes about 70 seconds)
:Timings measured on: Apple M5 Pro (18 cores, 16 of them used), 64 GB of RAM, macOS 26.6.2,
   Java 21.0.12 (Oracle JDK), Evolver 2.2-SNAPSHOT, jMetal 7.6

In short: **RVEA** spreads its solutions evenly on regular fronts and keeps doing so with many
objectives, where NSGA-II fails; it does poorly on fronts that do not cover the whole simplex
(degenerate, disconnected or inverted fronts) and, surprisingly, on the bi-objective ZDT1.
**RVEA*** and **iRVEA** fix most of those cases at a small cost on regular fronts. On MaF08, a
problem of the iRVEA paper, iRVEA is the best of the four.

Idea and origin
---------------

RVEA (`Cheng et al., IEEE TEVC 2016 <https://doi.org/10.1109/TEVC.2016.2519378>`_) is a
decomposition-based algorithm designed for many-objective problems. Instead of ranking solutions by
Pareto dominance, which stops discriminating once almost every solution is nondominated, it divides
the objective space into angular niches, one per **reference vector**, and keeps one survivor per
occupied niche. Every generation it:

#. **translates** the objective vectors so that the ideal point (the componentwise minimum of the
   current solutions) is at the origin;
#. **associates** each solution with the reference vector closest to it in angle;
#. **scores** the solutions of each niche with the *angle-penalized distance* (APD):

   .. code-block:: text

      APD(f, v, t) = ||f|| * (1 + M * (t / T)^alpha * angle(f, v) / gamma(v))

   where ``||f||`` is the distance to the ideal point (convergence), ``angle(f, v)`` how far the
   solution is from the centre of its niche (diversity), ``gamma(v)`` the angle from ``v`` to its
   closest neighbour, ``M`` the number of objectives, and ``t / T`` the fraction of the run elapsed.
   Early in the run the score is plain convergence; ``alpha`` sets how quickly the diversity term
   takes over;
#. **selects** the best solution of each occupied niche. Empty niches contribute nothing, so the
   population can shrink below the number of reference vectors;
#. every ``fr * T`` generations, **rescales** the reference vectors to the ranges of the current
   objectives, so that badly scaled objectives are still covered evenly.

A fixed set of vectors spread over the whole simplex is RVEA's strength on regular fronts and its
weakness on the others: when the front covers only part of the simplex, many vectors have no
solution and many niches stay empty.

Variants in Evolver
-------------------

The ``replacement`` parameter selects the variant, with the names and values of jMetal's
``AutoRVEA``:

.. list-table::
   :header-rows: 1
   :widths: 15 85

   * - Value
     - Variant
   * - ``rvea``
     - RVEA as described above.
   * - ``rveaStar``
     - RVEA* (Section VI of the RVEA paper): a second set of reference vectors, whose inactive
       vectors are regenerated at random, meant for irregular fronts. Survivors are the
       nondominated candidates, selected with APD over both sets (up to 2N during the run, N at the
       end).
   * - ``iRVEA``
     - iRVEA (`Liu et al., CEC 2019 <https://doi.org/10.1109/CEC.2019.8790214>`_): replaces at most
       one inactive adaptive vector per generation with a direction of the least covered region,
       protects the regions of the front already found, and in the last part of the run
       (``lateStageFraction``) complements APD with dominance (the strengthened dominance relation
       above six objectives) and an epsilon-indicator selection.

The implementation is jMetal's (``jmetal-component``); its
`documentation of the RVEA variants <https://github.com/jMetal/jMetal/blob/main/docs/rvea-variants.rst>`_
explains the three algorithms in more detail, with a worked example of APD and the choices made
where the papers leave details open.

Parameter space
---------------

Besides the operators shared with the other algorithms (initialization, crossover, mutation,
external archive), ``RVEADouble.yaml`` has:

- ``replacement``: ``rvea``, ``rveaStar`` or ``iRVEA``, with ``alpha`` (in [0.5, 10]) and ``fr``
  (in [0.01, 1]) for all of them, and ``numberOfSubregions`` (in [10, 100]),
  ``lateStageFraction`` (in [0.5, 1]) and ``epsilonKappa`` (in [0.01, 0.2]) for iRVEA;
- ``selection``: ``random`` (RVEA's) or ``tournament``;
- ``offspringPopulationSize``: 10, 20, 50, 100, 150 or 200. Below 10 the running time grows
  without better results, and above 200 the results degrade.

The **population size is the number of reference vectors**. ``DoubleRVEA`` takes them as a list,
or, like MOEA/D, reads them for each problem from a directory of weight vector files named after
the number of objectives and the population size (``resources/weightVectors/W3D_100.dat``, …).

Default configurations
----------------------

The three files of ``defaultConfigurations`` share the operators of jMetal's standard RVEA: SBX
crossover (probability 0.9, distribution index 20), polynomial mutation (probability 1/n,
distribution index 20), random selection, ``alpha`` = 2, ``fr`` = 0.1 and 100 offspring per
generation. The RVEA paper uses SBX with probability 1.0 and distribution index 30.
``IRVEADoubleDefault.txt`` adds iRVEA's defaults: 40 subregions, ``lateStageFraction`` = 0.8 and
``epsilonKappa`` = 0.05.

.. literalinclude:: ../../src/main/resources/defaultConfigurations/IRVEADoubleDefault.txt
   :language: none
   :caption: IRVEADoubleDefault.txt

Running it
----------

From Java, reading the default configuration and the reference vectors of
``resources/weightVectors/W3D_100.dat``:

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/algorithms/RVEAGuide.java
   :language: java
   :start-after: // [step-1-start]
   :end-before: // [step-1-end]
   :dedent: 4

From the command line, with the bundled request ``rvea-dtlz2-request.yaml`` (see
:doc:`../utilities/cli_tools`):

.. code-block:: bash

   java -cp target/Evolver-*-jar-with-dependencies.jar \
       org.uma.evolver.cli.solving.SolveRunnerMain rvea-dtlz2-request.yaml

The experiment
--------------

``RVEAGuide`` compares the three variants, each with its default configuration, with NSGA-II (its
default configuration too) on seven problems chosen for the kind of front they have. Each algorithm
runs 15 times on each problem, with a population of 100. The budget is 25,000 evaluations, except
for DTLZ2 with six objectives (50,000) and MaF08 (60,000, the budget of the iRVEA paper, which uses
a population of 105 instead of 100):

.. list-table::
   :header-rows: 1
   :widths: 25 30 45

   * - Problem
     - Front
     - Why it is here
   * - DTLZ2
     - regular, 3 objectives
     - the case RVEA is built for
   * - DTLZ2 (6 objectives)
     - regular, many objectives
     - RVEA's design target, where dominance-based algorithms struggle
   * - DTLZ5
     - degenerate (a curve in 3D)
     - most reference vectors meet no solution
   * - DTLZ7
     - disconnected (four regions)
     - empty niches between the regions
   * - DTLZ2Minus
     - inverted
     - the front does not fill the simplex the vectors are spread over
   * - MaF08
     - the Pareto set is a triangle in a two-dimensional decision space, 3 objectives
     - a problem of the iRVEA paper, where iRVEA stands out
   * - ZDT1
     - convex, 2 objectives
     - a simple bi-objective reference

MaF08 asks to find a small polygon (its Pareto set, a triangle of side 1.7) in a large decision
space: the MaF test suite defines its two variables in ``[-10000, 10000]``. jMetal's ``MaF08``
bounds them to ``[0, 1]`` up to version 7.6, which contains only a fifth of the triangle, so
``RVEAGuide`` sets the bounds of the suite. The reference front with three objectives,
``resources/referenceFronts/MaF08.3D.csv``, is the image of the whole triangle: 10,011 points
obtained by evaluating the problem on a lattice of it (all of them nondominated).

.. literalinclude:: ../../src/main/java/org/uma/evolver/example/algorithms/RVEAGuide.java
   :language: java
   :start-after: // [step-2-start]
   :end-before: // [step-2-end]
   :dedent: 4

Median IGD+ over the 15 runs (interquartile range in parentheses; lower is better). The best value
of each problem is in bold; ``=`` marks no significant difference from the best (Wilcoxon rank-sum
test, 0.05), and every other value is significantly worse:

.. list-table::
   :header-rows: 1
   :widths: 20 20 20 20 20

   * - Problem
     - RVEA
     - RVEA*
     - iRVEA
     - NSGA-II
   * - DTLZ2
     - **0.0223** (0.0003)
     - 0.0239 (0.0006)
     - 0.0253 (0.0011)
     - 0.0374 (0.0019)
   * - DTLZ2 (6 obj.)
     - **0.1039** (0.0002)
     - 0.1188 (0.0027)
     - 0.1378 (0.0140)
     - 0.9718 (0.1842)
   * - DTLZ5
     - 0.0694 (0.0173)
     - 0.0059 (0.0029)
     - **0.0030** (0.0003)
     - 0.0037 (0.0002)
   * - DTLZ7
     - 0.0579 (0.0110)
     - **0.0248** (0.0012)
     - 0.0344 (0.0058)
     - 0.0366 (0.0030)
   * - DTLZ2Minus
     - 0.0433 (0.0012)
     - **0.0294** (0.0014)
     - 0.0333 (0.0021)
     - 0.0355 (0.0023)
   * - MaF08
     - 0.0487 (0.0052)
     - 0.0274 (0.0031)
     - **0.0236** (0.0008)
     - 0.0363 (0.0025)
   * - ZDT1
     - 0.0319 (0.0026)
     - 0.0039 (0.0003) =
     - **0.0038** (0.0003)
     - 0.0039 (0.0003) =

The hypervolume names the same best algorithm on every problem, except on ZDT1, where iRVEA and
NSGA-II are tied in both indicators. The fronts with the median hypervolume of each algorithm:

.. figure:: ../figures/algorithms/rvea-median-fronts-3d.png
   :align: center
   :alt: Fronts with the median HV of RVEA, RVEA*, iRVEA and NSGA-II on DTLZ2, DTLZ5, DTLZ7 and DTLZ2Minus
   :figwidth: 100%

   Fronts with the median HV on the three-objective problems (reference front in gray).

.. figure:: ../figures/algorithms/rvea-median-fronts-maf08.png
   :align: center
   :alt: Fronts with the median HV of RVEA, RVEA*, iRVEA and NSGA-II on MaF08
   :figwidth: 100%

   Fronts with the median HV on MaF08.

.. figure:: ../figures/algorithms/rvea-median-fronts-zdt1.png
   :align: center
   :alt: Fronts with the median HV of RVEA, RVEA*, iRVEA and NSGA-II on ZDT1
   :figwidth: 100%

   Fronts with the median HV on ZDT1.

Where it works well
-------------------

- **Regular fronts, and above all many objectives.** RVEA is the best of the four on DTLZ2 with
  three and with six objectives, with a very small spread between runs, and places its solutions
  evenly on the front (first row of the figure). With six objectives the gap to NSGA-II is the
  largest of the study: NSGA-II's median hypervolume is 0.0002, against 0.715 for RVEA, since
  Pareto dominance barely discriminates between solutions there. RVEA* and iRVEA are close behind
  RVEA on these problems.
- **Irregular fronts, with the variants.** RVEA* is the best on the disconnected (DTLZ7) and the
  inverted (DTLZ2Minus) fronts, and iRVEA on the degenerate one (DTLZ5), where it also beats
  NSGA-II. Note that on DTLZ7, a disconnected front, RVEA* does better than iRVEA with these
  settings, and iRVEA places most of its solutions in one of the four regions.
- **MaF08: iRVEA stands out.** With the 60,000 evaluations of the iRVEA paper, it is the best of
  the four in IGD+ and in hypervolume, significantly better than RVEA*, than NSGA-II and than plain
  RVEA, and with the smallest spread between runs. It returns 100 solutions spread over the whole
  surface (figure), while RVEA* returns 95, NSGA-II crowds its solutions at the three tips of the
  surface, and RVEA returns only 36, some of them away from it.

Where it works poorly
---------------------

- **Fronts that do not fill the simplex, with plain RVEA.** On DTLZ5, DTLZ7, DTLZ2Minus and MaF08
  many reference vectors meet no solution: RVEA ends with a median of 57, 65, 58 and 36 solutions
  instead of 100, and on DTLZ5 it does not even reach the curve (its IGD+ is twelve times RVEA*'s,
  and varies widely between runs). This is the case RVEA* and iRVEA were designed for. iRVEA fixes
  it completely (100 solutions everywhere); RVEA* improves the IGD+ and returns 100 solutions
  except on DTLZ5 and MaF08, where it still returns 57 and 95.
- **ZDT1.** On this simple bi-objective problem RVEA has not converged after 25,000 evaluations
  (IGD+ 0.032, more than eight times the others'), although it keeps 100 solutions; RVEA*, iRVEA
  and NSGA-II reach the front. A likely cause is that the variants add dominance to APD in their
  selection, while plain RVEA relies on APD alone.
- **The variants on regular fronts.** RVEA* and iRVEA are slightly but significantly worse than
  RVEA on DTLZ2 and with six objectives, and iRVEA's spread between runs is larger with six
  objectives.

Tuning notes
------------

- ``replacement`` matters most: none of the three variants is best everywhere. When the shape of
  the front is unknown, the variants (RVEA* or iRVEA) are the safer choice in this study; on regular
  fronts with many objectives, RVEA.
- ``alpha`` and ``fr`` trade convergence for spread and control how often the vectors adapt; the
  defaults (2 and 0.1) come from the RVEA paper.
- The population size is fixed by the reference vectors, so it is not tuned; the offspring
  population size is.
- To tune RVEA for a set of problems, see :doc:`../tutorials/meta_optimization_workflow`. Training
  sets may mix problems with different numbers of objectives, since the vectors of each problem
  are read from its own file; the bundled request ``rvea-zdt1-dtlz2-request.yaml`` does so.

References
----------

- R. Cheng, Y. Jin, M. Olhofer and B. Sendhoff. A reference vector guided evolutionary algorithm
  for many-objective optimization. IEEE Transactions on Evolutionary Computation, 20(5):773-791,
  2016. `doi:10.1109/TEVC.2016.2519378 <https://doi.org/10.1109/TEVC.2016.2519378>`_
- Q. Liu, Y. Jin, M. Heiderich and T. Rodemann. Adaptation of reference vectors for evolutionary
  many-objective optimization of problems with irregular Pareto fronts. IEEE Congress on
  Evolutionary Computation, 2019.
  `doi:10.1109/CEC.2019.8790214 <https://doi.org/10.1109/CEC.2019.8790214>`_
- N. Rodríguez Uribe. RVEA, RVEA*, and iRVEA. jMetal documentation, 2026.
