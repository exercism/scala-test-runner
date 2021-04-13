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
tests_file="${input_dir}/src/test/scala/${exercise}Test.scala"
tests_results_file="${input_dir}/target/test-reports/TEST-${exercise}Test.xml"
original_tests_file="${input_dir}/src/test/scala/${exercise}Test.scala.original"
results_file="${output_dir}/results.json"
build_log_file="${output_dir}/build.log"

# Create the output directory if it doesn't exist
mkdir -p "${output_dir}"

echo "${slug}: testing..."

pushd "${input_dir}" > /dev/null

cp "${tests_file}" "${original_tests_file}"

# Enable all pending tests
sed -i 's/pending//g' "${tests_file}"

# TODO: figure out how to 
sbt test > "${build_log_file}"
# sbt -Dsbt.ivy.home=/opt/test-runner/.ivy2 -Divy.home=/opt/test-runner/.ivy2 -Dsbt.global.base=/opt/test-runner/.sbt/1.0 -Dsbt.boot.directory=/opt/test-runner/.sbt/boot test

mv -f "${original_tests_file}" "${tests_file}"

popd > /dev/null

# Write the results.json file
sbt "run ${build_log_file} ${tests_results_file} ${results_file}"

echo "${slug}: done"
