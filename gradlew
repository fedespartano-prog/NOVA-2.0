
#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

GRADLE_VERSION="8.9"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIR="$GRADLE_USER_HOME/manual-gradle"
GRADLE_HOME="$GRADLE_DIR/gradle-$GRADLE_VERSION"

if ! command -v curl >/dev/null 2>&1; then
    echo "ERRORE: curl non disponibile."
    exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
    echo "ERRORE: unzip non disponibile."
    exit 1
fi

mkdir -p "$GRADLE_DIR"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    TMP_DIR=$(mktemp -d "$GRADLE_DIR/gradle-download.XXXXXX")
    trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

    curl -fL --retry 3 \
        "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
        -o "$TMP_DIR/gradle.zip"

    unzip -q "$TMP_DIR/gradle.zip" -d "$TMP_DIR"

    rm -rf "$GRADLE_HOME"
    mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$GRADLE_HOME"

    rm -rf "$TMP_DIR"
    trap - EXIT HUP INT TERM
fi

exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
