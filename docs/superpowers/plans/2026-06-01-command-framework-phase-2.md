# Command Framework Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the working MVP toward the master plan with route DSL registration, help/schema/debug output, annotation registration, and a first terminal adapter.

**Architecture:** Keep the `api` contracts small, add implementation in `core`, and introduce optional modules only when the feature is isolated enough. The route DSL must compile into the same registry model as builder registration.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 6: Route DSL Registration

**Files:**
- Modify: `api/src/main/java/dev/buildmycommand/api/CommandRegistry.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/SimpleCommandRegistry.java`
- Modify: `core/src/test/java/dev/buildmycommand/core/CommandFrameworkTest.java`

- [ ] Write failing tests for `registry.route("ban <target:String> [reason:String...] [--silent|-s]")`.
- [ ] Implement route pattern parsing for literals, required typed args, optional typed args, greedy string args, boolean flags, valued options, and short aliases.
- [ ] Verify route DSL dispatch compiles to existing command behavior.

### Task 7: Help And Schema

**Files:**
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/SimpleCommandRegistry.java`
- Modify: `core/src/test/java/dev/buildmycommand/core/CommandFrameworkTest.java`

- [ ] Write failing tests for usage/help text and a simple schema export.
- [ ] Implement `framework.help("command path")` and `framework.schema()` using the command tree.
- [ ] Verify output is deterministic.

### Task 8: Annotation API Module

**Files:**
- Create: `annotations/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `annotations/src/main/java/dev/buildmycommand/annotation/*`
- Create tests under `annotations/src/test/java/...`

- [ ] Write failing tests for scanning `@Command` methods with `@Arg` and `@Flag`.
- [ ] Compile annotations into the existing registry builder.
- [ ] Verify annotated command dispatch.

### Task 9: Terminal Adapter Module

**Files:**
- Create: `terminal-adapter/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `terminal-adapter/src/main/java/dev/buildmycommand/terminal/*`
- Create tests under `terminal-adapter/src/test/java/...`

- [ ] Write tests for mapping terminal input to framework dispatch.
- [ ] Implement a minimal adapter around `InputStream`/`PrintStream` without adding platform dependencies.
- [ ] Verify adapter can execute one line and print a reply.
