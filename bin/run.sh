#!/usr/bin/env bash

# Synopsis:
# Run the test runner on a solution.

# Arguments:
# $1: exercise slug
# $2: absolute path to solution folder
# $3: absolute path to output directory

# Output:
# Writes the test results to a results.json file in the passed-in output directory.
# The test results are formatted according to the specifications at https://github.com/exercism/docs/blob/main/building/tooling/test-runners/interface.md

# Example:
# ./bin/run.sh two-fer /absolute/path/to/two-fer/solution/folder/ /absolute/path/to/output/directory/

# If any required arguments is missing, print the usage and exit
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
    echo "usage: ./bin/run.sh exercise-slug /absolute/path/to/two-fer/solution/folder/ /absolute/path/to/output/directory/"
    exit 1
fi

slug="$1"
input_dir="${2%/}"
output_dir="${3%/}"

test_runner_jar=/opt/test-runner/target/test-runner.jar

# Class data sharing archives, dumped into the image by bin/warmup.sh. They hold
# the compiler's and the runner's classes ready to be memory-mapped, which is
# where most of a short run's time used to go. Should either ever go missing or
# stop matching, the JVM ignores it and the run is merely slow again.
scalac_archive=/opt/test-runner/cds/scalac.jsa
runner_archive=/opt/test-runner/cds/runner.jsa

# ScalaTest's lazy vals were compiled against the old `scala.runtime.LazyVals`
# API, which reaches for `sun.misc.Unsafe`; on JDK 24 and later every JVM that
# loads them prints four lines about it. The lines land wherever the run's
# output is going - the console, or `build.log`, which is read back as the
# message of a failed compile - so they are turned off rather than filtered out.
# Nothing here can fix the call itself short of a ScalaTest built on Scala 3.9.
unsafe_warning_off=--sun-misc-unsafe-memory-access=allow

workdir=/tmp/exercise
workdir_target="${workdir}/target"
workdir_test_sources="${workdir}/src/test/scala"
test_results_file="${workdir}/test-results.json"

results_file="${output_dir}/results.json"
build_log_file="${output_dir}/build.log"
runner_log_file="${output_dir}/runner.log"

# Create the output directory if it doesn't exist
mkdir -p "${output_dir}"

# ensure a clean workdir
rm -rf "${workdir}"
mkdir -p "${workdir}"
mkdir -p "${workdir_target}"

echo
echo "${slug}: testing..."

cp -R "${input_dir}"/src "${workdir}"

# Enable all pending tests
sed -i 's/pending//g' "${workdir}"/src/test/scala/*

# Compile source and tests.
#
# `-XX:TieredStopAtLevel=1` holds the JIT at its first tier. Compiling a handful
# of small files never runs long enough for the optimising tier to earn back
# what it costs to reach it, and the same goes for reading a results file and
# writing it out in another shape below. The test run in between is left alone:
# that is the one JVM here running a student's own code, which may well hold a
# loop worth optimising.
scalac -J-XX:SharedArchiveFile="${scalac_archive}" -J-XX:TieredStopAtLevel=1 \
    "-J${unsafe_warning_off}" \
    -classpath "${test_runner_jar}" -d "${workdir_target}" \
    "${workdir}"/src/main/scala/* "${workdir}"/src/test/scala/* &> "${build_log_file}"

# run tests, recording what each test reported, printed, and ran to check it.
# The test sources are read back for that last one, so they are passed along.
java -XX:SharedArchiveFile="${runner_archive}" "${unsafe_warning_off}" \
    -classpath "${test_runner_jar}" TestRun "${workdir_target}" "${test_results_file}" "${workdir_test_sources}"

# Write the results.json file in the exercism format
java -XX:SharedArchiveFile="${runner_archive}" -XX:TieredStopAtLevel=1 "${unsafe_warning_off}" \
    -jar "${test_runner_jar}" "${build_log_file}" "${test_results_file}" "${results_file}" &> "${runner_log_file}"

# change workdir back to the original input_dir in the final results file
sed -i "s~${workdir}~${input_dir}~g" "${results_file}"

echo "${slug}: done"
