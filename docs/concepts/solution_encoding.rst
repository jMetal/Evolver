.. _solution-encoding:

Solution Encoding
=================

Evolver represents a base-level algorithm configuration as a solution of the meta-optimization problem. Two encodings are available: a **flat double encoding**, where every parameter is a real number in a fixed-length vector, and a **derivation tree encoding**, where only the parameters active for the current configuration appear, structured as a tree. This section describes the string format both encodings decode to, then covers each encoding in turn.

Configurations for Base-Level Metaheuristics
--------------------------------------------
All the base-level metaheuristics in Evolver are configured from a string containing all the parameters. This string has this scheme: "--parameterName1 parameterValue1 --parameterName2 parameterValue2 ... --parameterNameN parameterValueN". This string is parsed and the corresponding parameter values are set in the base-level metaheuristic.

Both encodings described below ultimately produce this string; they differ in how a solution of the meta-optimization problem is represented internally and decoded into it.

Flat Double Encoding
---------------------
Using the parameter string directly as a solution encoding would involve the design and implementation of specific mutation and crossover operators. Our approach, on the other hand, adopts a very simple scheme: each parameter is encoded as a real number. This approach simplifies the meta-optimization problem by making it continuous, allowing us to use any metaheuristic included in jMetal that can solve continuous optimization problems as a meta-optimizer. This includes not only evolutionary algorithms like NSGA-II, but also particle swarm optimization algorithms like SMPSO.

Encoding Scheme
~~~~~~~~~~~~~~~~
All parameters are encoded as real numbers in the range [0.0, 1.0], regardless of their original type. This uniform encoding allows the use of any continuous metaheuristic as a meta-optimizer.

The following list shows the type conversions:

**Boolean Parameters**:

    - ``[0.0, 0.5)`` → ``false``
    - ``[0.5, 1.0]`` → ``true``


**Categorical Parameters**:

    For a parameter with *N* possible values:

    - The range ``[0.0, 1.0]`` is divided into *N* equal-sized bins
    - Each bin corresponds to one category

Example with 3 categories (A, B, C):

    - ``[0.0, 0.333)`` → A
    - ``[0.333, 0.666)`` → B
    - ``[0.666, 1.0]`` → C

**Numerical Parameters**:

    For parameters with range ``[min, max]``:

    - ``value = min + (max - min) * encoded_value``

**Example**:

    Consider a configuration with:

    - A boolean parameter (e.g., ``useArchive``)
    - A categorical parameter with 3 values (e.g., ``crossover`` = {SBX, BLX, PCX})
    - A numerical parameter in ``[0, 100]`` (e.g., ``populationSize``)

    An encoded solution ``[0.7, 0.4, 0.25]`` would decode to:

    - ``useArchive = true`` (0.7 ≥ 0.5)
    - ``crossover = BLX`` (0.4 falls in second bin)
    - ``populationSize = 25`` (0 + (100 - 0) * 0.25)

Implications
~~~~~~~~~~~~
The adopted encoding scheme introduces two potential drawbacks that must be considered. First, all parameters in the parameter space are flattened in the encoding, and dependencies among parameters are not taken into account. This means that, for example, the uniform mutation perturbation will be included in any configuration, independently of whether the selected mutation operator is uniform mutation or another type. Second, encoding boolean and categorical parameters within the range [0.0, 1.0] can lead to situations where a mutation does not alter the decoded value of the parameter. For instance, if a variable representing a boolean parameter has a value of 0.2 and is changed to 0.4, the decoded value remains False in both cases. This is because values below 0.5 are decoded as False, while values of 0.5 or higher are decoded as True.

While these potential effects can be mitigated by increasing the probability of the mutation operator used by the meta-optimizer, it is important to note that applying a variation operator (such as a mutation) to a solution may not alter the decoded value of the parameters, so the original configuration might not be changed and evaluating it is useless.

An alternative encoding that eliminates both drawbacks — inactive variables and neutral mutations — is described next in :ref:`tree-encoding`.

.. _tree-encoding:

Derivation Tree Encoding
-------------------------

The flat double encoding described above is simple and lets any continuous optimizer act as a meta-optimizer, but it has two structural drawbacks:

- **Inactive variables.** All parameters—including those that only apply for one particular categorical choice—are always present in the vector. Varying the crossover distribution index when the selected crossover is *wholeArithmetic* (which has no distribution index) wastes evaluations and misleads the optimizer.
- **Neutral mutations.** Perturbing a double that encodes a boolean or categorical value often leaves the decoded configuration unchanged, making the variation operator effectively a no-op.

