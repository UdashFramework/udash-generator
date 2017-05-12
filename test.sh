#!/usr/bin/env bash

echo "Assembling Udash generator..."
sbt assembly
cp cmd/target/scala-2.12/udash-generator.jar dist/udash-generator.jar

cd dist

ERRORS=0
for f in ../test/*.cnf; do
  echo "Starting test $f..."
  ./run.sh < $f > /dev/null
  cd test-app
  echo "Compiling $f..."
  sbt compile > ../$f.log
  if [ $? -eq 0 ]; then
    echo -e "Test $f \e[32msucceed\e[39m!"
  else
    echo -e "Test $f \e[31mfailed\e[39m!"
    ((ERRORS++))
  fi
  cd ..
done

cd ..
exit ${ERRORS}