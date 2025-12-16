References
==========

This section contains bibliographic references for the methodologies and algorithms implemented in Evolver.

Ablation Analysis
-----------------

Core Methodology
~~~~~~~~~~~~~~~~

.. [Fawcett2016] Fawcett, C., & Hoos, H. H. (2016). An ablation study of parameter control mechanisms in multi-objective evolutionary algorithms. *Artificial Intelligence*, 245, 96-119.

.. [Birattari2002] Birattari, M., Stützle, T., Paquete, L., & Varrentrapp, K. (2002). A racing algorithm for configuring metaheuristics. In *Proceedings of the 4th Annual Conference on Genetic and Evolutionary Computation* (pp. 11-18).

.. [Lopez2016] López-Ibáñez, M., Dubois-Lacoste, J., Cáceres, L. P., Birattari, M., & Stützle, T. (2016). The irace package: Iterated racing for automatic algorithm configuration. *Operations Research Perspectives*, 3, 43-58.

Algorithm Configuration
~~~~~~~~~~~~~~~~~~~~~~~

.. [Hutter2011] Hutter, F., Hoos, H. H., & Leyton-Brown, K. (2011). Sequential model-based optimization for general algorithm configuration. In *Learning and Intelligent Optimization* (pp. 507-523). Springer.

.. [Eiben2011] Eiben, A. E., & Smit, S. K. (2011). Parameter tuning for configuring and analyzing evolutionary algorithms. *Swarm and Evolutionary Computation*, 1(1), 19-31.

.. [Nannen2007] Nannen, V., & Eiben, A. E. (2007). Relevance estimation and value calibration of evolutionary algorithm parameters. In *Proceedings of the 20th International Joint Conference on Artificial Intelligence* (pp. 975-980).

.. [Adenso2019] Adenso-Díaz, B., Lozano, S., & García-González, J. (2019). Understanding parameter importance in multi-objective optimization. In *Proceedings of the Genetic and Evolutionary Computation Conference* (pp. 573-581).

Feature Importance Analysis
---------------------------

Machine Learning Methods
~~~~~~~~~~~~~~~~~~~~~~~~~

.. [Breiman2001] Breiman, L. (2001). Random forests. *Machine Learning*, 45(1), 5-32.

.. [Strobl2007] Strobl, C., Boulesteix, A. L., Zeileis, A., & Hothorn, T. (2007). Bias in random forest variable importance measures: Illustrations, sources and a solution. *BMC Bioinformatics*, 8(1), 1-21.

.. [Altmann2010] Altmann, A., Toloşi, L., Sander, O., & Lengauer, T. (2010). Permutation importance: a corrected feature importance measure. *Bioinformatics*, 26(10), 1340-1347.

.. [Fisher2019] Fisher, A., Rudin, C., & Dominici, F. (2019). All models are wrong, but many are useful: Learning a variable's importance by studying an entire class of prediction models simultaneously. *Journal of Machine Learning Research*, 20(177), 1-81.

Hyperparameter Analysis
~~~~~~~~~~~~~~~~~~~~~~~

.. [Hutter2014] Hutter, F., Hoos, H., & Leyton-Brown, K. (2014). An efficient approach for assessing hyperparameter importance. In *International Conference on Machine Learning* (pp. 754-762).

.. [VanRijn2018] van Rijn, J. N., & Hutter, F. (2018). Hyperparameter importance across datasets. In *Proceedings of the 24th ACM SIGKDD International Conference on Knowledge Discovery & Data Mining* (pp. 2367-2376).

.. [Probst2019] Probst, P., Wright, M. N., & Boulesteix, A. L. (2019). Hyperparameters and tuning strategies for random forest. *Wiley Interdisciplinary Reviews: Data Mining and Knowledge Discovery*, 9(3), e1301.

Interaction Analysis
~~~~~~~~~~~~~~~~~~~~

.. [Friedman2001] Friedman, J. H. (2001). Greedy function approximation: a gradient boosting machine. *Annals of Statistics*, 1189-1232.

.. [Goldstein2015] Goldstein, A., Kapelner, A., Bleich, J., & Pitkin, E. (2015). Peeking inside the black box: Visualizing statistical learning with plots of individual conditional expectation. *Journal of Computational and Graphical Statistics*, 24(1), 44-65.

.. [Apley2020] Apley, D. W., & Zhu, J. (2020). Visualizing the effects of predictor variables in black box supervised learning models. *Journal of the Royal Statistical Society: Series B (Statistical Methodology)*, 82(4), 1059-1086.

Multi-Objective Optimization
----------------------------

Benchmark Problems
~~~~~~~~~~~~~~~~~~

.. [Zitzler2000] Zitzler, E., Deb, K., & Thiele, L. (2000). Comparison of multiobjective evolutionary algorithms: Empirical results. *Evolutionary Computation*, 8(2), 173-195.

