#!/usr/bin/env bash

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
  sed  -i '' '/pending/d' $f
done

echo running tests
cd $solution_path
sbt test

