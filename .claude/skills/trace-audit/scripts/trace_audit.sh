#!/usr/bin/env bash
# trace-audit — resolve a Trace id (or a PR commit) to its full audit chain:
#   code region(s)  ->  DDR(s)  ->  commit(s)  ->  captured AI session(s) in the sink (hash-verified).
#
# Read-only. Works against artifacts you already produce: provenance fences, docs/DDR.md,
# git history, .clean-room/manifests/pr-*.json, and the capture sink (SINK_DIR / SINK_*_CMD).
#
# Usage:
#   trace_audit.sh <TRACE_ID>          # resolve everything linked to that trace
#   trace_audit.sh --pr <commit>       # resolve a specific PR manifest (pr-<commit>.json)
#   trace_audit.sh <TRACE_ID> --show   # also dump the linked session transcript text
set -uo pipefail

# ---------- config ----------
CFG=".clean-room/config"; [ -f "$CFG" ] && . "$CFG"
DDRLOG="${DDR_LOG:-docs/DDR.md}"
LOGCONF="${LOGGING_CONF:-$HOME/.clean-room/logging.conf}"; [ -f "$LOGCONF" ] && . "$LOGCONF"
: "${CLAUDE_PROJECTS:=$HOME/.claude/projects}"
MANDIR=".clean-room/manifests"
INC=(--include='*.kt' --include='*.kts' --include='*.java' --include='*.py' --include='*.ts'
     --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.go' --include='*.c'
     --include='*.cc' --include='*.cpp' --include='*.cs' --include='*.rs' --include='*.rb'
     --include='*.sh' --include='*.sql')

sha_of(){ command -v sha256sum >/dev/null 2>&1 && sha256sum "$1" | awk '{print $1}' || shasum -a 256 "$1" | awk '{print $1}'; }
hr(){ printf '%s\n' "----------------------------------------------------------------------"; }

# ---------- sink read ----------
sink_chunks(){ # session(no ext) -> chunk keys (relative to sink root)
  local s="$1"
  if [ -n "${SINK_DIR:-}" ]; then
    find "$SINK_DIR/sessions" -type f -path "*/$s/*" 2>/dev/null | sed "s#^$SINK_DIR/##" | sort
  elif [ -n "${SINK_LS_CMD:-}" ]; then SESSION="$s" bash -c "$SINK_LS_CMD"
  fi
}
sink_get(){ # key -> stdout (raw stored bytes)
  local k="$1"
  if [ -n "${SINK_GET_CMD:-}" ]; then KEY="$k" bash -c "$SINK_GET_CMD"
  elif [ -n "${SINK_DIR:-}" ]; then cat "$SINK_DIR/$k" 2>/dev/null
  fi
}
sink_text(){ # key -> decompressed transcript text (gz-aware)
  if [ "${1##*.}" = gz ]; then sink_get "$1" | gunzip -c 2>/dev/null; else sink_get "$1"; fi
}
sink_all_chunks(){ # list every chunk key under sessions/
  if [ -n "${SINK_DIR:-}" ]; then find "$SINK_DIR/sessions" -type f 2>/dev/null | sed "s#^$SINK_DIR/##" | sort
  elif [ -n "${SINK_LS_CMD:-}" ]; then bash -c "$SINK_LS_CMD"
  fi
}
verify_chunk(){ # chunkkey -> "OK"|"UNVERIFIED"
  local key="$1" mkey tmp sha msha
  mkey="manifests/$(printf '%s' "$key" | tr '/' '_').json"
  tmp="$(mktemp)"; sink_get "$key" > "$tmp" 2>/dev/null
  sha="$(sha_of "$tmp")"; rm -f "$tmp"
  msha="$(sink_get "$mkey" 2>/dev/null | grep -oE '"sha256":"[a-f0-9]+"' | head -1 | sed 's/.*:"//; s/"$//')"
  [ -n "$msha" ] && [ "$sha" = "$msha" ] && echo OK || echo UNVERIFIED
}
sink_configured(){ [ -n "${SINK_DIR:-}" ] || [ -n "${SINK_LS_CMD:-}" ]; }

# ---------- report sections ----------
show_sessions(){ # args: session tokens like "<name>.jsonl@<sha>" or "<name>.jsonl"
  local tok s live n=0
  for tok in "$@"; do
    s="${tok%@*}"; s="${s%.jsonl}"; [ -n "$s" ] || continue
    n=$((n+1)); echo "  • session: $s"
    if sink_configured; then
      local got=0
      while IFS= read -r key; do
        [ -n "$key" ] || continue; got=1
        printf '      sink: %s   [%s]\n' "$key" "$(verify_chunk "$key")"
        [ "${SHOW:-0}" = 1 ] && { echo "      ---- transcript chunk ----"; sink_text "$key" | sed 's/^/      /'; echo "      --------------------------"; }
      done < <(sink_chunks "$s")
      [ "$got" = 0 ] && echo "      sink: (no chunks found for this session yet — capture may be behind)"
    else
      live="$CLAUDE_PROJECTS/$s.jsonl"
      [ -f "$live" ] || live="$(find "$CLAUDE_PROJECTS" -name "$s.jsonl" 2>/dev/null | head -1)"
      if [ -n "$live" ] && [ -f "$live" ]; then
        printf '      live:  %s   (sha %s)  — no sink configured; not immutable-verified\n' "$live" "$(sha_of "$live")"
        [ "${SHOW:-0}" = 1 ] && { echo "      ---- transcript (live) ----"; sed 's/^/      /' "$live"; echo "      ---------------------------"; }
      else
        echo "      (transcript not found locally and no sink configured)"
      fi
    fi
  done
  [ "$n" = 0 ] && echo "  (none recorded)"
}