The **derivation tree encoding** eliminates both problems by representing an algorithm configuration as a tree whose structure is derived directly from the grammar defined by the parameter space. Only active parameters appear in the tree, and every node carries a typed value, so operators can be made aware of parameter semantics.

.. note::

   Operators, solution types, and utilities for the derivation tree encoding live in the
   ``org.uma.evolver.meta.encoding`` package. The meta-optimization problem classes for both
   encodings—``MetaOptimizationProblem`` (flat) and ``TreeMetaOptimizationProblem`` (tree)—
   share a common base class ``AbstractMetaOptimizationProblem`` and both reside in
   ``org.uma.evolver.meta.problem``.


From Grammar to Tree
~~~~~~~~~~~~~~~~~~~~~

A parameter space (see :doc:`parameter_spaces`) implicitly defines a context-free grammar:

- Each **categorical parameter** becomes a non-terminal with one production per valid value.
- Conditional parameters become the right-hand side of those productions.
- Global sub-parameters appear in every production of their parent.
- Numeric and boolean parameters are terminals.

The ``GrammarConverter`` utility can make this grammar explicit in BNF notation:

.. code-block:: java

   ParameterSpace space = new NSGAIIDoubleParameterSpace();
   System.out.println(GrammarConverter.toBnf(space));

A fragment of the output for NSGA-II might look like:

.. code-block:: none

   <start> ::= <algorithmResult> <createInitialSolutions> <offspringPopulationSize> <variation> <selection>
   <algorithmResult> ::= "population" | "externalArchive" <populationSizeWithArchive> <archiveType>
   <variation> ::= "crossover" <crossoverProbability> <crossover>
   <crossover> ::= "SBX" <sbxDistributionIndex> | "blxAlpha" <blxAlphaCrossoverAlpha> | "wholeArithmetic"
   <sbxDistributionIndex> ::= DOUBLE[5.0, 400.0]
   ...

A **derivation tree** is a complete, fully resolved parse of this grammar: every non-terminal has been expanded to exactly one production, and every terminal holds a concrete value.


Data Structures
~~~~~~~~~~~~~~~~

TreeNode
^^^^^^^^

``TreeNode`` (``org.uma.evolver.meta.encoding.solution.TreeNode``) is a single node in the derivation tree. Each node wraps one ``Parameter<?>`` object and stores its decoded value together with two child lists:

- **Global children** – always present, regardless of the node's value (mirrors global sub-parameters).
- **Conditional children** – present only for the production that was selected (mirrors conditional parameters for the chosen categorical value).

.. code-block:: java

   TreeNode node = ...;                      // obtained from a DerivationTreeSolution
   node.grammarSymbol();                     // parameter name, e.g. "crossover"
   node.type();                              // CATEGORICAL | DOUBLE | INTEGER | BOOLEAN
   node.value();                             // decoded value, e.g. "SBX"
   node.globalChildren();                    // always-present sub-parameters
   node.conditionalChildren();               // active conditional sub-parameters
   node.children();                          // globalChildren + conditionalChildren

Numeric nodes expose ``lowerBound()`` and ``upperBound()`` to support type-aware operators. Categorical nodes expose ``validValues()`` for the full list of alternatives.

``TreeNode.deepCopy()`` produces an independent copy of the entire subtree, which is used internally by the solution's ``copy()`` method.

DerivationTreeSolution
^^^^^^^^^^^^^^^^^^^^^^^

``DerivationTreeSolution`` (``org.uma.evolver.meta.encoding.solution.DerivationTreeSolution``) implements jMetal's ``Solution<TreeNode>`` interface, making it compatible with any evolutionary algorithm in the framework. It holds:

- A list of **root nodes** (one per top-level parameter in the parameter space).
- Objective and constraint arrays (same as any jMetal solution).
- An attribute map for extra metadata (rank, crowding distance, etc.).

The key method for meta-optimization is ``toParameterArray()``, which serialises the active nodes into the ``--paramName value`` string format expected by the base-level algorithm's ``parse()`` method:

.. code-block:: java

   DerivationTreeSolution solution = ...;
   String[] params = solution.toParameterArray();
   // e.g. ["--algorithmResult", "population", "--crossover", "SBX",
   //        "--crossoverProbability", "0.9", "--sbxDistributionIndex", "20.0", ...]

