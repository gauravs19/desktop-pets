#!/usr/bin/env bash
# Build (if needed) and launch Desktop Pets on macOS or Linux.
#
#   ./run.sh             # build if the jar is missing, then run
#   ./run.sh --rebuild   # always rebuild first
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jar="$root/target/desktop-pets.jar"

if [[ "${1:-}" == "--rebuild" || ! -f "$jar" ]]; then
  echo "Building..."
  mvn -q -f "$root/pom.xml" clean package
fi

echo "Launching Desktop Pets. Right-click a pet for its menu."
# Detached, with output kept out of the terminal, so closing the shell does not take the pets with it.
nohup java -jar "$jar" >/dev/null 2>&1 &
disown || true
