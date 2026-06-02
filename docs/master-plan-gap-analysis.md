# Master Plan Gap Analysis

This audit compares the current repository against `java_command_framework_master_plan (1).md`.

## Executive Summary

The repository currently has a solid early command engine slice: public API basics, core dispatch, typed parsing for common Java types, nested subcommands, aliases, flags/options, route DSL parsing, annotation scanning, help/schema strings, a minimal testkit, a minimal terminal adapter, Minecraft adapter scaffolding, and IntelliJ DSL highlighting.

The master plan is much broader. The largest missing areas are not only annotations. The biggest gaps are the universal platform model (`CommandInput`, `CommandMessage`, `CommandPlatform`, adapter SDK), configurable parser/suggestion/middleware/error pipelines, rich suggestions with replacement ranges, schema/debug tooling, full adapter implementations, and project quality/CI/release infrastructure.

## Legend

- `Done`: implemented in a usable form.
- `Partial`: present, but narrower than the plan.
- `Missing`: not implemented yet.

## Public APIs

| Area | Status | Notes |
| --- | --- | --- |
| Annotation API | Partial | `@Command`, `@Route`, `@Subcommand`, `@Arg`, `@OptionalArg`, `@Greedy`, `@Default`, `@Flag`, `@Option`, `@Alias`, `@Description`, `@Permission` exist. Missing cooldowns, rich suggestion annotations, hidden/example metadata, richer inference, and MethodCommandBinder separation. |
| Builder API | Partial | `registry.command(...)` supports literals, subcommands, args, flags/options, aliases, metadata, handlers. Missing richer option metadata, defaults, suggestions, middleware, case strategy, hidden commands, and lifecycle hooks. |
| Manual API | Partial | `Commands.literal`, `Arguments`, `Flags`, and `CommandNode` exist. Missing richer records from the plan: metadata map, middleware, suggestion providers, hidden/group nodes, route identity, defaults, required options, and public docs. |
| Route DSL API | Partial | DSL supports literals, typed args, optional/greedy args, flags/options, aliases, nested routes, and validation. Missing command literal aliases, several built-in types, inline enum/constraints/metadata, canonicalization API, conflict analyzer integration, and standalone `dsl` module. |

## Core Model

| Area | Status | Notes |
| --- | --- | --- |
| `CommandFramework` builder | Missing | Current API is `CommandFramework.create()`. Plan expects builder config for parsers, middleware, errors, suggestions, case mode, match strategy, etc. |
| `CommandInput` | Missing | Dispatch still takes raw `String`. Plan expects source, raw input, cursor, prefix, and platform. Minecraft has its own `MinecraftInvocation`, but core has no universal input object. |
| `CommandPlatform` | Missing | No platform capability model in `api`. |
| `CommandMessage` | Missing | `CommandResult` currently wraps `Optional<String>`. Plan expects rich messages with level and metadata. |
| `CommandResult` rich sealed model | Partial | Current result has status + optional string. Missing `CommandMessage`, typed success/failure records, translatable messages, and adapter-aware rendering. |
| `CommandSource` | Partial | Has `reply(String)` and `hasPermission`. Missing `id`, `name`, `locale`, `unwrap`, `metadata`, and `reply(CommandMessage)`. |
| `CommandContext` | Partial | Supports source, raw input string, args, flags/options. Missing `CommandInput`, path, reply helpers, platform object unwrap, locale/message helpers. |
| `CommandGraph` | Missing | The registry has internal root nodes, but no explicit public/internal graph object. |

## Parsing And Routing

| Area | Status | Notes |
| --- | --- | --- |
| Tokenizer | Partial | Handles quotes, escaping, whitespace, and errors. Missing public token model with start/end/quoted metadata and cursor-aware tokenization in the public API. |
| Argument parser registry | Partial | Built-in parser registry exists internally. Missing public `ArgumentParser<T>`, public `ArgumentRegistry`, custom parser registration, parse contexts, and parser-provided suggestions. |
| Built-in parser coverage | Partial | Supports `String`, integer, long, double, boolean, enum, UUID. Missing `Float`, `Duration`, `LocalDate`, `LocalDateTime`, `Path`, `URI`, `URL`, and custom domain parser examples. |
| Router | Partial | Nested literals, aliases, parent args, and literal priority exist. Missing public route debug model, ambiguity warnings, configurable match strategies, fuzzy matching, and route inspector. |
| Strict vs lenient parsing | Missing | No global parsing mode. |
| Case sensitivity strategies | Missing | No `CaseMode`, `MatchStrategy`, or case-insensitive routing/suggestions config. |

## Flags And Options

