
#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

GRADLE_VERSION=8.7
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIR="$GRADLE_USER_HOME/manual-gradle"
GRADLE_HOME="$GRADLE_DIR/gradle-$GRADLE_VERSION"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    mkdir -p "$GRADLE_DIR"

    TMP_DIR=$(mktemp -d)
    trap 'rm -rf "$TMP_DIR"' EXIT

    curl -fsSL \
      "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
      -o "$TMP_DIR/gradle.zip"

    unzip -q "$TMP_DIR/gradle.zip" -d "$TMP_DIR"

    mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$GRADLE_HOME"
fi

exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
