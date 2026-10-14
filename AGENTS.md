# Barbering --- AI Agent Instructions

## Mission

You are the coding agent for Barbering, a full-stack barber discovery,
booking, and AI-assisted hairstyle platform. Implement requested
outcomes end-to-end, not merely isolated UI/classes.

You have bounded autonomy: - Create/modify files necessary to complete
the requested feature. - Make directly required supporting
improvements. - Do not expand into unrelated product features. - Do not
silently make major architectural decisions without approval.

## Product Vision

Public users can discover barbers, locations, services, ratings, and
availability without an account.

Authenticated customers can: - use email/password or optional Google
OAuth - book, cancel, and reschedule appointments - view history - save
favourites - use AI features

Barbers can: - manage profiles/photos - manage services, prices, and
service duration - set working hours/breaks - manage availability -
accept/reject appointments - view calendar and earnings - manage
customers

Booking: availability → slots → service/slot → booking request →
accept/reject → confirmation → reminder → appointment →
completed/cancelled/no-show.

Payments are future scope.

AI: - hairstyle/photo try-on - text style recommender using hair/face
characteristics, preferences, maintenance needs, and barber/service
data - AI features require authentication - AI UX should be simple and
approachable

## Current Repository Reality

The repository contains session-based Spring Security authentication,
PostgreSQL/Flyway persistence, public barber discovery, barber profiles and
services, working hours and recurring breaks, availability, appointment
requests and lifecycle actions, reviews, favourites, local barber-photo
storage, and customer/barber dashboards. AI routes have typed contracts but
return an unavailable response until a provider is configured.

Google OAuth, password recovery, notifications/reminders, a connected AI
provider, and payment flows are not implemented. PostgreSQL is the production
database, but verify whether it is available in the current environment before
claiming PostgreSQL runtime behavior.

Read `.ai/CODEBASE_MAP.md` before making assumptions; actual source code is the
final authority.

## Context Loading

Always read this file first.

Load only relevant context: - Product/current repository:
`.ai/PROJECT.md`, `.ai/CODEBASE_MAP.md` - Architecture/workflow:
`.ai/ARCHITECTURE.md`, `.ai/WORKFLOW.md` - Frontend:
`.ai/frontend/AGENTS.md` - Backend: `.ai/backend/AGENTS.md` - Auth:
`.ai/auth/AGENTS.md` - Database: `.ai/database/AGENTS.md` - Cross-app
API: `.ai/api/AGENTS.md`

Frontend-only work should not load backend/database/auth documentation
unless the task crosses those boundaries. Do the same in reverse.
Cross-system tasks should load only affected domains.

## Source-of-Truth Priority

1.  Actual source code
2.  Tests and executable configuration
3.  Current `.ai/` documentation
4.  README
5.  General assumptions

Never invent functionality from planned documentation.

## End-to-End Rule

Interpret requests such as "implement login" as complete behavior.
Investigate the necessary UI, API, backend, persistence, password
handling, sessions, authorization, protected routes, errors, tests, and
documentation. Implement only what the selected architecture requires.

"Add OAuth" or "integrate auth with the dashboard/database" should
trigger investigation of every affected boundary.

## Scope

Allowed supporting work: - required
components/classes/services/repositories/DTOs - validation and error
handling - relevant tests - API contract updates - documentation
updates - small directly-blocking fixes

Do not silently: - redesign unrelated UI - replace frameworks - switch
databases - add unrelated OAuth providers - add payments before
requested - add deployment infrastructure during unrelated work -
rewrite large unrelated areas

## Decision Gates

Ask before major unresolved decisions such as: - PostgreSQL vs MongoDB -
auth/session architecture - OAuth provider choices - payment provider -
major cloud/deployment architecture - breaking API contracts

If an ADR already decides it, follow it without asking the user to choose again. Record approved major decisions
in `.ai/decisions/`.

## Git Safety

Do not create, amend, squash, rebase, or push Git commits unless the user explicitly asks. Do not modify Git history or discard user work. Never run destructive commands such as `git reset --hard`, `git clean -fd`, `git checkout -- .`, or `git restore .`. Read-only inspection such as `git status` and `git diff` is allowed.

## Definition of Done

A feature is complete only when, as applicable: 1. Requested behavior
works end-to-end. 2. Frontend/backend contracts agree. 3. Validation and
important error states exist. 4. Authentication/authorization/security
are handled. 5. Existing functionality is not unnecessarily broken. 6.
Relevant tests are added/updated. 7. Relevant checks are actually
executed. 8. Affected documentation is updated.

Never claim a command passed unless executed.

## Workflow

1.  Understand the desired outcome.
2.  Inspect existing implementation and callers.
3.  Load only relevant context.
4.  Plan substantial changes.
5.  Implement the smallest coherent set of changes.
6.  Test/build/lint as appropriate.
7.  Review for security, regressions, and scope.
8.  Update affected documentation.

## Loop / Hallucination Protection

Do not endlessly retry.

If the same failure persists after two meaningful attempts: - stop -
report failure/evidence - list attempts - state likely root cause -
state missing information/decision

Do not change unrelated code just to make a test pass. Do not invent
APIs, classes, fields, configuration, credentials, or external-service
behavior.

If ambiguity materially changes architecture/data design, ask the user.

## Security

Never store plaintext passwords, log credentials/tokens, commit secrets,
expose secrets to the browser, or return sensitive auth data
unnecessarily.

Backend authorization must protect sensitive operations; frontend
visibility alone is not security.

## Product Boundaries

Discovery is public. Authentication is required for booking, appointment
management, favourites, dashboards, and AI tools. Do not make the whole
website auth-gated unless requirements change.

## Documentation Maintenance

Update only affected docs: - architecture → `.ai/ARCHITECTURE.md` - API
→ `.ai/API.md` - database → `.ai/DATABASE.md` - roadmap →
`.ai/TASKS.md` - decisions → `.ai/decisions/` - repository structure →
`.ai/CODEBASE_MAP.md` - domain conventions → relevant scoped
instructions

## Final Report

After a task, report: \### Changed files and purpose \### Verified
commands actually executed and results \### Notes decisions,
limitations, and anything not verified.
