#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SRC="src/main/java"
DST="classes"
JAR="dist/betoffer-stake.jar"
MANIFEST="dist/MANIFEST.MF"

rm -rf "$DST" dist
mkdir -p "$DST" dist
echo "Main-Class: stake.Main" > "$MANIFEST"
echo "" >> "$MANIFEST"

javac -d "$DST" "$SRC"/stake/*.java
jar cfm "$JAR" "$MANIFEST" -C "$DST" .
echo "Built: $JAR"

cp "$JAR" "$(basename "$JAR")"

