"""
This module contains the Evolver components that may be used in the dashboard.
Ultimately, this module will be replaced by calls to the Evolver JAR file, to
dinamically load the available components.
"""

meta_optimizers = {
    "NSGA-II": "NSGAII",
    "Asynchronous NSGA-II": "ASYNCNSGAII",
    "Generational Genetic Algorithm": "GGA",
    "Speed-constrained Multi-objective Particle Swarm Optimization": "SMPSO",
}

configurable_algorithms = {
    "NSGA-II": "NSGAII",
    "NSGA-II with Diferential": "NSGAIIDE",
    "MOPSO": "MOPSO",
    "MOEA/D": "MOEAD",
    "SMS-EMOA": "SMSEMOA",
}

problems = {
    "ZDT1": "org.uma.jmetal.problem.multiobjective.zdt.ZDT1",
    "ZDT2": "org.uma.jmetal.problem.multiobjective.zdt.ZDT2",
    "ZDT3": "org.uma.jmetal.problem.multiobjective.zdt.ZDT3",
    "ZDT4": "org.uma.jmetal.problem.multiobjective.zdt.ZDT4",
    "ZDT6": "org.uma.jmetal.problem.multiobjective.zdt.ZDT6",
    "DTLZ1": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1",
    "DTLZ2": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2",
    "DTLZ3": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3",
    "DTLZ4": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4",
    "DTLZ5": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5",
    "DTLZ6": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6",
    "DTLZ7": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7",
    "WFG1": "org.uma.jmetal.problem.multiobjective.wfg.WFG1",
    "WFG2": "org.uma.jmetal.problem.multiobjective.wfg.WFG2",
    "WFG3": "org.uma.jmetal.problem.multiobjective.wfg.WFG3",
    "WFG4": "org.uma.jmetal.problem.multiobjective.wfg.WFG4",
    "WFG5": "org.uma.jmetal.problem.multiobjective.wfg.WFG5",
    "WFG6": "org.uma.jmetal.problem.multiobjective.wfg.WFG6",
    "WFG7": "org.uma.jmetal.problem.multiobjective.wfg.WFG7",
    "WFG8": "org.uma.jmetal.problem.multiobjective.wfg.WFG8",
    "WFG9": "org.uma.jmetal.problem.multiobjective.wfg.WFG9",
    "UF1": "org.uma.jmetal.problem.multiobjective.uf.UF1",
    "UF2": "org.uma.jmetal.problem.multiobjective.uf.UF2",
    "UF3": "org.uma.jmetal.problem.multiobjective.uf.UF3",
    "UF4": "org.uma.jmetal.problem.multiobjective.uf.UF4",
    "UF5": "org.uma.jmetal.problem.multiobjective.uf.UF5",
    "UF6": "org.uma.jmetal.problem.multiobjective.uf.UF6",
    "UF7": "org.uma.jmetal.problem.multiobjective.uf.UF7",
    "UF8": "org.uma.jmetal.problem.multiobjective.uf.UF8",
    "UF9": "org.uma.jmetal.problem.multiobjective.uf.UF9",
    "UF10": "org.uma.jmetal.problem.multiobjective.uf.UF10",
    # Real world problems
    "Engineering": "org.uma.jmetal.problem.multiobjective.rwa.Goel2007",
}

# This paths are relative to the resources folder
referenceFront = {
    "ZDT1": "referenceFronts/ZDT1.csv",
    "ZDT2": "referenceFronts/ZDT2.csv",
    "ZDT3": "referenceFronts/ZDT3.csv",
    "ZDT4": "referenceFronts/ZDT4.csv",
    "ZDT6": "referenceFronts/ZDT6.csv",
    "DTLZ1": "referenceFronts/DTLZ1.3D.csv",
    "DTLZ2": "referenceFronts/DTLZ2.3D.csv",
    "DTLZ3": "referenceFronts/DTLZ3.3D.csv",
    "DTLZ4": "referenceFronts/DTLZ4.3D.csv",
    "DTLZ5": "referenceFronts/DTLZ5.3D.csv",
    "DTLZ6": "referenceFronts/DTLZ6.3D.csv",
    "DTLZ7": "referenceFronts/DTLZ7.3D.csv",
    "WFG1": "referenceFronts/WFG1.2D.csv",
    "WFG2": "referenceFronts/WFG2.2D.csv",
    "WFG3": "referenceFronts/WFG3.2D.csv",
    "WFG4": "referenceFronts/WFG4.2D.csv",
    "WFG5": "referenceFronts/WFG5.2D.csv",
    "WFG6": "referenceFronts/WFG6.2D.csv",
    "WFG7": "referenceFronts/WFG7.2D.csv",
    "WFG8": "referenceFronts/WFG8.2D.csv",
    "WFG9": "referenceFronts/WFG9.2D.csv",
    "UF1": "referenceFronts/UF1.csv",
    "UF2": "referenceFronts/UF2.csv",
    "UF3": "referenceFronts/UF3.csv",
    "UF4": "referenceFronts/UF4.csv",
    "UF5": "referenceFronts/UF5.csv",
    "UF6": "referenceFronts/UF6.csv",
    "UF7": "referenceFronts/UF7.csv",
    "UF8": "referenceFronts/UF8.csv",
    "UF9": "referenceFronts/UF9.csv",
    "UF10": "referenceFronts/UF10.csv",
    # Real world problems
    "Engineering": "referenceFronts/Goel2007.csv",
}

quality_indicators = {
    "Hypervolume": "HV",
    "Epsilon": "EP",
    "Spread": "SP",
    "Generational Distance": "GD",
    "Inverted Generational Distance": "IGD",
    "Inverted Generational Distance Plus": "IGD+",
    "Normalized Hypervolume": "NHV",
    "Generalized Spread": "GSPREAD",
}