.. [Deb2005] Deb, K., Thiele, L., Laumanns, M., & Zitzler, E. (2005). Scalable test problems for evolutionary multiobjective optimization. In *Evolutionary Multiobjective Optimization* (pp. 105-145). Springer.

Quality Indicators
~~~~~~~~~~~~~~~~~~

.. [Zitzler2003] Zitzler, E., Thiele, L., Laumanns, M., Fonseca, C. M., & Da Fonseca, V. G. (2003). Performance assessment of multiobjective optimizers: an analysis and review. *IEEE Transactions on Evolutionary Computation*, 7(2), 117-132.

.. [Ishibuchi2015] Ishibuchi, H., Masuda, H., Tanigaki, Y., & Nojima, Y. (2015). A study on performance evaluation ability of a modified inverted generational distance indicator. In *Proceedings of the 2015 Annual Conference on Genetic and Evolutionary Computation* (pp. 695-702).

BibTeX Entries
--------------

For researchers who need to cite these works, here are the complete BibTeX entries:

Ablation Analysis
~~~~~~~~~~~~~~~~~

.. code-block:: bibtex

   @article{fawcett2016ablation,
     title={An ablation study of parameter control mechanisms in multi-objective evolutionary algorithms},
     author={Fawcett, Chris and Hoos, Holger H},
     journal={Artificial Intelligence},
     volume={245},
     pages={96--119},
     year={2016},
     publisher={Elsevier}
   }

   @inproceedings{birattari2002racing,
     title={A racing algorithm for configuring metaheuristics},
     author={Birattari, Mauro and St{\"u}tzle, Thomas and Paquete, Luis and Varrentrapp, Klaus},
     booktitle={Proceedings of the 4th Annual Conference on Genetic and Evolutionary Computation},
     pages={11--18},
     year={2002}
   }

   @article{lopez2016irace,
     title={The irace package: Iterated racing for automatic algorithm configuration},
     author={L{\'o}pez-Ib{\'a}{\~n}ez, Manuel and Dubois-Lacoste, J{\'e}r{\'e}mie and C{\'a}ceres, Leslie P{\'e}rez and Birattari, Mauro and St{\"u}tzle, Thomas},
     journal={Operations Research Perspectives},
     volume={3},
     pages={43--58},
     year={2016},
     publisher={Elsevier}
   }

Feature Importance Analysis
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: bibtex

   @article{breiman2001random,
     title={Random forests},
     author={Breiman, Leo},
     journal={Machine Learning},
     volume={45},
     number={1},
     pages={5--32},
     year={2001},
     publisher={Springer}
   }

   @article{altmann2010permutation,
     title={Permutation importance: a corrected feature importance measure},
     author={Altmann, Andr{\'e} and Tolo{\c{s}}i, Laura and Sander, Oliver and Lengauer, Thomas},
     journal={Bioinformatics},
     volume={26},
     number={10},
     pages={1340--1347},
     year={2010},
     publisher={Oxford University Press}
   }

   @inproceedings{hutter2014efficient,
     title={An efficient approach for assessing hyperparameter importance},
     author={Hutter, Frank and Hoos, Holger and Leyton-Brown, Kevin},
     booktitle={International Conference on Machine Learning},
     pages={754--762},
     year={2014}
   }

Algorithm Configuration
~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: bibtex

   @article{hutter2011sequential,
     title={Sequential model-based optimization for general algorithm configuration},
     author={Hutter, Frank and Hoos, Holger H and Leyton-Brown, Kevin},
     journal={Learning and Intelligent Optimization},
     pages={507--523},
     year={2011},
     publisher={Springer}
   }

   @article{eiben2007parameter,
     title={Parameter tuning for configuring and analyzing evolutionary algorithms},
     author={Eiben, Agoston E and Smit, Selmar K},
     journal={Swarm and Evolutionary Computation},
     volume={1},
     number={1},
     pages={19--31},
     year={2011},
     publisher={Elsevier}
   }

Multi-Objective Benchmarks
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: bibtex

   @article{zitzler2000comparison,
     title={Comparison of multiobjective evolutionary algorithms: Empirical results},
     author={Zitzler, Eckart and Deb, Kalyanmoy and Thiele, Lothar},
     journal={Evolutionary Computation},
     volume={8},
     number={2},
     pages={173--195},
     year={2000},
     publisher={MIT Press}
   }

   @inproceedings{deb2005scalable,
     title={Scalable test problems for evolutionary multiobjective optimization},
     author={Deb, Kalyanmoy and Thiele, Lothar and Laumanns, Marco and Zitzler, Eckart},
     booktitle={Evolutionary Multiobjective Optimization},
     pages={105--145},
     year={2005},
     publisher={Springer}
   }