#!/usr/bin/env bash
# Deterministic provenance gate. Reads .clean-room/config if present (DDR_LOG=...).
# FAILS (exit 1) if any code file contains:
#   - a DDR: TBD placeholder, or
#   - a HUMAN-AUTHORED fence/header with NO DDR: field at all, or
#   - a DDR: <id> that doesn't resolve to an entry (with a real Rationale) in the DDR log.
# PASSES `DDR: none` (explicit opt-out) and any `DDR: <id>` that resolves.
#
# Usage: bash ci/check_provenance.sh [dir] [ddr-log]
set -euo pipefail

# config: .clean-room/config can set DDR_LOG, CORPUS_DIR, TOKEN_THRESHOLD, etc.
CFG=".clean-room/config"
[ -f "$CFG" ] && . "$CFG"

DIR="${1:-src}"
DDRLOG="${2:-${DDR_LOG:-docs/DDR.md}}"
INC=(--include='*.kt' --include='*.kts' --include='*.java' --include='*.py' --include='*.ts'
     --include='*.js' --include='*.go' --include='*.cs' --include='*.rb')
fail=0

while IFS= read -r line; do echo "DDR-TBD    $line"; fail=1; done \
  < <(grep -rInE 'DDR:[[:space:]]*TBD' "$DIR" "${INC[@]}" 2>/dev/null || true)

while IFS= read -r line; do echo "NO-DDR     $line"; fail=1; done \
  < <(grep -rInE 'PROVENANCE(-BEGIN)?:[[:space:]]*HUMAN-AUTHORED' "$DIR" "${INC[@]}" 2>/dev/null \
      | grep -vE 'DDR:[[:space:]]*[^[:space:]]' || true)

for id in $(grep -rhoE 'DDR:[[:space:]]*DDR-[0-9A-Za-z_-]+' "$DIR" "${INC[@]}" 2>/dev/null \
             | grep -oE 'DDR-[0-9A-Za-z_-]+' | sort -u); do
  if ! grep -qE "^##[[:space:]]+$id( |—|-|$)" "$DDRLOG" 2>/dev/null; then
    echo "DDR-MISSING  $id not found in $DDRLOG"; fail=1; continue
  fi
  rat=$(awk -v id="## $id" '$0 ~ "^"id { f=1 } f && /^## / && $0 !~ "^"id { exit } f && /^Rationale:/ { sub(/^Rationale:[[:space:]]*/,""); print; exit }' "$DDRLOG")
  case "$rat" in ""|"<"*) echo "DDR-NO-RATIONALE  $id has no real Rationale in $DDRLOG"; fail=1 ;; esac
  # rationale-origin: the entry must attest human conception
  org=$(awk -v id="## $id" '$0 ~ "^"id { f=1 } f && /^## / && $0 !~ "^"id { exit } f && tolower($0) ~ /^conceived-by:/ { print tolower($0); exit }' "$DDRLOG")
  case "$org" in *human*) : ;; *) echo "DDR-NO-HUMAN-ORIGIN  $id lacks 'Conceived-by: human' in $DDRLOG"; fail=1 ;; esac
done

# 4) cut-line: a whole-file BASELINE file edited since the cut-line must be CONVERTED to fences,
#    so new work can't hide under the top-of-file baseline header.
CUT="$(awk -F':' '/cut_line_commit/{gsub(/[[:space:]]/,"",$2); print $2}' .clean-room/baseline 2>/dev/null || true)"
if [ -n "${CUT:-}" ] && git rev-parse --verify "$CUT" >/dev/null 2>&1; then
  while IFS= read -r f; do
    [ -f "$f" ] || continue
    grep -qE 'PROVENANCE:[[:space:]]*BASELINE' "$f" || continue     # has a whole-file BASELINE header
    grep -q 'PROVENANCE-BEGIN' "$f" && continue                     # already converted to fences → ok
    if ! git diff --quiet "$CUT" -- "$f" 2>/dev/null; then          # changed since the cut-line
      echo "BASELINE-EDITED  $f — edited since cut-line but still whole-file BASELINE; convert to fences (fence the pre-existing code BASELINE, tag the new code)."; fail=1
    fi
  done < <(grep -rlE 'PROVENANCE:[[:space:]]*BASELINE' "$DIR" "${INC[@]}" 2>/dev/null || true)
fi

if [ "$fail" = 0 ]; then
  echo "PASS: DDRs resolved; no TBD/missing; HUMAN-AUTHORED blocks reference a real DDR or DDR: none; edited baseline files converted."
  exit 0
fi
echo "FIX REQUIRED: resolve the items above (add DDR ids/entries, or use DDR: none)."
exit 1
