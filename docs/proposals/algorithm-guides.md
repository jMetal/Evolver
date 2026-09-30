# Algorithm guides: catalogue and development plan

**Status:** living document — the plan of the algorithm guides (`docs/algorithms/`), one per
base-level algorithm of Evolver, and their status. Like the tutorials (`tutorials.md`), guides are
written one at a time and reviewed before the next one; a guide that has passed review is marked
"Done, vX.Y (date)" here.

## Why guides, and how they differ from the tutorials

The tutorials teach how to use Evolver (parameter spaces, training, analysis, validation, irace).
A guide teaches one algorithm: where it comes from, the variants Evolver offers, how to configure
and run it, and, backed by a reproducible experiment, where it works well and where it works
poorly. The claims of those two sections are written after looking at the results, never assumed.

## Outline of every guide

1. **Idea and origin**: the paper and the central mechanism.
2. **Variants in Evolver**: which parameter selects them.
3. **Parameter space**: what is specific to the algorithm, and the number of parameters.
4. **Default configurations**: the files of `defaultConfigurations` (one per variant).
5. **Running it**: from Java (fragments of the guide's class) and from the command line (a request
   of `cli/solving`).
6. **Where it works well** and 7. **where it works poorly**: an experiment with 15 runs per
   algorithm and problem, IGD+ and HV medians with Wilcoxon tests, and the fronts with the median
   HV, against a reference algorithm (NSGA-II unless another one fits better), with hardware and
   software stated.
8. **Tuning notes**: which parameters matter most.
9. **References**.

Code: a class `org.uma.evolver.example.algorithms.<Algorithm>Guide`, with the comparison as an
`ExperimentBuilder` study, and an integration test `<Algorithm>GuideIT` that runs it with minimal
budgets. Figures go to `docs/figures/algorithms/`.

## Guides

| Algorithm | Encodings | Problems of the experiment | Status |
|---|---|---|---|
| RVEA (RVEA, RVEA*, iRVEA) | Double | DTLZ2 (3 and 6 objectives), DTLZ5, DTLZ7, DTLZ2Minus, ZDT1 | First version (`docs/algorithms/rvea.rst`, `example.algorithms.RVEAGuide`), in review |
| NSGA-II | Double, Binary, Permutation | to be chosen; the reference algorithm of the other guides | Planned (next) |
| MOEA/D | Double, Binary, Permutation | to be chosen | Planned |
| NSGA-III | Double | to be chosen | Planned |
| SMS-EMOA | Double, Binary, Permutation | to be chosen | Planned |
| AGE-MOEA | Double | to be chosen | Planned |
| MOPSO | Double | to be chosen | Planned |
| RDEMOEA | Double, Permutation | to be chosen | Planned |
| SSMOEA | Double | to be chosen; its section in `concepts/base_level_metaheuristics.rst` moves here | Planned |
| PAES | Double, Binary, Permutation | to be chosen; its section in `concepts/base_level_metaheuristics.rst` moves here | Planned |

## Order

RVEA first (it prompted the section, after its update to jMetal 7.6), then NSGA-II, the reference
algorithm of the comparisons, and MOEA/D; the rest in the order of the table, revisiting priorities
as the guides are written.
