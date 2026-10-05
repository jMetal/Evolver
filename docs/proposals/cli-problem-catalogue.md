# Problem catalogue in the introspection manifest (`DescribeMain`)

**Status:** implemented (2026-10-05) for Evolver 2.4, with the decisions below. Extends `cli-describe-manifest.md`.

## Motivation

`DescribeMain` lists the problems that `cli.training` and `cli.solving` can resolve by name as a
plain list (`problems: [DTLZ1, ..., ZDT6]`). An external tool (Evolver-Studio) cannot tell more
about a problem than its name, and that blocks three things:

- **Encodings other than Double.** The 95 registered problems are all `Problem<DoubleSolution>`.
  Binary problems (ZDT5) and permutation problems (the multi-objective TSP) are reachable only by
  their fully-qualified class name, so no selector can offer them. Evolver-Studio's *Run algorithm*
  offers the binary and permutation encodings of NSGA-II (Evolver 2.3) but lists only continuous
  problems: choosing one of them with those encodings fails at run time, because nothing checks that
  the problem and the algorithm agree. NSGA-II with the Permutation encoding on ZDT1 ends with
  `status: FAILED` and the message `class DefaultDoubleSolution cannot be cast to class
  PermutationSolution`, which names no parameter of the request.
- **Problems with arguments.** DTLZ, WFG, ZCAT, UF, LZ09 and LSMOP take constructor arguments (the
  number of objectives and variables, for instance) that a request can already give (`{class: ...,
  args: [7, 3]}`), but nothing says which arguments a problem has, of what type, or their defaults.
- **A Problems page** (Evolver-Studio's Explore › Problems is a placeholder) and the tutorial about
  solving your own problem (S10).

## What can be known about a problem

A probe of the 95 registered problems (instantiating each one with its no-arg constructor: 35 ms in
all, no failures) shows what can be derived and what cannot:

- **Derivable:** the encoding (the interface the problem implements: `DoubleProblem`,
  `BinaryProblem` or `PermutationProblem`), the default number of objectives and variables
  (`numberOfObjectives()`, `numberOfVariables()`), the family (the last segment of the package:
  `dtlz`, `wfg`, `zdt`, `re`, `rwa`, `lz09`, `uf`, `zcat`, `lsmop`) and the arity of each public
  constructor.
- **Not derivable:** the *names* of the constructor arguments. jMetal is not compiled with
  `-parameters`, so reflection gives `arg0`, `arg1`. The meaning of each argument (is the first one
  the number of variables or of objectives?) has to be written down, once per family.

## Design

### `ProblemDescriptor`

`ProblemRegistry` gains `registeredProblems()`, a list of

```java
record ProblemDescriptor(
    String name,                     // the name a request uses: "DTLZ2"
    String family,                   // "DTLZ", derived from the package
    String encoding,                 // "Double", "Binary" or "Permutation", derived from the interface
    Integer numberOfObjectives,      // of the no-arg problem; null if it cannot be instantiated
    Integer numberOfVariables,       // idem
    List<ArgumentDescriptor> arguments) {}

record ArgumentDescriptor(String name, String type, Object defaultValue) {}
```

The arguments come from a small table of the families that have them (`ZDT`: `numberOfVariables`;
`DTLZ`: `numberOfVariables`, `numberOfObjectives`; `WFG`: `k`, `l`, `m`; and so on), in the
positional order of the constructor that `ProblemSpec.args` matches by arity. Their defaults are
those of the no-arg problem. The table is the only hand-written metadata, one line per family, and a
completeness test checks it against the real constructors (an argument of the table must match the
type of the constructor of that arity).

Problems that cannot be built without external data (the TSP instances read
`resources/tspInstances/`) are described from their class, without dimensions, when instantiating
them fails.

### Binary and permutation problems get short names

The registry (`CURATED`) gets the problems of the other encodings that need no data of their own:
ZDT5 and OneZeroMax (binary, with the number of bits as argument) and the multi-objective TSP
instances of jMetal's `multiobjectivetsp.instance` package (permutation). A request names them like
any other (`problem: ZDT5`).

### The manifest

A new key, `problemCatalogue`, next to the existing one:

```yaml
problems: [DTLZ1, ..., ZDT6]          # unchanged: the names
problemCatalogue:
- name: DTLZ2
  family: DTLZ
  encoding: Double
  numberOfObjectives: 3
  numberOfVariables: 12
  arguments:
  - {name: numberOfVariables, type: integer, default: 12}
  - {name: numberOfObjectives, type: integer, default: 3}
- name: ZDT5
  family: ZDT
  encoding: Binary
  ...
```

`problems` is kept, as it is, so that a client written for 2.3 (Evolver-Studio reads it with
`sorted(manifest["problems"])`) keeps working with a 2.4 jar.

### Check the encoding before running

`TrainingRunner` and `SolveRunner` compare the encoding of every problem of the request with the
one of the base-level algorithm, and fail before the run with a message that says what is wrong:

```
Problem ZDT1 is Double-encoded, but the algorithm NSGA-II was configured with the Permutation
encoding: use a problem of that encoding (for instance KroAB100TSP).
```

instead of the `ClassCastException` that surfaces today in the middle of the run. Problems named by a
class that is not in the catalogue are checked through their interface, the same way.

## What it enables in Evolver-Studio

Not part of this change, but its reason: *Run algorithm* and *Training* can filter the problems by
the encoding of the algorithm, offer the arguments of a problem with its defaults, and know the number
of objectives without reading the reference front (which MOEA/D and RVEA need to choose their
weight vectors); *Explore › Problems* can be written; and S10 can use the same mechanism for a
problem of the user's.

## Plan

1. `ProblemDescriptor` and `ProblemRegistry.registeredProblems()`, with the table of arguments.
2. The problems of the other encodings in the registry.
3. `DescribeMain`: `problemCatalogue`.
4. The encoding check in both runners.
5. Tests: every registered problem has a descriptor, its encoding and dimensions agree with the
   instance, the table of arguments agrees with the constructors, the manifest has the key and keeps
   `problems`, and a mismatch fails before running with the message above.
6. Documentation: `docs/utilities/cli_tools.rst` (the manifest), the changelog, and this proposal's
   status.

## Decisions taken

- A new key, `problemCatalogue`, next to `problems`: no client written for 2.3 breaks.
- ZDT5 and OneZeroMax (binary) and the TSP instances of jMetal (permutation) are registered. Two of
  them are left out: `KroBC100TSP` and `KroBD100TSP` read `kroAC100.tsp` and `kroAD100.tsp`, files
  that do not exist (jMetal 7.7 bug: they should be `kroC100.tsp` and `kroD100.tsp`). Knapsack needs
  data of its own, and OneMax has one objective.
- The table of arguments covers ZDT, DTLZ, WFG, UF, LZ09, LSMOP, ZCAT and OneZeroMax. Writing the
  test that builds every problem with the defaults of the table found that UF5, UF6 and UF9 take
  more arguments than the rest of UF (a number of points and a tolerance): the table also accepts
  entries by problem name. LZ09's three arguments have no known default (they select the shape of
  the problem and jMetal does not expose them).

## Open questions

- Whether `problems` should eventually become the catalogue (a breaking change, for 3.0) instead of
  having two keys.
- Which other families deserve an argument table beyond the ones above (MaF, which Evolver
  does not register yet).
- Reference fronts per problem (the files of `resources/referenceFronts/` that fit it, with the
  number of objectives they are for): Evolver-Studio guesses them from the file name today
  (`DTLZ2.3D.csv`); the catalogue could list them, but the directory is not part of the jar.
