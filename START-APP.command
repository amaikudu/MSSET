#!/bin/bash
# ============================================================
#  Double-click this file in Finder to start the application.
#
#  macOS runs .command files when you double-click them, unlike .sh files
#  (which just open in a text editor). The Terminal window stays open while
#  the application runs; closing it stops the program.
#
#  You can also run it from Terminal:  ./START-APP.command
# ============================================================

cd "$(dirname "$0")" || exit 1

echo "=============================================="
echo "  Market Surveillance Engine"
echo "=============================================="
echo

if ! command -v java > /dev/null 2>&1; then
    echo "ERROR: Java was not found on this Mac."
    echo "Install a JDK (Java 8 or newer), then try again."
    echo
    read -r -p "Press Enter to close..."
    exit 1
fi

if [ ! -d build ] || [ ! -f build/Main.class ]; then
    echo "First run - compiling the project..."
    javac -encoding UTF-8 -d build $(find src tools -name '*.java') || {
        echo
        echo "ERROR: the project did not compile."
        read -r -p "Press Enter to close..."
        exit 1
    }
    echo "Compiled successfully."
    echo
fi

echo "Starting the application window..."
echo "(keep this Terminal window open while you use the app)"
echo

java -cp build Main

echo
echo "The application has closed."
read -r -p "Press Enter to close this window..."
