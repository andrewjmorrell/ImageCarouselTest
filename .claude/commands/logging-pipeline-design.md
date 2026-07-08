# Prompt — Logging Pipeline Design

**Use:** Paste into Claude (or save as a slash command). Input the client's stack; get a concrete immutable-logging design that satisfies the audit-trail standard.

**Arguments:** `$ARGUMENTS` = client AI tool(s), identity provider, available storage (object store/WORM? SIEM? Git?), Git host, retention/jurisdiction constraints.

---

You are designing the audit-logging pipeline for a clean-room AI development engagement. The goal: capture **every AI interaction (full prompt + response) immutably**, exportable, with retention set to the prosecution/litigation horizon, and traceable to commits and DDRs. Render no legal advice; flag retention/privacy/privilege questions for counsel.

CLIENT STACK:
$ARGUMENTS

Produce a design covering:

1. **Sources to capture** — chat/desktop (e.g., Compliance API export) and agentic CLI session logs; note any coverage gaps (e.g., API/Console traffic the Compliance API may not capture → propose a gateway).
2. **Capture mechanism** per source (native export cadence vs. logging gateway).
3. **Immutable store** — concrete target (object-lock/WORM bucket, or signed-commit repo); integrity verification.
4. **Schema mapping** — confirm records map to the AI Interaction Log schema (trace_id, timestamp, user_id, tool, model_version, prompt, response, output_used, target_module, modifications, provenance_tag).
5. **Trace-ID convention** — how logs link to commits, DDRs, and similarity findings.
6. **Retention & legal hold** — propose a window; flag GDPR-erasure-vs-immutability and privilege tensions for counsel.
7. **Verification step** — a live test: run one session, confirm the prompt/response actually lands in the immutable store.
8. **Risks & gaps** — anything that won't be captured, with mitigations.

Output as an implementable plan with owners (Tooling Owner / MLE) and a checklist. Be explicit where a vendor's coverage is uncertain and must be verified.
