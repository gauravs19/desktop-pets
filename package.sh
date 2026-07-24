#!/usr/bin/env bash
# Build a self-contained app image with jpackage (macOS and Linux).
#
#   ./package.sh          # -> dist/DesktopPets.app (macOS) or dist/DesktopPets/ (Linux)
#   ./package.sh --zip    # also produce dist/DesktopPets-<os>.zip
#
# The result bundles a trimmed Java runtime, so whoever runs it needs no JDK, no Maven, and no
# JavaFX. jpackage ships with JDK 21, so there is nothing extra to install to produce it.
#
# '--type app-image' is used deliberately: the installer types (dmg, pkg, deb, rpm) each need extra
# tooling on the build machine, and an app image is enough to run.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jar="$root/target/desktop-pets.jar"
stage="$root/target/jpackage-input"
dist="$root/dist"
want_zip=0
[[ "${1:-}" == "--zip" ]] && want_zip=1

command -v jpackage >/dev/null 2>&1 || {
  echo "jpackage not found on PATH. It ships with JDK 21 — check that a JDK (not just a JRE) is installed." >&2
  exit 1
}

if [[ ! -f "$jar" ]]; then
  echo "Jar missing; building first..."
  mvn -q -f "$root/pom.xml" clean package
fi

# Stage only the fat jar: jpackage copies its whole --input directory, and target/ also holds the
# unshaded original-desktop-pets.jar.
rm -rf "$stage" && mkdir -p "$stage"
cp "$jar" "$stage/"

case "$(uname -s)" in
  Darwin) os_tag=macos; produced="$dist/DesktopPets.app" ;;
  Linux)  os_tag=linux; produced="$dist/DesktopPets" ;;
  *)      echo "Unsupported OS: $(uname -s). Use package.ps1 on Windows." >&2; exit 1 ;;
esac

rm -rf "$produced"
mkdir -p "$dist"

echo "Running jpackage (this takes a minute)..."
jpackage \
  --type app-image \
  --name DesktopPets \
  --app-version 0.1.0 \
  --vendor 'Gaurav Sharma' \
  --description 'Chubby puppies that live on your desktop' \
  --input "$stage" \
  --main-jar desktop-pets.jar \
  --main-class dev.gauravs.desktoppets.Launcher \
  --dest "$dist"

echo "Built $produced"

if [[ "$want_zip" == 1 ]]; then
  zip_path="$dist/DesktopPets-$os_tag.zip"
  rm -f "$zip_path"
  (cd "$dist" && zip -qry "$(basename "$zip_path")" "$(basename "$produced")")
  echo "Zipped $zip_path"
fi
