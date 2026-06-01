# Command Framework Phase 7 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand built-in argument parsing beyond `String` and `Integer` while keeping parse errors friendly and deterministic.

**Architecture:** Keep the parser registry private to `CommandFramework` for this phase. A later phase can expose custom parser registration once the public API shape is clearer.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 19: Additional Built-In Parsers

**Files:**
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify tests under `core/src/test/java/...`

- [x] Write failing tests for `Long`, `Double`, `Boolean`, `UUID`, and enum arguments/options.
- [x] Add primitive-wrapper support for `long`, `double`, and `boolean`.
- [x] Add enum parsing by constant name.
- [x] Keep parse error messages type-specific and stable.
- [x] Verify route DSL/manual/builder declarations can use the added supported types where type lookup exists.
