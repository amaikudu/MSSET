#!/bin/bash
set -e

cd "$(dirname "$0")"

if [ ! -d build ]; then
    echo "The build folder does not exist yet. Running ./build.sh first."
    ./build.sh
fi

echo "Running the headless smoke test..."
java -Djava.awt.headless=true -cp build SmokeTest
