# AGENTS.md

## Project Overview
Evolver is a Java framework for the automatic configuration of multi-objective metaheuristics, built on top of jMetal.

## Tech Stack
- **Language**: Java 17+ (targeting Java 21 LTS)
- **Build Tool**: Maven
- **Framework**: jMetal 6.10

## Coding Guidelines
All Java code must follow the standards defined in:
- [JAVA_CODING_GUIDELINES.md](JAVA_CODING_GUIDELINES.md)

## Key Principles
1. Use modern Java features (records, sealed classes, pattern matching)
2. Prefer immutability and `Optional` over null
3. Follow clean code practices with meaningful naming
4. Write unit tests for new functionality

## Project Structure
```
src/
├── main/java/     # Production code
├── main/resources/ # Configuration files
└── test/java/     # Test code
```

## Build Commands
```bash
# Compile
mvn compile

# Run tests
mvn test

# Package
mvn package
```
