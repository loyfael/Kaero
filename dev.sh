#!/usr/bin/env bash
set -euo pipefail

# Compatibility wrapper (old entrypoint).
# Prefer: ./kaero dev

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$ROOT_DIR/kaero" dev "$@"