| Area | Status | Notes |
| --- | --- | --- |
| Boolean flags | Done | Long and short aliases supported. |
| Value options | Done | Long and short aliases supported. |
| Flags anywhere | Partial | Supported in common command patterns, but no explicit formal mode/config. |
| Defaults | Partial | Handlers use `Optional.orElse`; specs do not carry default values. |
| Required options | Missing | `FlagSpec` has no required flag/value semantics. |
| Flag descriptions | Missing | `FlagSpec` has no description field. |
| Grouped short flags | Missing | `-xvf` style not implemented. |
| Flag/option suggestion providers | Missing | No per-flag value suggestion API. |

## Suggestions And Help

| Area | Status | Notes |
| --- | --- | --- |
| Basic suggestions | Partial | `framework.suggest(...)` returns `List<String>`. |
| Rich `Suggestion` model | Missing | No replacement ranges, tooltip, type, priority, or rich context. |
| Suggestion providers | Missing | No public `SuggestionProvider` or parser-driven suggestions. |
| Permission-aware suggestions | Partial | Some permission behavior exists for dispatch/help; suggestions are not the full plan. |
| Help generation | Partial | Usage and descriptions exist. Missing rich help pages, subcommand listings, examples, source-aware hiding rules beyond current basics, and generated docs. |
| Schema export | Partial | String schema exists. Missing dedicated schema module, JSON/YAML export, OpenAPI-like docs, Mermaid graph export. |
| Debug/inspection API | Missing | No route inspector, conflict analyzer, or debug trace API. |

## Middleware, Errors, Async, Lifecycle

| Area | Status | Notes |
| --- | --- | --- |
| Middleware | Missing | No `CommandMiddleware`, middleware chain, inherited middleware, logging, cooldown, or permission middleware. Permissions are direct node metadata checks. |
| Cooldowns | Missing | No `@Cooldown`, cooldown manager, cooldown middleware, or per-source cooldown policy. |
| Error handling | Partial | Dispatch returns stable failure messages for parser errors. Missing `CommandErrorHandler`, custom error rendering, exception mapping, and adapter-specific errors. |
| Async support | Missing | Dispatch is synchronous and returns `CommandResult`, not `CompletionStage<CommandResult>`. |
| Command lifecycle | Missing | No register/unregister/update/listener lifecycle API. |
| Thread safety | Partial | Immutable specs help, but no documented concurrency contract or concurrent registry strategy. |

## Adapters

| Area | Status | Notes |
| --- | --- | --- |
| Adapter SDK | Missing | No generic `CommandAdapter`, `AdapterConfig`, platform capabilities, or adapter contract tests in `api`. |
| Terminal adapter | Partial | Has `runOnce` with input/output streams. Missing prompt loop, prefix config, autocomplete, history/JLine, lifecycle, and richer rendering. |
| Generic game chat adapter | Missing | No platform-neutral game chat adapter. |
| Discord adapter | Missing | No text/slash command adapter, ephemeral replies, Discord permissions, or slash sync. |
| Minecraft common | Partial | Good scaffolding for profiles, invocation normalization, source descriptors, result rendering, and edge cases. Missing generic Minecraft adapter config, native command registration contracts, suggestion bridge, version bridge, and message renderer variants. |
| Bukkit/Spigot/Paper/Velocity/Fabric/Forge/NeoForge | Partial | Modules exist with lightweight profile adapters. Missing real native API integration, command registration, lifecycle unregister, permission source mapping, native tab completion, Adventure/MiniMessage renderers, async completions where available. |
| Minestom/Sponge | Missing | Listed in the plan but no modules. |
| LuckPerms extension | Missing | Optional future extension not present. |

## Modules

| Module From Plan | Status | Current State |
| --- | --- | --- |
| `api` | Partial | Exists, but lacks several planned primitives. |
| `core` | Partial | Exists and works for early engine features. |
| `dsl` | Missing | DSL currently lives under `core.route`. |
| `annotations` | Partial | Exists, but incomplete versus annotation plan. |
| `builder` | Missing as separate module | Builder API is part of `api`/`core`. This may be acceptable, but the plan names it separately. |
| `schema` | Missing | Schema exporter lives in `core.help` as a string exporter. |
| `testkit` | Partial | Dispatch assertions exist. Missing suggestions, route assertions, fake source/platform, richer fluent assertions. |
| `terminal-adapter` | Partial | Minimal. |
| `adapters` | Partial | Minecraft family exists as scaffolding. Generic adapter SDK missing. |
| `discord-adapter` | Missing | Not present. |
| `studio` | Missing | Not present. |
| `intellij-plugin` | Extra/Partial | Not in the original module list, but aligns with DSL tooling. Missing parser inspections/completion. |

## Annotation API Detailed Gaps

Current annotations:

- `@Command`
- `@Route`
- `@Subcommand`
- `@Arg`
- `@OptionalArg`
- `@Greedy`
- `@Default`
- `@Flag`
- `@Option`
- `@Alias`
- `@Permission`
- `@Description`

