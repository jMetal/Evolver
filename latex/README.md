# Compact LaTeX Parameter Space Tables

This directory contains automatically generated compact LaTeX tables for jMetal Evolver parameter spaces, designed for scientific papers.

## Generated Files

### Individual Algorithm Tables (Compact Format)
- `table_nsgaiidouble.tex` - NSGA-II with double precision (basic)
- `table_nsgaiidoublefull.tex` - NSGA-II with double precision (full parameter space)
- `table_moeaddouble.tex` - MOEA/D with double precision
- `table_moeaddoublefull.tex` - MOEA/D with double precision (full parameter space)
- `table_smsemoadouble.tex` - SMS-EMOA with double precision
- `table_smsemoadoublefull.tex` - SMS-EMOA with double precision (full parameter space)
- `table_mopso.tex` - Multi-Objective Particle Swarm Optimization
- `table_mopsofull.tex` - MOPSO (full parameter space)
- `table_rdemoeadouble.tex` - RDEMOEA with double precision
- `table_nsgaiibinary.tex` - NSGA-II for binary problems
- `table_nsgaiipermutation.tex` - NSGA-II for permutation problems
- `table_smsemoabinary.tex` - SMS-EMOA for binary problems
- `table_smsemoapermutation.tex` - SMS-EMOA for permutation problems

## Compact Table Format

Each table uses a paper-friendly compact format inspired by the AMOPSO paper style:

### Domain Notation
- `\Realdomain{a, b}`: Real-valued parameters in range [a, b] ⊂ ℝ
- `\Intdomain{a, b}`: Integer-valued parameters in range [a, b] ⊂ ℕ
- `\Catdomain{opt1, opt2, ...}`: Categorical parameters with discrete options

### Conditional Parameters
Conditional parameters are shown with **if** clauses:
```latex
mutationProbability & \Realdomain{0.0, 1.0} \hspace{3em}\textbf{if} mutation$=$polynomial
```

### Table Structure
```latex
\begin{table}[!tb]
\caption{Parameter space of AlgorithmName.}
\label{tab:params_algorithmname}
\centering
\resizebox{\textwidth}{!}{%
\begin{tabular}{r@{\hskip 1em}p{25em}}
\toprule
\bf Parameter & \bf Domain \\
\midrule
% Parameters here
\bottomrule
\end{tabular}}
\end{table}
```

## Usage in Scientific Papers

### Including Tables in Your Document

Each `.tex` file is a complete LaTeX document that can be compiled independently, or you can extract just the table for inclusion in your paper.

To include a table in your paper:
1. Copy the table definition (from `\begin{table}` to `\end{table}`)
2. Add required packages to your preamble
3. Reference with `Table~\ref{tab:params_algorithmname}`

### Required LaTeX Packages

```latex
\usepackage{booktabs}      % Professional table formatting
\usepackage{array}         % Advanced column formatting  
\usepackage{amsmath}       % Math symbols
\usepackage{amssymb}       % Additional math symbols
\usepackage{graphicx}      % For \resizebox
```

### Domain Commands

Add these commands to your preamble:
```latex
\newcommand{\Realdomain}[1]{\ensuremath{[#1]\subset\mathbb{R}}}
\newcommand{\Intdomain}[1]{\ensuremath{[#1]\subset\mathbb{N}}}
\newcommand{\Catdomain}[1]{\ensuremath{\{\;}#1\ensuremath{\;\}}}
```

## Compilation

### Compile Individual Tables
```bash
# Compile a specific table
pdflatex table_nsgaiidouble.tex

# Or use the compile script
./compile.sh table_nsgaiidouble
```

### Compile All Tables
```bash
./compile_all_tables.sh
```

### Clean Up
```bash
./cleanup.sh
```

## Regenerating Tables

To regenerate tables after modifying YAML parameter spaces:

```bash
# Generate all tables
python yaml_to_latex.py --all

# Generate single table  
python yaml_to_latex.py ../src/main/resources/parameterSpaces/NSGAIIDoubleFull.yaml
```

## Example Paper Integration

```latex
\documentclass{article}
\usepackage{booktabs}
\usepackage{array}
\usepackage{amsmath}
\usepackage{amssymb}
\usepackage{graphicx}

% Domain notation commands
\newcommand{\Realdomain}[1]{\ensuremath{[#1]\subset\mathbb{R}}}
\newcommand{\Intdomain}[1]{\ensuremath{[#1]\subset\mathbb{N}}}
\newcommand{\Catdomain}[1]{\ensuremath{\{\;}#1\ensuremath{\;\}}}

\begin{document}

\section{Experimental Setup}

The parameter space for NSGA-II is shown in Table~\ref{tab:params_nsgaiidouble}.

% Copy table definition from table_nsgaiidouble.tex here
\begin{table}[!tb]
\caption{Parameter space of NSGAIIDouble.}
\label{tab:params_nsgaiidouble}
% ... rest of table
\end{table}

\end{document}
```

## Features

- **Paper-ready**: Tables automatically resize to fit page width
- **Compact notation**: Efficient use of space with mathematical notation
- **Conditional parameters**: Clear indication of parameter dependencies
- **Professional formatting**: Uses booktabs for publication-quality tables
- **Complete documents**: Each table can be compiled independently for preview