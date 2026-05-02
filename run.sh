#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
OUT="${ROOT}/out"
FL="${ROOT}/lib/flatlaf-3.4.1.jar"
mkdir -p "$OUT" "$ROOT/lib"
if [[ ! -f "$FL" ]]; then
  echo "Downloading FlatLaf…"
  curl -sfL -o "$FL" "https://repo1.maven.org/maven2/com/formdev/flatlaf/3.4.1/flatlaf-3.4.1.jar"
fi
JAVAC="${JAVA_HOME:-}/bin/javac"
[[ -x "$JAVAC" ]] || JAVAC="javac"
JAVA="${JAVA_HOME:-}/bin/java"
[[ -x "$JAVA" ]] || JAVA="java"
find "${ROOT}/src/main/java" -name '*.java' -print0 | xargs -0 "$JAVAC" --release 17 -cp "$FL" -d "$OUT"
exec "$JAVA" -cp "${OUT}:${FL}" com.nets150.recommender.Main "$@"
