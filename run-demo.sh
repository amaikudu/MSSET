#!/bin/bash
set -e

cd "$(dirname "$0")"

if [ ! -d build ]; then
    echo "The build folder does not exist yet. Running ./build.sh first."
    ./build.sh
fi

echo "Starting the Market Surveillance Engine..."
java -cp build Main
