Analyze the results of a meta-optimization run in the given directory.

Directory to analyze: $ARGUMENTS

If no directory is specified, use `experimentation/`.

## Steps

### 1. Explore the structure

List the directory contents to understand what experiments are present:
- Subdirectories per algorithm / problem / configuration
- Result files (`.csv`, `.txt`, `.out`)
- Quality indicator files

### 2. Identify the result type

Determine what the files contain:
- **Found configurations**: base algorithm parameters (format `--param value`)
- **Quality indicators**: HV, EP, IGD per run
- **Pareto fronts**: FUN/VAR files

### 3. Compute statistics per experiment

For each set of results:
- Median and standard deviation of each indicator
- Best and worst value
- Number of completed vs expected runs

Display results in a Markdown table:

| Algorithm | Problem | Metric | Median | Std | Min | Max | N |
|-----------|---------|--------|--------|-----|-----|-----|---|

### 4. Identify the best configuration

If configuration files are present:
- Locate the configuration associated with the best value of the primary indicator
- Show the most relevant parameters (operators, population sizes, probabilities)

### 5. Detect anomalies

Flag if there are:
- Unfinished runs (empty or incomplete files)
- Significant outliers (values > 3σ from the median)
- Empty directories or unexpected structure

### 6. Executive summary

Close with 3–5 lines indicating:
- Which algorithm / configuration obtained the best results
- Whether there are clear patterns in the parameters of the best configurations
- What would be worth investigating further
