#!/usr/bin/env bash
#
# Submits the independent replications of an Evolver training as a slurm job array of
# train_replicas.sbatch (tutorial E18): one task per replication, each with <cpus> cores.
#
#   scripts/slurm/submit_replicas.sh <jar> <request.yaml> <study-dir> <replications> <cpus> \
#       [more sbatch options]
#
#   scripts/slurm/submit_replicas.sh Evolver-2.5-jar-with-dependencies.jar \
#       studies/zdt/request.yaml results/zdt 15 40 --constraint=cal
#
# <cpus> must be the numberOfCores of the meta-optimizer file of the request. The results are in
# <study-dir>/run01 .. runNN, the logs in logs/. Replications already finished (with a
# results.yaml) are not submitted again, so the same command resubmits only the missing ones.

set -euo pipefail

if [ $# -lt 5 ]; then
    echo "Usage: submit_replicas.sh <jar> <request.yaml> <study-dir> <replications> <cpus> [sbatch options]" >&2
    exit 2
fi
JAR=$1; REQUEST=$2; STUDY=$3; REPLICATIONS=$4; CPUS=$5
shift 5
HERE=$(cd "$(dirname "$0")" && pwd)

for file in "$JAR" "$REQUEST"; do
    [ -f "$file" ] || { echo "No such file: $file" >&2; exit 1; }
done
[ -d resources ] || { echo "No resources/ directory here (the reference fronts)" >&2; exit 1; }

MISSING=()
for i in $(seq 1 "$REPLICATIONS"); do
    [ -f "$STUDY/$(printf "run%02d" "$i")/results.yaml" ] || MISSING+=("$i")
done
if [ ${#MISSING[@]} -eq 0 ]; then
    echo "All $REPLICATIONS replications of $STUDY are done"
    exit 0
fi
ARRAY=$(IFS=,; echo "${MISSING[*]}")

mkdir -p logs
echo "Submitting ${#MISSING[@]} replications of $REQUEST ($CPUS cores each) to $STUDY: tasks $ARRAY"
sbatch --array="$ARRAY" --cpus-per-task="$CPUS" --job-name="evolver_$(basename "$STUDY")" "$@" \
    "$HERE/train_replicas.sbatch" "$JAR" "$REQUEST" "$STUDY"
