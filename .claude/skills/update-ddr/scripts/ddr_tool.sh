#!/usr/bin/env bash
# Helper for the update-ddr skill. Read-only: locates DDRs; the skill does the editing.
# Resolves the decision log from .clean-room/config (DDR_LOG=...), default docs/DDR.md.
#
# Usage:
#   ddr_tool.sh ids-in <file>       # DDR-<id>s referenced by that file's fences, in file order
#   ddr_tool.sh show <id> [log]     # print the entry block for one DDR id
#   ddr_tool.sh list [log]          # list all DDR ids + titles (with line numbers)
set -euo pipefail

CFG=".clean-room/config"
DDRLOG="docs/DDR.md"
[ -f "$CFG" ] && . "$CFG"
DDRLOG="${DDR_LOG:-$DDRLOG}"

cmd="${1:-}"; shift || true
case "$cmd" in
  ids-in)
    f="${1:?usage: ddr_tool.sh ids-in <file>}"
    [ -f "$f" ] || { echo "no such file: $f" >&2; exit 1; }
    grep -oE 'DDR:[[:space:]]*DDR-[0-9A-Za-z_-]+' "$f" 2>/dev/null \
      | grep -oE 'DDR-[0-9A-Za-z_-]+' | awk '!seen[$0]++'
    ;;
  show)
    id="${1:?usage: ddr_tool.sh show <id> [log]}"; log="${2:-$DDRLOG}"
    [ -f "$log" ] || { echo "no such log: $log" >&2; exit 1; }
    out="$(awk -v id="## $id" '$0 ~ "^"id{f=1} f&&/^## /&&$0!~"^"id{exit} f{print}' "$log")"
    [ -n "$out" ] || { echo "$id not found in $log" >&2; exit 1; }
    printf '%s\n' "$out"
    ;;
  list)
    log="${1:-$DDRLOG}"
    [ -f "$log" ] || { echo "no such log: $log" >&2; exit 1; }
    grep -nE '^##[[:space:]]+DDR-' "$log" || echo "(no DDR entries in $log)"
    ;;
  *)
    echo "usage: ddr_tool.sh {ids-in <file> | show <id> [log] | list [log]}" >&2; exit 2 ;;
esac
