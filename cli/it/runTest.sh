#!/usr/bin/env bash

sbt cli/assembly

cd cli/it

java -jar ../target/scala-2.12/stags-0.2.9-SNAPSHOT ./ -o tags

/usr/bin/vim -Nu test.vim -c "call Test()" TestFile.scala
if [[ "$?" == 1 ]]; then
  echo "nvim test failed"
  exit 1
fi
