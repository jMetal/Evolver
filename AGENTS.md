# AGENTS.md

This file provides guidance to AI agents working on this repository.
For Claude Code, the primary reference is `CLAUDE.md`. This file adds pointers for other agents.

## Project

Evolver is a Java framework for automatically configuring multi-objective metaheuristics using a
two-level (meta/base) optimization approach built on [jMetal 7.4](https://github.com/jMetal/jMetal).

See `CLAUDE.md` for the full project overview, architecture, and build/test commands.

## Coding Standards

All Java coding standards are defined in [`JAVA_CODING_GUIDELINES.md`](JAVA_CODING_GUIDELINES.md):

- §0 — Google Java Style (formatting, naming)
- §1 — Records for DTOs
- §2 — Pattern matching and switch expressions
- §3 — Optional instead of null
- §4 — Streams API
- §5 — Try-with-resources
- §6 — Single return point with guard clauses
- §7 — Single Responsibility Principle
- §8 — Javadoc
- §9 — Specific exceptions
- §10 — Immutability by default
- §11 — `var` usage
- §12 — Maven project structure
- §13 — Testing (JUnit 6, Given-When-Then, AAA, Mockito, parameterized, integration tests)

## Git Conventions

All commit and branching conventions are defined in [`GIT_GUIDELINES.md`](GIT_GUIDELINES.md)
(Conventional Commits, atomic commits, allowed types).

## Key Pointers

- Parameter spaces: YAML files in `src/main/resources/parameterSpaces/`
- Default configurations: `src/main/resources/defaultConfigurations/`
- Test fixtures: `src/test/resources/parameterSpaces/`
- Use builders (e.g., `MetaNSGAIIBuilder`) for complex object construction
- New algorithms must extend jMetal components and be configurable via `ParameterSpace`
- jMetal quality indicators are used as optimization objectives (Epsilon, NormalizedHypervolume)
- Temporary output directories (`experimentation/`, `results/`, `scripts/`) must not be committed
