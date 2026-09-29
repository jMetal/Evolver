#!/bin/bash

## Installs the bundled irace package in ./R, and runs irace on a scenario.
##
## Usage: ./run.sh <scenario file> <run number>
##
## The run number distinguishes replications of irace: it sets irace's seed and the execution
## directory, execdir-<run number>, next to the scenario file. The number of cores irace uses to
## run experiments in parallel is 8 by default; set N_CPUS to change it, e.g.
##     N_CPUS=16 ./run.sh scenario-NSGAII.txt 1

SCENARIO=$1
RUN=$2
shift 2
N_CPUS=${N_CPUS:-8}
let SEED=1234567+RUN
EXECDIR=$(dirname ${SCENARIO})/execdir-${RUN}
IRACE_PARAMS="--scenario ${SCENARIO} --debug-level 1 --parallel $N_CPUS --seed ${SEED} --exec-dir=${EXECDIR}"

RPACKAGE="./irace_4.4.3.tar.gz"

# Install irace in ./R
if [ ! -r $RPACKAGE ]; then
    echo "cannot read $RPACKAGE"
    exit 1
fi
RLIBDIR="$(pwd)/R/"
mkdir -p $RLIBDIR
R CMD INSTALL $RPACKAGE --library=$RLIBDIR
export R_LIBS="$RLIBDIR:$R_LIBS"
irace="${RLIBDIR}/irace/bin/irace"
if [ ! -x $irace ]; then
    echo "cannot execute $irace"
    exit 1
fi
export PATH="$(pwd)/":${PATH}

# Run irace. The target runner reads the reference fronts from resources/referenceFronts, relative
# to the execution directory, so a link to ./resources is created there.
echo "$irace ${IRACE_PARAMS} 1> ${EXECDIR}/irace.stdout.txt 2> ${EXECDIR}/irace.stderr.txt"
mkdir -p $EXECDIR \
    && ln -fs $(pwd)/resources ${EXECDIR}/ \
    && $irace ${IRACE_PARAMS} 1> ${EXECDIR}/irace.stdout.txt 2> ${EXECDIR}/irace.stderr.txt
