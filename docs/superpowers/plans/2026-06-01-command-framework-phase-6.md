# Command Framework Phase 6 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make tokenization robust enough for common command-line input while preserving the current string-token API.

**Architecture:** Keep tokenizer internals in `core` for now. This phase improves behavior without introducing a public `TokenizedInput` model yet.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5.

---

### Task 18: Quote And Escape Tokenization

**Files:**
- Modify: `core/src/main/java/dev/buildmycommand/core/CommandFramework.java`
- Modify tests under `core/src/test/java/...`

- [x] Write failing tests for single-quoted strings, escaped double quotes, escaped single quotes, escaped backslashes, and trailing escape errors.
- [x] Support single and double quoted strings.
- [x] Support backslash escaping inside and outside quotes.
- [x] Preserve existing unclosed quote behavior with clearer quote-specific coverage.
- [x] Verify existing dispatch, route DSL, suggestions, permissions, and help tests still pass.
