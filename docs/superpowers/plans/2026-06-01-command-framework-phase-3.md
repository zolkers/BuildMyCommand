# Command Framework Phase 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the public manual command tree API described in the master plan, then tighten command metadata so help/schema and adapters can build on stable contracts.

**Architecture:** Keep platform-neutral command declarations in `api`. The manual API should compile into the same registry/tree model used by builder, route DSL, and annotations. The first version should stay small and avoid exposing core internals.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 10: Manual Command Tree API

**Files:**
- Modify: `api/src/main/java/dev/buildmycommand/api/CommandRegistry.java`
- Create: `api/src/main/java/dev/buildmycommand/api/CommandNode.java`
- Create: `api/src/main/java/dev/buildmycommand/api/Commands.java`
- Create: `api/src/main/java/dev/buildmycommand/api/ArgumentSpec.java`
- Create: `api/src/main/java/dev/buildmycommand/api/Arguments.java`
- Create: `api/src/main/java/dev/buildmycommand/api/FlagSpec.java`
- Create: `api/src/main/java/dev/buildmycommand/api/Flags.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/SimpleCommandRegistry.java`
- Modify: `core/src/test/java/dev/buildmycommand/core/CommandFrameworkTest.java`

- [x] Write failing tests for registering `Commands.literal("ban")...build()` through `registry.register(node)`.
- [x] Write failing tests for manual API validation: nonblank names, duplicate child/alias/flag rejection, required arguments before optional arguments, one greedy argument, greedy argument last, and missing option values during dispatch.
- [x] Implement immutable public specs/factories for literals, required/optional/greedy arguments, boolean flags, value options through `Flags.option(...)`, aliases, children, and handlers using the existing public `CommandRegistry.CommandExecutor` contract.
- [x] Compile public `CommandNode` instances into the existing internal registry model.
- [x] Verify manual, builder, route DSL, and annotation registration still produce equivalent dispatch/help/schema behavior for representative nested literals, aliases, required arguments, greedy optional arguments, boolean flags, value options, help, and schema.

### Task 11: Command Metadata Basics

**Files:**
- Modify: `api/src/main/java/dev/buildmycommand/api/CommandRegistry.java`
- Modify: `api/src/main/java/dev/buildmycommand/api/CommandNode.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify: `core/src/main/java/dev/buildmycommand/core/SimpleCommandRegistry.java`
- Modify tests under `core/src/test/java/...`

- [x] Write failing tests for description and permission metadata on builder/manual/route declarations.
- [x] Add metadata storage without enforcing permissions yet.
- [x] Surface descriptions in help/schema in a deterministic format.
- [x] Verify metadata does not change dispatch semantics.
