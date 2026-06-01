# Command Framework MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first executable slice of the universal Java command framework from the master plan.

**Architecture:** Public contracts live in `api`, implementation in `core`, and fluent testing helpers in `testkit`. All declaration styles compile toward the same command model over time; the first slice focuses on manual/builder registration and dispatch.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 1: Build Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `api/build.gradle.kts`
- Create: `core/build.gradle.kts`
- Create: `testkit/build.gradle.kts`

- [ ] Create a Gradle multi-module Java build with `api`, `core`, and `testkit`.
- [ ] Configure Java 21 and JUnit 5.
- [ ] Verify `gradle test` can run once a wrapper or local Gradle distribution is available.

### Task 2: Minimal Dispatch API

**Files:**
- Create tests under `core/src/test/java/dev/buildmycommand/core`
- Create API contracts under `api/src/main/java/dev/buildmycommand/api`
- Create core implementation under `core/src/main/java/dev/buildmycommand/core`

- [ ] Write failing tests for registering `ping` and receiving `Pong`.
- [ ] Implement `CommandFramework`, `CommandRegistry`, `CommandContext`, `CommandSource`, `CommandResult`, and `Results`.
- [ ] Verify the test passes.

### Task 3: Tokenizer And Arguments

**Files:**
- Extend tests under `core/src/test/java/dev/buildmycommand/core`
- Extend API/core parser classes.

- [ ] Write failing tests for quoted tokens, required arguments, optional arguments, greedy arguments, and invalid integer parsing.
- [ ] Implement tokenizer and argument registry.
- [ ] Verify all core tests pass.

### Task 4: Subcommands And Aliases

**Files:**
- Extend `CommandNode` and registry tests.

- [ ] Write failing tests for nested literal routing and aliases.
- [ ] Implement tree routing with literal priority.
- [ ] Verify all core tests pass.

### Task 5: Flags And Suggestions

**Files:**
- Extend parser/suggestion tests and core classes.

- [ ] Write failing tests for boolean flags, valued options, short aliases, and command suggestions.
- [ ] Implement flag parsing and suggestion generation.
- [ ] Verify all core tests pass.