Only nodes that are part of the tree (i.e., active parameters) appear in the output. Inactive parameters are simply absent.

``allNodes()`` returns all nodes via depth-first traversal and is the primary input to the genetic operators. ``nodesBySymbol(String)`` finds all nodes with a given grammar symbol, used by the crossover operator to locate type-compatible swap points.


Generating Solutions
~~~~~~~~~~~~~~~~~~~~~

``TreeSolutionGenerator`` (``org.uma.evolver.meta.encoding.util.TreeSolutionGenerator``) traverses the parameter space hierarchy and produces random derivation tree solutions:

.. code-block:: java

   ParameterSpace space = new NSGAIIDoubleParameterSpace();
   TreeSolutionGenerator generator = new TreeSolutionGenerator(space);

   DerivationTreeSolution solution = generator.generate(2); // 2 objectives

For each top-level parameter, the generator calls ``generateSubtree(parameter)``, which:

1. For **categorical** parameters: picks a random value, generates global sub-parameter subtrees unconditionally, and generates conditional sub-parameter subtrees only for the chosen production.
2. For **numeric** parameters: draws a uniform random value within ``[min, max]``.
3. For **boolean** parameters: flips a fair coin.

``generateConditionalChildren(parameter, selectedValue)`` is exposed separately for use by the mutation operator when a categorical node changes its value (see :ref:`tree-mutation`).


Operators
~~~~~~~~~~

.. _tree-crossover:

SubtreeCrossover
^^^^^^^^^^^^^^^^

