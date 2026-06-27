#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 is required." >&2
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Apache Maven 3.9+ is required." >&2
  exit 1
fi
if grep -q '^db.password=CHANGE_ME$' config/database.properties 2>/dev/null; then
  echo "Edit config/database.properties and set your MySQL password first." >&2
  exit 1
fi
mvn clean javafx:run
