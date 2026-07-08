---
name: trace-audit
description: Resolve a Trace id (or a PR commit) to its full clean-room audit chain — the fenced code region(s) carrying that trace, the linked Design Decision Record(s), the commit(s), and the captured AI session transcript(s) in the immutable sink, hash-verified against their manifests. Read-only; assembles evidence from provenance fences, docs/DDR.md, git history, .clean-room/manifests/pr-*.json, and the capture sink. Use to answer "show me the AI interactions behind this code" for review, audit, or a patentability gate.
---

# trace-audit — resolve code provenance to the AI interactions behind it

## What it does
Given a `Trace:` id stamped in a provenance fence, it walks the whole chain and prints it:
1. **Code** — every fenced region carrying that trace (file:line + the fence header).
2. **DDR** — the decision record(s) linked (via the fence's `DDR:` field and any DDR whose own `Trace:` matches), including the human `Rationale` and `Conceived-by:` line.
3. **Commit(s)** — commits referencing the trace (message grep + `-S` string search).
4. **AI session(s)** — from the per-PR manifests (`.clean-room/manifests/pr-*.json`) that name this
   trace, the bound session transcripts, located in the capture **sink** and **hash-verified** against
   their shipped manifests (`[OK]` / `[UNVERIFIED]`).

## When to run
During PR review, an audit, or a patentability gate — whenever someone asks "what AI interactions
produced this code, and can we prove it?" It is read-only evidence assembly; it never edits anything.

## Usage
```
bash .claude/skills/trace-audit/scripts/trace_audit.sh <TRACE_ID>          # full chain for a trace
bash .claude/skills/trace-audit/scripts/trace_audit.sh <TRACE_ID> --show   # also print the transcripts
bash .claude/skills/trace-audit/scripts/trace_audit.sh --pr <commit>       # everything bound to one PR
```

## What it reads (all things you already produce)
- Provenance fences in source (the `Trace:`/`DDR:` fields).
- `docs/DDR.md` (or `DDR_LOG` from `.clean-room/config`).
- Git history.
- `.clean-room/manifests/pr-*.json` from `pr_manifest.sh` (binds traces ↔ sessions ↔ commit).
- The capture sink, configured in `~/.clean-room/logging.conf` (`SINK_DIR`, or `SINK_LS_CMD` /
  `SINK_GET_CMD` for a remote/object-store sink). If no sink is configured, it falls back to the live
  `~/.claude/projects/*.jsonl` and clearly labels it **not immutable-verified**.

## How the mapping works (and its granularity)
The `Trace:` id is the human-readable join key: it is stamped in the fence, the DDR, and the commit.
`pr_manifest.sh` records, per push, the traces present in the changed code alongside the AI sessions
active in that window (session id + sha256). So a trace resolves to sessions **at PR-window
granularity** — "this PR's code ↔ these hash-verified sessions." For exact per-interaction resolution,
include the work-item/trace id in prompts (or run one session per work item); then the id appears in
the transcript and `--show` output can be grepped directly.

## Reading the output
- `[OK]` next to a sink chunk = the transcript in the sink still matches the sha256 recorded when it
  was shipped (tamper-evident).
- `[UNVERIFIED]` = the chunk is present but its hash doesn't match its manifest (or the manifest is
  missing) — investigate before relying on it.
- "no PR manifest references this trace yet" = run `pr_manifest.sh` at push time so the binding exists.

## Do NOT
- Treat live `~/.claude/projects` transcripts as audit-grade — only sink chunks are immutable/verified.
- Edit code, DDRs, or manifests — this skill only reads and reports.

*Evidence assembler for the clean-room audit trail. Not legal advice. Pairs with the capture agent
(`logging_team`) and `pr_manifest.sh`, which produce the sink and the PR bindings it reads.*