Missing from the plan:

- `@CommandGroup`
- `@Cooldown`
- `@Suggest`
- `@Hidden`
- `@Example`
- `@Usage`
- `@Require` or richer permission expression annotations
- parameter inference without `@Arg` when compiled with `-parameters`
- `MethodCommandBinder` as a separate class from the scanner
- annotation-to-`CommandNode` compilation without registering directly into the mutable registry
- route/method consistency validation for `@Route`, for example missing method binding for a DSL argument or binding to a non-existent route name

## DSL Detailed Gaps

Implemented:

- literals
- required args
- optional args
- greedy string args
- boolean flags
- value options
- short aliases for flags/options
- nested routes
- registration-time validation

Missing:

- command aliases in DSL, such as `ban|block`
- multiple literal alternatives
- inline enum syntax, such as `<mode:enum(EASY,NORMAL,HARD)>`
- inline constraints, such as `<amount:int{1..64}>`
- inline metadata blocks, if kept
- canonical route identity per command
- route canonicalization API
- route conflict analyzer
- route debug output
- DSL parser module split
- IntelliJ validation/completion against the actual DSL grammar

## Quality, CI, And Release

| Area | Status | Notes |
| --- | --- | --- |
| JaCoCo | Partial | Configured, but thresholds are currently low and `intellij-plugin` is excluded from verification. |
| Formatter | Missing | No google-java-format or Checkstyle formatter gate. |
| Static analysis | Missing | No SpotBugs, Error Prone, NullAway/Checker Framework, or Sonar config. |
| Mutation testing | Missing | No PIT config. |
| CI | Missing | No GitHub Actions workflows in repo. |
| Java matrix | Missing | Build uses Java 21 baseline, but plan discusses Java 17/21 matrix. |
| Publishing | Missing | No Maven Central/GitHub Packages publication config. |
| API versioning policy | Partial | Version exists, but no binary compatibility tooling or SemVer docs. |

## Documentation Gaps

Existing docs cover README, IntelliJ plugin, Minecraft adapter, and superpowers phase plans.

Missing docs from the plan:

- Getting Started
- Core Concepts
- CommandSource
- CommandInput
- CommandMessage
- Arguments
- Flags
- Subcommands
- Autocomplete
- DSL reference
- Annotations reference
- Builder API reference
- Manual API reference
- Adapter SDK
- Terminal adapter
- Discord adapter
- Minecraft native setup guides
- Testing guide
- Advanced routing
- Error handling
- Schema export
- Release/publishing guide

## Recommended Next Implementation Slices

### Slice 1: Universal Core Primitives

Add `CommandInput`, `CommandMessage`, message levels, richer `CommandSource`, and update `CommandContext`/`CommandResult` carefully. This unlocks adapters, rich errors, and platform-independent rendering.

### Slice 2: Public Parser And Suggestion API

Add public `ArgumentParser<T>`, `ArgumentRegistry`, `Suggestion`, `SuggestionProvider`, and rich suggestion contexts. Upgrade `framework.suggest` from `List<String>` to a rich model while keeping a convenience string API if useful.

### Slice 3: Annotation Completeness

Add `@Alias`, `@OptionalArg`, `@Greedy`, `@Cooldown`, `@Default`, `@Suggest`, `@Hidden`, `@Example`, and route/method consistency validation. Extract scanner internals into a real `MethodCommandBinder`.

### Slice 4: Adapter SDK Before More Native Adapters

Add a generic adapter contract in `api` or `adapters`: map native event to `CommandInput`, map native source to `CommandSource`, dispatch/suggest, render `CommandResult`. Then retrofit terminal and Minecraft common to this model.

### Slice 5: Schema And Debug Module

Split or formalize schema/debug APIs: JSON schema, route inspector, conflict analyzer, canonical route output, and Mermaid graph export.

### Slice 6: Native Minecraft Integrations

After the adapter SDK is in place, implement real native modules one by one. Recommended order: Spigot/Bukkit, Paper, Velocity, then Minestom/Sponge or mod loaders depending on priority.

### Slice 7: Quality Infrastructure

Add formatter, Checkstyle/SpotBugs/Error Prone or equivalent, stricter JaCoCo thresholds per module, and GitHub Actions with Java matrix.

## Highest-Risk Gaps

1. Adding adapters before `CommandInput`/`CommandMessage` will keep creating platform-specific mini-models.
2. Adding more annotations before route/method validation will make annotation DSL easy to misuse.
3. Native Minecraft integration before adapter SDK risks duplicating dispatch/suggest/render code per platform.
4. Keeping parser registry internal blocks custom domain types like `User`, `Rank`, `World`, and `Item`.
5. Keeping suggestions as `List<String>` blocks serious IDE/Minecraft/Discord autocomplete.
