# Prompt — Separation-of-Duties Enforcement Config

**Use:** Paste into Claude (or save as a slash command). Generates the **enforcement requirements spec** that implements the separation-of-duties rule in the client's Git host. (This is the requirement surfaced by Item 1 and owned/produced under Item 2.)

**Arguments:** `$ARGUMENTS` = Git host (GitHub / GitLab / Bitbucket / Azure DevOps), repo model (single-team or two-team Room A/B), team size, and current branch-protection settings if known.

---

You are producing the separation-of-duties enforcement spec for a clean-room AI development engagement. The core rule: **the author of a module must never be the reviewer who signs off its transformation or compliance check.** Translate that into concrete, host-specific configuration. Render no legal advice.

CONTEXT:
$ARGUMENTS

Produce:

1. **Branch protection requirements**
   - Require pull requests to protected branches; block direct pushes.
   - Require ≥1 approving review **from someone other than the author** (enable "dismiss stale approvals"; disable author self-approval).
   - Require **status checks to pass** before merge: token + AST similarity screening (Item 3) and any build/test gates.
   - Require linear history / no force-push to protected branches (as appropriate).
2. **Code ownership routing** — CODEOWNERS (or host equivalent) so transformation reviews route to qualified reviewers who are not the author; name the Compliance Reviewer where its sign-off is required.
3. **Two-team additions** (if applicable) — repository isolation between Room A and Room B (no shared history); access restricted by SSO group; cross-repo access alerts to the Compliance Officer.
4. **Mapping to gates** — show which protection enforces which process gate (code-complete, similarity, transformation).
5. **Verification** — how to test that an author genuinely cannot approve their own PR.
6. **Host-specific notes** — exact setting names/paths for the named Git host, and any limitations (e.g., features not available on the client's tier).

Output as a configuration checklist an admin can apply, plus a short "how we verify" section. Flag anything the host can't enforce natively so it can be covered procedurally.
