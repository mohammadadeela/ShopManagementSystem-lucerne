#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "WARNING: database/lucerne_demo_final.sql DROPS and recreates lucerne_demo."
read -r -p "Type IMPORT-DEMO to continue: " answer
[[ "$answer" == "IMPORT-DEMO" ]] || { echo "Cancelled."; exit 1; }
mysql -u "${MYSQL_USER:-root}" -p < database/lucerne_demo_final.sql
