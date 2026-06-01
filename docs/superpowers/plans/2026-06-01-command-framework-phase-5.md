# Command Framework Phase 5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first permission enforcement path and improve permission-aware user-facing discovery without introducing a full middleware system yet.

**Architecture:** Permission checks stay platform-neutral through `CommandSource.hasPermission(String)`. Metadata continues to live on command nodes. Adapters can later map native platform permissions into `CommandSource`.
Permissions are inherited along the matched command path: any permission declared on a matched parent or final command must be granted before dispatch or discovery proceeds.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 15: Dispatch Permission Enforcement

**Files:**
- Modify: `api/src/main/java/dev/buildmycommand/api/CommandSource.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify tests under `core/src/test/java/...`

- [x] Write failing tests for denied and allowed permissioned command dispatch.
- [x] Add `CommandSource.hasPermission(String)` defaulting to `true`.
- [x] Return a failure result when the matched executable command or matched parent command path has a permission and the source lacks it.
- [x] Verify commands without permissions and sources using the default method preserve existing dispatch behavior.

### Task 16: Permission-Aware Suggestions

**Files:**
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify tests under `core/src/test/java/...`

- [x] Write failing tests for hiding root commands, subcommands, and flags/options when the current source cannot execute the relevant command.
- [x] Filter suggestions using the nearest command permission available.
- [x] Verify existing suggestions remain deterministic for permitted sources.

### Task 17: Permission-Aware Help

**Files:**
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify tests under `core/src/test/java/...`

- [ ] Add a source-aware help overload while keeping the existing `help(String)` convenience method.
- [ ] Hide or deny permissioned command help for sources without permission.
- [ ] Verify existing help output stays unchanged for the convenience overload.
