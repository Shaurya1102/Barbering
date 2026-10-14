# Barbering — Agent Workflow

## Context

1. Read the root `AGENTS.md` first.
2. Load only documents relevant to the task.
3. Use source code and executable configuration as the primary truth.
4. Preserve the existing `.ai/CODEBASE_MAP.md`; update it only when repository structure materially changes.

## Implementation

1. Understand the requested outcome and acceptance conditions.
2. Inspect affected source files and callers before editing.
3. Load the scoped `.ai` guidance for each affected boundary.
4. For cross-boundary work, verify the frontend/API/backend/database/auth contracts together.
5. Implement the smallest coherent end-to-end change.
6. Make directly blocking fixes when necessary; avoid unrelated cleanup.
7. Add or update relevant tests.
8. Run relevant build, test, lint, and type checks.
9. Review the final diff for regressions, security issues, and scope creep.
10. Update only documentation affected by the change.

## Autonomous Work

For a requested feature, finish the necessary supporting work across affected layers rather than stopping after one layer. Do not ask about a decision already accepted in an ADR.

If an unresolved decision materially changes architecture, data design, security, or an external provider, stop and ask rather than guessing.

## Failure / Loop Protection

If the same failure persists after two meaningful attempts, stop. Report the error, evidence, attempts made, likely root cause, and missing information. Do not modify unrelated code to force a pass.

Never claim a command, test, build, or integration passed unless it was actually executed.

## Git Safety

Do not create, amend, squash, rebase, or push commits unless the user explicitly asks.

Do not modify Git history or discard user work. Do not run destructive commands such as:

- `git reset --hard`
- `git clean -fd`
- `git checkout -- .`
- `git restore .`

Read-only Git inspection such as `git status` and `git diff` is allowed.
