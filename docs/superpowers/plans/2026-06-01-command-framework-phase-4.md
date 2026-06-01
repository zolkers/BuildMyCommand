# Command Framework Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring annotations and examples up to the richer public command model, then add lightweight project quality infrastructure.

**Architecture:** Annotation metadata must compile into the same registry builder metadata as manual, route DSL, and builder declarations. Quality tooling should be Gradle-native and avoid platform dependencies.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 12: Annotation Metadata

**Files:**
- Create: `annotations/src/main/java/dev/buildmycommand/annotation/Description.java`
- Create: `annotations/src/main/java/dev/buildmycommand/annotation/Permission.java`
- Modify: `annotations/src/main/java/dev/buildmycommand/annotation/AnnotationCommandScanner.java`
- Modify tests under `annotations/src/test/java/...`

- [x] Write failing tests for annotated command descriptions and permissions appearing in help/schema.
- [x] Implement `@Description` and `@Permission` on command methods with `@Retention(RUNTIME)` and `@Target(METHOD)`.
- [x] Reject blank annotated description/permission values with clear scanner errors, matching builder/manual/route metadata validation.
- [x] Verify annotated metadata does not change dispatch semantics.

### Task 13: Examples Module

**Files:**
- Modify: `settings.gradle.kts`
- Create: `examples/build.gradle.kts`
- Create examples under `examples/src/main/java/...`

- [x] Add compile-tested minimal builder/manual/route DSL/annotation/terminal example classes with no runtime integration assertions.
- [x] Keep examples dependency-only; no runtime platform dependencies beyond existing modules.
- [x] Verify examples compile in the normal build.

### Task 14: Coverage Baseline

**Files:**
- Modify: root `build.gradle.kts`
- Modify module build files as needed.

- [ ] Add JaCoCo report generation for Java modules.
- [ ] Add a conservative coverage verification baseline that current tests pass, excluding the compile-only `examples` module.
- [ ] Verify `test` and coverage tasks pass.
