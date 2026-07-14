#!/usr/bin/env sh
set -eu

# Usage:
#   ./build-lib.sh            -> construit le .jar dans target/
#   ./build-lib.sh --install  -> construit + installe dans ~/.m2/repository

INSTALL_LOCAL=false
if [ "${1-}" = "--install" ]; then
  INSTALL_LOCAL=true
fi

if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
else
  echo "[ERREUR] Maven (mvn) n'est pas installé." >&2
  exit 1
fi

ARTIFACT_ID="$($MVN help:evaluate -Dexpression=project.artifactId -q -DforceStdout)"
VERSION="$($MVN help:evaluate -Dexpression=project.version -q -DforceStdout)"
PACKAGING="$($MVN help:evaluate -Dexpression=project.packaging -q -DforceStdout)"

if [ "$PACKAGING" != "jar" ]; then
  echo "[ERREUR] Le packaging du projet est '$PACKAGING' (attendu: jar)." >&2
  exit 1
fi

echo "[INFO] Build du framework..."
$MVN clean package -DskipTests

JAR_PATH="target/${ARTIFACT_ID}-${VERSION}.jar"
if [ ! -f "$JAR_PATH" ]; then
  echo "[ERREUR] Jar introuvable: $JAR_PATH" >&2
  exit 1
fi

echo "[SUCCESS] Jar généré: $JAR_PATH"

if [ "$INSTALL_LOCAL" = "true" ]; then
  echo "[INFO] Installation dans le dépôt Maven local (~/.m2/repository)..."
  $MVN install -DskipTests
  echo "[SUCCESS] Installation locale terminée."
fi
