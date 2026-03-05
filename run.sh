#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR="betoffer-stake.jar"
if [[ ! -f "$JAR" ]]; then
  echo "JAR not found. Run ./compile.sh first."
  exit 1
fi
exec java -jar "$JAR" "$@"
