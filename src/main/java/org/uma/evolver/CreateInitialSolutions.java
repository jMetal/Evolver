package org.uma.evolver;

public enum CreateInitialSolutions {
    RANDOM {
        public String toString() {
            return "random";
        }
    },
    LATIN_HYPERCUBE_SAMPLING {
        public String toString() {
            return "latinHypercubeSampling";
        }
    },
    SCATTER_SEARCH {
        public String toString() {
            return "scatterSearch";
        }
    }
}
