#!/usr/bin/env sh

slug="$1"
solution_path="$2"

echo "slug:          $slug"
echo "solution path: $solution_path"

if [ -z "$slug" ] || [ -z "$solution_path" ]; then
    echo "slug and solution path must be present"
    exit 1
fi

echo remove pending annotations
for f in $solution_path/src/test/scala/*.scala
do
  sed -i -e '/pending/d' $f
  sed -i -e 's/import org.scalatest.{Matchers, FunSuite}/import org.scalatest.{Matchers, FunSuite, CancelAfterFailure}/' $f
  sed -i -e 's/FunSuite with Matchers/FunSuite with CancelAfterFailure with Matchers/' $f
done

echo running tests
cd $solution_path


host=$(hostname)
echo 127.0.0.1 $host > /etc/hosts

sbt -Dsbt.ivy.home=/opt/scala-test-runner/.ivy2 -Divy.home=/opt/scala-test-runner/.ivy2 -Dsbt.global.base=/opt/scala-test-runner/.sbt/1.0 -Dsbt.boot.directory=/opt/scala-test-runner/.sbt/boot test

# Uncomment below if wanting to grab dependencies for building new release of test runner
#cp -r /opt/scala-test-runner/.ivy2 .
#cp -r /opt/scala-test-runner/.sbt .
