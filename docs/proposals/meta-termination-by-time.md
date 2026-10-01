# Termination of the meta-optimizers by computing time

**Status:** proposal (2026-10-01), nothing implemented. Open for review before any code.

## Motivation

Every meta-optimizer stops after a fixed number of meta-evaluations (`TerminationByEvaluations` in
`MetaAsyncNSGAIIBuilder`, `MetaSMPSOBuilder`, `MetaAsyncGeneticAlgorithmBuilder`; the same count in
`MetaNSGAIIBuilder`, `MetaSPEA2Builder`, `TreeNSGAII` and `RandomSearch`). A time limit is more
useful in practice, for three reasons:

- **The user's real budget is time.** The cost of a meta-evaluation depends on the base-level
  algorithm and on the configuration being evaluated (SMS-EMOA with hypervolume costs far more than
  NSGA-II), so the same number of meta-evaluations means very different waits.
- **Fair comparisons.** Evolver (flat or tree encoding) and irace do not spend the same time per
  evaluation; the equitable comparison is at equal wall-clock time.
- **Components with variable cost.** The hybrid mutation with a local LLM (branch
  `experiment/llm-mutation`) adds calls whose latency is large and variable. With an evaluation
  limit they would look free; with a time limit their cost is paid with fewer meta-evaluations,
  which is the only honest way to evaluate them. The hybrid experiment should therefore use this
  condition from the start.

## Proposal

A second, optional termination condition: **maximum computing time**. The two conditions can be
given together and the run stops at the first one that is met. Evaluations stay the default, so
existing requests, examples and tutorials do not change.

- **Mechanism.** jMetal's `TerminationByComputingTime` (jmetal-component) and, for both conditions
  at once, a composite that stops when any of them is met (to be checked whether jMetal already has
  one; otherwise a small class in `org.uma.evolver.meta`). The builders get a setter such as
  `setMaxComputingTime(Duration)` next to `maxEvaluations`; `TreeNSGAII` and `RandomSearch`, which
  have their own loops, check the clock at the end of each generation.
- **Granularity.** The meta-optimizers are parallel and generational, so the time is checked
  between generations: a run can exceed the limit by up to one generation. With 16 cores and long
  evaluations this can be noticeable; the report (below) states the real elapsed time, and the
  documentation states the overshoot.
- **Budget strategies** (`meta.strategy`, evaluations of the base-level algorithm) are not affected:
  only the stopping rule of the meta level changes.
- **CLI.** `cli.training` requests accept `maxComputingTime` (for example in seconds or as an ISO
  duration, to decide) as an alternative or in addition to the existing meta-evaluation limit.
  Evolver-Studio exposes it in the training page.
- **Convergence plots.** `scripts/plot_training_convergence.py` uses the meta-evaluation as the
  axis; add elapsed time as an alternative axis, so runs stopped by time can be compared. The
  indicator history needs a timestamp per generation.

## Reporting the stopping condition

`METADATA.txt` (written by `ConsolidatedOutputResults` and `TreeOutputResults`) already has the
configured `Max Evaluations` of the meta-optimizer and, appended at the end, a `--- Execution ---`
section with the wall-clock time. It must also say **why the run stopped and with what budget**,
for every run, including those stopped only by evaluations:

- in `--- Meta-Optimizer ---`: the configured limits, `Max Evaluations: 2000` (or `none`) and
  `Max Computing Time: 1h 0m 0s (3600000 ms)` (or `none`);
- in `--- Execution ---`: `Stopping condition: evaluations | computing time`, the meta-evaluations
  actually performed and the wall-clock time already written. With the two limits given, the one that
  stopped the run.

This makes a result stopped by time reproducible in intent (same limit) and comparable in fact (the
evaluations it reached). Studio and the analysis scripts that read `METADATA.txt` need the new
fields to be optional, so that old result directories are still readable.

A side finding in the same file: the line `Evaluation Strategy:` shows the default `toString` of the
object (`org.uma.evolver.meta.strategy.FixedEvaluationsStrategy@5e25a92e`), which says nothing about
the budget; the strategies should give a readable description. It can be fixed with this work.

## Risks

- **Reproducibility.** A run stopped by time does not give the same result on another machine or
  under another load. Mitigation: record the evaluations reached and keep the evaluation limit as the
  default for the tutorials and for validations that must be exact.
- **Termination in the asynchronous builders.** Check that the clock is read where the evaluation
  count is today and that in-flight evaluations are not lost.

## Plan (atomic commits on `develop`)

1. Maximum computing time in the builders and in `TreeNSGAII`/`RandomSearch`, with the composite
   condition, and tests (short limit, both limits, evaluations only).
2. `METADATA.txt`: limits and stopping condition in the two writers, readable description of the
   evaluation strategies, tests; changelog.
3. `cli.training`: the request field, documentation in `docs/utilities/cli_tools.rst`, bundled
   example request.
4. Convergence plot with elapsed time as an axis.
5. Evolver-Studio: the training page field, and the reading of the new metadata fields.

## Open questions

- Format of the limit in requests (seconds, ISO 8601 duration, or `1h30m`).
- Whether a time-only run should require a minimum number of generations.
- Whether the validation studies of the papers move to time budgets, and with which values.
