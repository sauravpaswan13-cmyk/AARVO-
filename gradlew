#!/bin/sh

# Gradle startup script for AARVO Android builds.
set -e

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=9.7.1
GRADLE_USER_HOME=${GRADLE_USER_HOME:-$HOME/.gradle}
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
DIST_ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ -x "$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle" ]; then
  exec "$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle" "$@"
fi

mkdir -p "$DIST_DIR"
if command -v curl >/dev/null 2>&1; then
  curl -fsSL "$DIST_URL" -o "$DIST_ZIP"
elif command -v wget >/dev/null 2>&1; then
  wget -q "$DIST_URL" -O "$DIST_ZIP"
else
  echo "Unable to download Gradle: curl or wget is required." >&2
  exit 1
fi

unzip -q -o "$DIST_ZIP" -d "$DIST_DIR"
exec "$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle" "$@"
