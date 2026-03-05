#!/usr/bin/env bash
# Injects test/sample-data.txt into the running stake API server.
# Builds SampleInitializer.jar in the project root and runs it.
# Usage: Run from the project root with ./test/run-initializer.sh [baseUrl]
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

INIT_CLASSES="test/classes"
rm -rf "$INIT_CLASSES"
mkdir -p "$INIT_CLASSES"
echo "Compiling SampleInitializer..."
javac -d "$INIT_CLASSES" "$SCRIPT_DIR"/SampleInitializer.java

MANIFEST="test/MANIFEST_TEST.MF"
rm -f "$MANIFEST"
echo "Main-Class: SampleInitializer" > "$MANIFEST"
echo "" >> "$MANIFEST"
jar cfm test/SampleInitializer.jar "$MANIFEST" -C "$INIT_CLASSES" SampleInitializer.class

echo "Running SampleInitializer..."
java -jar test/SampleInitializer.jar "$@"
