# Prompt — Tooling Readiness Review

**Use:** Paste into Claude (or save as a `.claude/commands/` slash command). Input a description of the client's current AI tooling/identity/storage setup; get back pre-flight-gate status.

**Arguments:** `$ARGUMENTS` = a description of the client environment (AI tools in use, identity/SSO, storage, Git host, current logging).

---

You are assisting a clean-room AI development engagement. Given the client environment below, assess readiness against the **pre-flight gate** and report status. Render no legal advice; flag anything requiring counsel.

CLIENT ENVIRONMENT:
$ARGUMENTS

Assess each item as ✅ Ready / 🟡 Partial / ❌ Missing, with a one-line reason and the next action + owner:

1. Commercial AI plan active; **not** enrolled in any model-training program
2. DPA (and BAA if regulated data) executed; retention set with counsel
3. SSO enforced; SCIM provisioning; personal logins blocked
4. Workspaces map to roles (and Rooms, if two-team) with isolation
5. Enterprise desktop/app deployed to the fleet
6. Agentic CLI installed, stable channel, authed to the org (not personal)
7. Guardrail file (CLAUDE/AGENTS/GEMINI.md) committed and verified to load
8. Compliance/audit logging on; export to an immutable store; live test entry verified
9. Agent session logs reach the archive; trace IDs link logs ↔ commits
10. Branch protection enforces reviewer ≠ author; similarity CI required
11. Copilot (or other incumbent AI) policy enforced for covered repos
12. Engineers trained & certified; policy acknowledged

Then output:
- **Overall verdict:** GO / NOT YET
- **Top blockers** (the ❌/🟡 items that gate covered work), each with owner + next action
- **Counsel-flag items** (anything legal must confirm)

Be specific and conservative; if the environment description is silent on an item, mark it 🟡 and say what to confirm.