resolve_trace(){
  local T="$1"
  echo "TRACE: $T"; hr
  echo "CODE regions carrying this trace:"
  local hdrs; hdrs="$(grep -rInE "PROVENANCE.*Trace:[[:space:]]*$T([^0-9A-Za-z_-]|\$)" . "${INC[@]}" 2>/dev/null || true)"
  if [ -n "$hdrs" ]; then printf '%s\n' "$hdrs" | sed 's/^/  /'; else echo "  (no fenced region found with this trace)"; fi

  echo; echo "DDR(s) linked:"
  local ddrs; ddrs="$(printf '%s\n' "$hdrs" | grep -oE 'DDR:[[:space:]]*DDR-[0-9A-Za-z_-]+' | grep -oE 'DDR-[0-9A-Za-z_-]+' | sort -u)"
  # also DDR entries whose own Trace matches
  local ddrs2; ddrs2="$(awk -v t="$T" 'BEGIN{RS="\n## "} index($0,"Trace: "t) || index($0,"Trace:  "t){ split($0,a,"\n"); print a[1] }' "$DDRLOG" 2>/dev/null | grep -oE 'DDR-[0-9A-Za-z_-]+' | sort -u)"
  local allddr; allddr="$(printf '%s\n%s\n' "$ddrs" "$ddrs2" | sed '/^$/d' | sort -u)"
  if [ -n "$allddr" ]; then
    while IFS= read -r id; do
      [ -n "$id" ] || continue
      echo "  $id:"
      awk -v id="## $id" '$0 ~ "^"id{f=1} f&&/^## /&&$0!~"^"id{exit} f{print}' "$DDRLOG" 2>/dev/null | sed 's/^/    /' || true
    done <<< "$allddr"
  else echo "  (no DDR linked)"; fi

  echo; echo "COMMIT(s) referencing this trace:"
  local commits; commits="$( { git log --oneline --all --grep="$T" 2>/dev/null; git log --oneline -S"$T" -- . 2>/dev/null; } | sort -u )"
  if [ -n "$commits" ]; then printf '%s\n' "$commits" | sed 's/^/  /'; else echo "  (none — commits may not reference the trace id)"; fi

  if [ "${CAPTURE_MODE:-team}" = enterprise ]; then
    echo; echo "AI SESSION(s) bound by trace-in-transcript (enterprise capture):"
    local any=0 sess
    while IFS= read -r key; do
      [ -n "$key" ] || continue
      sink_text "$key" | grep -q "$T" || continue
      any=1; sess="$(printf '%s' "$key" | sed -E 's#.*sessions/[^/]+/([^/]+)/.*#\1#')"
      printf '  • session: %s   chunk: %s   [%s]\n' "$sess" "$key" "$(verify_chunk "$key")"
      [ "$SHOW" = 1 ] && { echo "      ---- transcript ----"; sink_text "$key" | sed 's/^/      /'; echo "      --------------------"; }
    done < <(sink_all_chunks)
    [ "$any" = 0 ] && echo "  (no captured session mentions this trace — ensure the trace id appears in prompts)"
  else
    echo; echo "AI SESSION(s) bound via PR manifest(s):"
    local found=0
    if [ -d "$MANDIR" ]; then
      for m in "$MANDIR"/pr-*.json; do
        [ -f "$m" ] || continue
        grep -qE "\"$T\"" "$m" || continue
        found=1
        local commit; commit="$(grep -oE '"commit":"[^"]*"' "$m" | sed 's/.*:"//; s/"$//')"
        echo "  from $(basename "$m")  (commit $commit):"
        local sess2; sess2="$(grep -oE '"sessions":\[[^]]*\]' "$m" | sed 's/.*\[//; s/\]//; s/"//g; s/,/ /g')"
        show_sessions $sess2
      done
    fi
    [ "$found" = 0 ] && echo "  (no PR manifest references this trace yet — run pr_manifest.sh at push time)"
  fi
}

resolve_pr(){
  local C="$1" m="$MANDIR/pr-$C.json"
  [ -f "$m" ] || { echo "no PR manifest: $m" >&2; exit 1; }
  echo "PR MANIFEST: $(basename "$m")"; hr
  cat "$m" | sed 's/^/  /'
  echo; echo "TRACES in this PR:"; grep -oE '"traces":\[[^]]*\]' "$m" | sed 's/.*\[//; s/\]//; s/"//g; s/,/\n/g' | sed 's/^/  /'
  echo; echo "AI SESSION(s):"
  local sess; sess="$(grep -oE '"sessions":\[[^]]*\]' "$m" | sed 's/.*\[//; s/\]//; s/"//g; s/,/ /g')"
  show_sessions $sess
}

# ---------- args ----------
SHOW=0; MODE=trace; ARG=""
for a in "$@"; do case "$a" in
  --show) SHOW=1 ;; --pr) MODE=pr ;; *) ARG="$a" ;;
esac; done
[ -n "$ARG" ] || { echo "usage: trace_audit.sh <TRACE_ID> [--show] | --pr <commit>" >&2; exit 2; }
export SHOW
[ "$MODE" = pr ] && resolve_pr "$ARG" || resolve_trace "$ARG"
