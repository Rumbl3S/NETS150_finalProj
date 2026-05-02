#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
OUT="${ROOT}/out"
mkdir -p "$OUT"
JAVAC="${JAVA_HOME:-}/bin/javac"
[[ -x "$JAVAC" ]] || JAVAC="javac"
JAVA="${JAVA_HOME:-}/bin/java"
[[ -x "$JAVA" ]] || JAVA="java"
find "${ROOT}/src/main/java" -name '*.java' -print0 | xargs -0 "$JAVAC" --release 17 -d "$OUT"
exec "$JAVA" -cp "$OUT" com.nets150.recommender.Main "$@"
