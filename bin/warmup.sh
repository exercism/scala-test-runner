#!/usr/bin/env bash

# Synopsis:
# Build the class data sharing archives that bin/run.sh starts its JVMs with.
# Run once, while the image is being built - not once per solution.

# Most of a run went on loading classes rather than on the student's code. The
# compiler alone loads around 6500 of them, and every run parsed, verified and
# linked all 6500 from scratch. An archive holds those classes in the form the
# JVM would otherwise have to derive each time, ready to be memory-mapped
# instead. That, and holding the JIT at its first tier, takes about three fifths
# off a compile.
#
# The only way to dump an archive is to do a real run, so that is what this is:
# a throwaway exercise, compiled and tested exactly the way bin/run.sh does it.
# What the exercise computes does not matter; what matters is that it is a
# ScalaTest suite written the way the track's exercises are written, so that the
# classes it pulls in are the ones a real solution will need.

# Example:
# ./bin/warmup.sh

set -euo pipefail

test_runner_jar=/opt/test-runner/target/scala-3.4.2/TestRunner-assembly-0.1.0-SNAPSHOT.jar
cds_dir=/opt/test-runner/cds

workdir=/tmp/warmup
workdir_target="${workdir}/target"

rm -rf "${workdir}"
mkdir -p "${workdir}/src/main/scala" "${workdir}/src/test/scala" "${workdir_target}" "${cds_dir}"

cat > "${workdir}/src/main/scala/Warmup.scala" <<'SCALA'
object Warmup {
  def leapYear(year: Int): Boolean = {
    def divisibleBy(i: Int) = year % i == 0
    divisibleBy(4) && (divisibleBy(400) || !divisibleBy(100))
  }
}
SCALA

cat > "${workdir}/src/test/scala/WarmupTest.scala" <<'SCALA'
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class WarmupTest extends AnyFunSuite with Matchers {

  test("a passing test") {
    Warmup.leapYear(2015) should be (false)
  }

  test("a failing test, so that the reporting path is loaded too") {
    Warmup.leapYear(1900) should be (true)
  }
}
SCALA

echo "warmup: dumping ${cds_dir}/scalac.jsa"
scalac -J-XX:ArchiveClassesAtExit="${cds_dir}/scalac.jsa" \
    -classpath "${test_runner_jar}" -d "${workdir_target}" \
    "${workdir}"/src/main/scala/* "${workdir}"/src/test/scala/*

echo "warmup: dumping ${cds_dir}/runner.jsa"
java -XX:ArchiveClassesAtExit="${cds_dir}/runner.jsa" \
    -classpath "${test_runner_jar}" TestRun "${workdir_target}" "${workdir}/test-results.json"

# An archive the JVM cannot use is ignored rather than reported: the run simply
# goes back to being slow, which is not the sort of thing anyone notices in a
# build log. `-Xshare:on` turns that silence into a refusal to start, so a
# classpath that has drifted out of step with bin/run.sh fails the build here
# instead of quietly costing every run a second. bin/run.sh itself leaves the
# default `-Xshare:auto` in place, where a bad archive only costs speed.
echo "warmup: verifying the archives are usable"
scalac -J-Xshare:on -J-XX:SharedArchiveFile="${cds_dir}/scalac.jsa" \
    -classpath "${test_runner_jar}" -d "${workdir_target}" \
    "${workdir}"/src/main/scala/* "${workdir}"/src/test/scala/*
java -Xshare:on -XX:SharedArchiveFile="${cds_dir}/runner.jsa" \
    -classpath "${test_runner_jar}" TestRun "${workdir_target}" "${workdir}/test-results.json"
java -Xshare:on -XX:SharedArchiveFile="${cds_dir}/runner.jsa" \
    -jar "${test_runner_jar}" /dev/null "${workdir}/test-results.json" "${workdir}/results.json"

rm -rf "${workdir}"

echo "warmup: done"
