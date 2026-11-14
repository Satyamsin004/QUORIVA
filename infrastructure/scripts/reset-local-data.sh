#!/usr/bin/env bash
# CS Scholar AI — Local Development Data Purge & Clean-State Reset (Bash)
set -euo pipefail

echo "======================================================================"
echo "CS Scholar AI — Local Development Data Reset"
echo "======================================================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_BIN="${SCRIPT_DIR}/../../ai-service/.venv/bin/python"

if [ ! -f "$PYTHON_BIN" ]; then
    PYTHON_BIN="python3"
fi

"$PYTHON_BIN" "${SCRIPT_DIR}/reset_local_data.py" "$@"

