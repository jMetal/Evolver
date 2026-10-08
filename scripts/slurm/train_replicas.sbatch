#!/usr/bin/env bash
#
# One independent replication of an Evolver training, as a task of a slurm job array (tutorial
# E18, docs/tutorials/independent_replications.rst). Task i runs
#
#   java -cp <jar> org.uma.evolver.cli.training.TrainingRunnerMain <request.yaml> \
#       --output-dir <study-dir>/runNN          (NN = i with two digits: run01, run02, ...)
#
# so that every replication shares the same request and writes a normal training output directory,
# the input of scripts/plot_training_convergence.py and scripts/training_replicas.py.
#
# Submit it with submit_replicas.sh (it creates logs/ and sets the array, the cores and the name):
#
#   scripts/slurm/submit_replicas.sh Evolver-<version>-jar-with-dependencies.jar \
#       request.yaml results/my-study 15 40
#
# Without slurm the same script runs one replication by hand, in the current directory:
#
#   TASK=3 bash scripts/slurm/train_replicas.sbatch <jar> <request.yaml> <study-dir>
#
# Run it from a directory with the jar, the resources/ directory of Evolver (the reference fronts:
# the parameter spaces and the bundled base-level and meta-optimizer files are inside the jar), the
# request and any base-level or meta-optimizer file of your own that it references (by a path
# relative to that directory). The cores of a task (--cpus-per-task) must be the numberOfCores of
# the meta-optimizer file: the meta-optimizer uses that many, whatever slurm gives it.
#
# ---- Adapt to your cluster ---------------------------------------------------------------------
# The #SBATCH lines and the three variables below depend on the cluster. sbatch options given on
# the command line (submit_replicas.sh gives --array, --cpus-per-task and --job-name) override them.
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=40
#SBATCH --mem=64gb
#SBATCH --time=100:00:00
#SBATCH --output=logs/%x_%A_%a.out
#SBATCH --error=logs/%x_%A_%a.err
##SBATCH --constraint=cal

# How to put Java (21 or later) on the path, e.g. "module load java/jdk-25"; empty if it already is.
JAVA_SETUP=${JAVA_SETUP:-"module load java/jdk-25"}
# A fast local disk of the node to run on (the result is copied back); empty to run in place.
SCRATCH_DIR=${SCRATCH_DIR:-${LOCALSCRATCH:+$LOCALSCRATCH/$USER/${SLURM_JOB_ID:-local}_${SLURM_ARRAY_TASK_ID:-0}}}
# Java options, e.g. "-Xmx48g".
JAVA_OPTS=${JAVA_OPTS:-}
# ------------------------------------------------------------------------------------------------

set -euo pipefail

if [ $# -ne 3 ]; then
    echo "Usage: train_replicas.sbatch <jar> <request.yaml> <study-dir>" >&2
    exit 2
fi
JAR=$1
REQUEST=$2
STUDY=$3
TASK=${SLURM_ARRAY_TASK_ID:-${TASK:?"set TASK (the replication number) when running without slurm"}}
RUN=$(printf "run%02d" "$TASK")

if [ -n "$JAVA_SETUP" ] && [ -n "${SLURM_JOB_ID:-}" ]; then
    eval "$JAVA_SETUP"
fi

HERE=$PWD
if [ -n "$SCRATCH_DIR" ]; then
    mkdir -p "$SCRATCH_DIR/$(dirname "$REQUEST")"
    cp "$JAR" "$SCRATCH_DIR/"
    cp -R resources "$SCRATCH_DIR/"
    cp -R "$(dirname "$REQUEST")/." "$SCRATCH_DIR/$(dirname "$REQUEST")/"
    cd "$SCRATCH_DIR"
    JAR=$(basename "$JAR")
fi

echo "Replication $RUN of $REQUEST on $(hostname), $(nproc 2>/dev/null || sysctl -n hw.ncpu) cores, $(date)"
# shellcheck disable=SC2086
time java $JAVA_OPTS -cp "$JAR" org.uma.evolver.cli.training.TrainingRunnerMain \
    "$REQUEST" --output-dir "$STUDY/$RUN"

if [ -n "$SCRATCH_DIR" ]; then
    mkdir -p "$HERE/$STUDY"
    rm -rf "${HERE:?}/$STUDY/$RUN"
    cp -R "$STUDY/$RUN" "$HERE/$STUDY/"
    cd "$HERE"
    rm -rf "${SCRATCH_DIR:?}"
fi
echo "Replication $RUN done: $STUDY/$RUN"
