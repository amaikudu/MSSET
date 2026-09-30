#!/bin/bash
set -e

cd "$(dirname "$0")"

echo "Cleaning the build folder..."
rm -rf build
mkdir -p build

echo "Compiling src/ and tools/ ..."
javac -encoding UTF-8 -d build $(find src tools -name '*.java')

echo
echo "Running the pre-submission check..."
java -cp build PreflightCheck src

echo
echo "Build finished successfully. Classes are in build/"
echo
echo "Next steps:"
echo "  ./run-demo.sh          start the demo window"
echo "  ./run-smoke-test.sh    check that every panel runs without errors"
echo "  ./verify-all.sh        build + smoke test + pre-submission check"
