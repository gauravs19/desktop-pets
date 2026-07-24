#!/usr/bin/env bash
# One-shot setup for macOS and Linux: install prerequisites if missing, build, and run.
#
#   ./setup.sh            # install what's missing, build, launch
#   ./setup.sh --no-run   # install and build only
#
# Prerequisites are JDK 21+ and Maven. JavaFX itself needs no installation: it resolves as ordinary
# Maven dependencies for the current platform and is shaded into the output jar.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
run_after=1
[[ "${1:-}" == "--no-run" ]] && run_after=0

have() { command -v "$1" >/dev/null 2>&1; }

java_ok() {
  have java || return 1
  # 'openjdk version "21.0.10"' -> 21
  local major
  major="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
  [[ -n "$major" && "$major" -ge 21 ]]
}

install_pkgs() {
  local what="$*"
  echo "Installing: $what"
  if have brew; then
    # shellcheck disable=SC2086
    brew install $what
  elif have apt-get; then
    sudo apt-get update
    # shellcheck disable=SC2086
    sudo apt-get install -y $what
  elif have dnf; then
    # shellcheck disable=SC2086
    sudo dnf install -y $what
  elif have pacman; then
    # shellcheck disable=SC2086
    sudo pacman -S --noconfirm $what
  else
    echo "No supported package manager found (brew, apt-get, dnf, pacman)." >&2
    echo "Install JDK 21+ and Maven manually, then re-run this script." >&2
    exit 1
  fi
}

# --- Java ---------------------------------------------------------------------------------------
if java_ok; then
  echo "JDK 21+ already present."
else
  if have brew; then
    install_pkgs openjdk@21
    echo "Note: Homebrew's openjdk@21 is keg-only. If 'java' still is not found, add it to PATH:"
    echo '  export PATH="$(brew --prefix openjdk@21)/bin:$PATH"'
  elif have apt-get; then
    install_pkgs openjdk-21-jdk
  elif have dnf; then
    install_pkgs java-21-openjdk-devel
  elif have pacman; then
    install_pkgs jdk21-openjdk
  else
    install_pkgs "a JDK 21"
  fi
fi

# --- Maven --------------------------------------------------------------------------------------
if have mvn; then
  echo "Maven already present."
else
  install_pkgs maven
fi

if ! have mvn || ! java_ok; then
  echo
  echo "Prerequisites installed, but this shell cannot see them yet."
  echo "Open a new terminal and run ./setup.sh again."
  exit 0
fi

# --- Build --------------------------------------------------------------------------------------
echo "Building..."
mvn -q -f "$root/pom.xml" clean package
echo "Built $root/target/desktop-pets.jar"

if [[ "$run_after" == 1 ]]; then
  "$root/run.sh"
fi
