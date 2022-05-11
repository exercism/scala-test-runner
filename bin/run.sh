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

exercise=$(echo "${slug}" | sed -E 's/(^|-)([a-z])/\U\2/g')

test_runner_jar=/opt/test-runner/target/scala-2.13/TestRunner-assembly-0.1.0-SNAPSHOT.jar

workdir=/tmp/exercise
workdir_target=/tmp/exercise/target

results_file="${output_dir}/results.json"
build_log_file="${output_dir}/build.log"
runner_log_file="${output_dir}/runner.log"
tests_results_file="${workdir}/test-reports/TEST-${exercise}Test.xml"

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

# compile source and tests
scalac -classpath "${test_runner_jar}" -d "${workdir_target}" "${workdir}"/src/main/scala/* "${workdir}"/src/test/scala/* &> "${build_log_file}"

# run tests
scala -classpath "${test_runner_jar}" org.scalatest.tools.Runner -R "${workdir_target}" -u "${workdir}"/test-reports

# Write the results.json file in the exercism format
java -jar "${test_runner_jar}" "${build_log_file}" "${tests_results_file}" "${results_file}" &> "${runner_log_file}"

# change workdir back to the original input_dir in the final results file
sed -i "s~${workdir}~${input_dir}~g" "${results_file}"

echo "${slug}: done"