``SubtreeCrossover`` (``org.uma.evolver.meta.encoding.operator.SubtreeCrossover``) implements **Strongly Typed GP (STGP) crossover** (Montana, 1995 [#montana1995]_): the operator selects a random node in one parent and swaps its subtree with a node of the **same grammar symbol** in the other parent, guaranteeing that both offspring are valid configurations.

Algorithm:

1. Deep-copy both parents into ``child1`` and ``child2``.
2. With probability ``crossoverProbability``, select a random node ``n1`` from ``child1``.
3. Collect all nodes in ``child2`` whose ``grammarSymbol()`` equals that of ``n1`` (type-compatible candidates).
4. If no candidates exist, return the copies unchanged.
5. Pick a random candidate ``n2`` from ``child2``.
6. Swap the content of ``n1`` and ``n2``: value, global children, and conditional children.

Swapping content in-place (rather than re-wiring parent pointers) keeps the implementation simple and avoids the need to track parent references.

.. code-block:: java

   SubtreeCrossover crossover = new SubtreeCrossover(0.9);

   List<DerivationTreeSolution> parents = List.of(parent1, parent2);
   List<DerivationTreeSolution> offspring = crossover.execute(parents);


.. _tree-mutation:

TreeMutation
^^^^^^^^^^^^

``TreeMutation`` (``org.uma.evolver.meta.encoding.operator.TreeMutation``) applies one mutation event per individual, combining standard GP mutation (Koza, 1992 [#koza1992]_; Poli et al., 2008 [#poli2008]_) with polynomial mutation for numeric nodes:

1. With probability ``mutationProbability``, select a random node from ``allNodes()``.
2. Mutate it according to its type:

   - **DOUBLE** – apply polynomial mutation (Deb and Goyal, 1996 [#deb1996]_) within ``[lowerBound, upperBound]``.
   - **INTEGER** – apply polynomial mutation then round to the nearest integer.
   - **CATEGORICAL** – pick a *different* value uniformly at random, then regenerate the conditional subtree for that new value via ``TreeSolutionGenerator.generateConditionalChildren()``. Global children are preserved.
   - **BOOLEAN** – flip the value.

.. code-block:: java

   TreeSolutionGenerator generator = new TreeSolutionGenerator(space);
   TreeMutation mutation = new TreeMutation(
       0.1,   // probability
       20.0,  // distribution index for polynomial mutation
       generator
   );

   DerivationTreeSolution mutated = mutation.execute(solution);

Categorical mutation is the tree-specific operation: changing a production forces the conditional subtree to be regenerated from scratch, because the old sub-parameters may not exist in the new production. The global children—which are shared across all productions—are untouched.

Polynomial Mutation Details
""""""""""""""""""""""""""""

For a numeric node with value :math:`x` in :math:`[x_L, x_U]`, the perturbed value is:

.. math::

   x' = x + \delta_q \cdot (x_U - x_L)

where :math:`\delta_q` is drawn from a polynomial distribution controlled by the distribution index :math:`\eta`:

.. math::

   \delta_q = \begin{cases}
   (2u + (1-2u)(1-\delta_1)^{\eta+1})^{1/(\eta+1)} - 1 & \text{if } u \leq 0.5 \\
   1 - (2(1-u) + 2(u-0.5)(1-\delta_2)^{\eta+1})^{1/(\eta+1)} & \text{otherwise}
   \end{cases}

with :math:`\delta_1 = (x - x_L)/(x_U - x_L)`, :math:`\delta_2 = (x_U - x)/(x_U - x_L)`, and :math:`u \sim \mathcal{U}(0,1)`. Higher :math:`\eta` concentrates the perturbation near the current value; typical values are 10–30.


Grammar Validation
~~~~~~~~~~~~~~~~~~~

``GrammarConverter.validate(solution)`` checks a derivation tree for structural consistency:

- Categorical nodes hold a value that appears in ``validValues()``.
- Numeric nodes hold values within their declared range.
- Boolean nodes hold a ``Boolean`` value.

It returns a list of error strings (empty when the solution is valid) and is useful for testing operators:

.. code-block:: java

   List<String> errors = GrammarConverter.validate(offspring.get(0));
   assert errors.isEmpty() : "Invalid offspring: " + errors;


Meta-Optimization with the Tree Encoding
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``TreeMetaOptimizationProblem`` (``org.uma.evolver.meta.problem.TreeMetaOptimizationProblem``)
extends ``AbstractMetaOptimizationProblem`` and operates on ``DerivationTreeSolution``.
It shares the full evaluation pipeline with ``MetaOptimizationProblem``; the only
encoding-specific hooks are ``createSolution()`` and ``toParameterArray()``. The evaluation flow is:

1. Call ``solution.toParameterArray()`` to obtain the active parameter string.
2. Run the base-level algorithm with those parameters on each training problem.
3. Compute the configured quality indicators and assign them as objective values.

The end-to-end wiring is illustrated in the ``TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D`` example:

.. code-block:: java

   var parameterSpace = new NSGAIIDoubleParameterSpace();
   var generator = new TreeSolutionGenerator(parameterSpace);

   var problem = new TreeMetaOptimizationProblem<>(
       baseAlgorithm, trainingProblems, referenceFronts,
       List.of(epsilonIndicator, hvIndicator),
       evaluationBudgetStrategy, runs, generator);

   var crossover = new SubtreeCrossover(0.9);
   var mutation  = new TreeMutation(0.1, 20.0, generator);

   // … wire into any jMetal meta-optimizer that accepts Solution<TreeNode>


Choosing an Encoding
---------------------

+-----------------------------------+---------------------+-----------------------+
| Property                          | Flat double         | Derivation tree       |
+===================================+=====================+=======================+
| Inactive variables                | Yes                 | No                    |
+-----------------------------------+---------------------+-----------------------+
| Neutral mutations                 | Yes (cat./bool.)    | No                    |
+-----------------------------------+---------------------+-----------------------+
| Meta-optimizer compatibility      | Any continuous      | Must support TreeNode |
+-----------------------------------+---------------------+-----------------------+
| Operator complexity               | Standard SBX / PM   | STGP crossover + PM   |
+-----------------------------------+---------------------+-----------------------+
| Genotype–phenotype transparency   | Low                 | High                  |
+-----------------------------------+---------------------+-----------------------+

The flat encoding remains the default because it allows any jMetal continuous optimizer to be used as a meta-optimizer without modification. The tree encoding is the preferred choice when the parameter space has deep conditional structure and inactive variables are a concern.

For more details on how these encodings are used in practice, see the :doc:`evaluation` documentation.


References
----------

.. [#montana1995] Montana, D. J. (1995). Strongly typed genetic programming. *Evolutionary Computation*, 3(2), 199–230. https://doi.org/10.1162/evco.1995.3.2.199

.. [#koza1992] Koza, J. R. (1992). *Genetic Programming: On the Programming of Computers by Means of Natural Selection*. MIT Press.

.. [#poli2008] Poli, R., Langdon, W. B., & McPhee, N. F. (2008). *A Field Guide to Genetic Programming*. Lulu Press.

.. [#deb1996] Deb, K., & Goyal, M. (1996). A combined genetic adaptive search (GeneAS) for engineering design. *Computer Science and Informatics*, 26(4), 30–45.
