#!/bin/sh

# Gradle Wrapper Executable Script
# Resolves to system gradle if gradle-wrapper.jar is not present
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)

if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    JAVA_CMD="java"
    if [ -n "$JAVA_HOME" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
    fi
    exec "$JAVA_CMD" -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
elif command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Error: Neither gradle command nor gradle-wrapper.jar was found." >&2
    exit 1
fi
