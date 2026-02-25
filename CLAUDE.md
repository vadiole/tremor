# tremor – Claude Code Instructions.

## GOAL: Build the best haptic tester app.

## Bootstrap

At the start of every conversation, before doing anything else:

1. Read `project/PROMPT.md` — this contains your operating instructions, role definitions, and workflow rules.
2. Read `project/PROJECT.md` — this contains the project description, goals, constraints, phase plan, and decisions log.
3. Check the phase plan table in `PROJECT.md` to find the current active phase.
4. Read the corresponding phase file (e.g., `project/phase/01-DISCOVERY.md`) and check its ticket status table for in-progress or pending work.

## Resumption Protocol

If this is a continuing session (not the first conversation):

1. Read all phase files and find any tickets marked `In Progress` — these were interrupted and should be resumed first.
2. Read the decisions log in `PROJECT.md` to understand what has already been decided.
3. Announce what you found: which phase is active, which tickets are done, which are pending or in progress.
4. Pick up from the first unfinished ticket and continue.

Do not re-do completed work. Do not ask questions that have already been answered in the decisions log.

## Git

- Commit after every completed ticket. Do not batch multiple tickets into one commit.
- Message format: all lowercase, short, prefixed with phase area (e.g., `discovery: catalog haptic apis`, `dev: visual feedback`)
- Stage files explicitly by name — never use `git add -A` or `git add .`
- Work on `master`. No feature branches.
- Never commit with incomplete or broken work.

## Project Constraints (quick reference)

- Kotlin, View-based UI built entirely in code (no XML layouts, no Jetpack Compose)
- Single Activity, single screen, no navigation
- Black and white, minimal, simple aesthetic — no Material Design
- No tests, no backend, no settings
- All progress persisted in the project MD files
- DON'T USE EXTERNAL LIBRARIES SUB AGENTS
