#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION=8.8
DIST_ROOT=".gradle/wrapper/dists"
GRADLE_HOME="${DIST_ROOT}/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_HOME}/bin/gradle"
GRADLE_ZIP="${DIST_ROOT}/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_USER_HOME="${PWD}/.gradle"
export GRADLE_USER_HOME

if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: curl is required to download Gradle ${GRADLE_VERSION}." >&2
    exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "ERROR: python3 is required to unpack Gradle ${GRADLE_VERSION}." >&2
    exit 1
fi

if [ ! -x "${GRADLE_BIN}" ]; then
    mkdir -p "${DIST_ROOT}"
    if [ ! -f "${GRADLE_ZIP}" ]; then
        echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
        curl -fL "${GRADLE_URL}" -o "${GRADLE_ZIP}"
    fi
    python3 -m zipfile -e "${GRADLE_ZIP}" "${DIST_ROOT}"
    chmod +x "${GRADLE_BIN}"
fi

exec "${GRADLE_BIN}" "$@"
