# BuildMyCommand Completion Roadmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish BuildMyCommand into a clean, modular, enterprise-grade command framework with a stable core, rich DSL/annotations, real adapters, IntelliJ tooling, docs, tests, CI, and release infrastructure.

**Architecture:** Keep `api` as the stable public contract, `core` as the engine, `annotations` as a compiler/binder into public command models, `adapters` as thin platform bridges, and `intellij-plugin` as DSL/annotation tooling. Do not let platform details leak into `core`; adapters convert native inputs into framework primitives and render framework results back to native APIs.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5, JaCoCo, Brigadier for Minecraft tree projection, IntelliJ Platform SDK, TextMate/Java PSI injection for DSL highlighting.

---

## Current Baseline

Already present:

- Core command registration, dispatch, typed arguments, nested routes, aliases, flags/options, help/schema strings.
- Route DSL with root/subcommand aliases, optional/greedy args, options and short aliases.
- Annotation scanner with `@Command`, `@Route`, `@Subcommand`, `@Alias`, `@CaseInsensitive`, args/options/flags/defaults.
- Minecraft common model, Brigadier projection, native command adapter abstraction for Spigot/Paper/Bungee/Velocity-style label+args backends.
- IntelliJ plugin with DSL highlighting/injection and install scripts.

Main architectural risks remaining:

- Platform adapter models still sit partly beside core instead of on a universal adapter SDK.
- Suggestions are not rich enough for serious Minecraft/Discord/IDE completion.
- Annotation scanner is becoming too large and should be split into binder/compiler/validator units.
- Native Minecraft modules are factories/profiles, not real server API integrations yet.
- No CI/release-quality guardrails yet.

---

## Slice 1: Universal Core Primitives

**Goal:** Make the core model platform-ready so every adapter uses the same input, output, source, message, and context objects.

**Files:**
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandInput.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandSource.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandResult.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandMessage.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/MessageLevel.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandContext.java`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/CommandFramework.java`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/dispatch/CommandDispatcher.java`
- Test: `modules/core/src/test/java/dev/riege/buildmycommand/core/CommandFrameworkTest.java`
- Test: `modules/adapters/minecraft/common/src/test/java/dev/riege/buildmycommand/adapters/minecraft/common/MinecraftCommandBridgeTest.java`

- [ ] **Step 1: Add failing tests for universal dispatch input**

Add tests proving:

- `CommandFramework.dispatch(CommandInput)` preserves raw input, normalized input, cursor, prefix, and platform.
- `CommandContext.input()` exposes the same object.
- Existing `dispatch(source, "command")` convenience API still works.

Run:

```powershell
.\gradlew.bat :core:test --tests dev.riege.buildmycommand.core.CommandFrameworkTest
```

Expected: FAIL until context/input plumbing is complete.

- [ ] **Step 2: Upgrade `CommandInput` and `CommandContext`**

Add stable fields:

- `CommandSource source`
- `String rawInput`
- `String normalizedInput`
- `int cursor`
- `String prefix`
- `CommandPlatform platform`

Keep convenience constructors or static factories so existing tests remain readable.

- [ ] **Step 3: Upgrade result/message rendering**

Make `CommandResult` carry `CommandMessage` while preserving string convenience factories in `Results`.

Required compatibility:

- `Results.success("ok")`
- `Results.failure("bad")`
- `Results.silent()`
- `result.reply()` still returns `Optional<String>` for now.

- [ ] **Step 4: Add source metadata without breaking defaults**

Extend `CommandSource` with default methods:

- `id()`
- `name()`
- `locale()`
- `unwrap(Class<T>)`
- `metadata(String key)`
- `reply(CommandMessage message)`

Keep existing anonymous `new CommandSource() {}` tests compiling through defaults.

- [ ] **Step 5: Run tests and commit**

Run:

```powershell
.\gradlew.bat :api:test :core:test :adapters:minecraft:common:test
git add modules/api modules/core modules/adapters/minecraft/common
git commit -m "feat: add universal command primitives"
```

---

## Slice 2: Public Parser And Rich Suggestion API

**Goal:** Make custom argument parsers and rich completions public, composable, and adapter-friendly.

**Files:**
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/ArgumentParser.java`
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/ArgumentParseContext.java`
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/ArgumentParseResult.java`
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/SuggestionProvider.java`
- Modify: `modules/api/src/main/java/dev/riege/buildmycommand/api/Suggestion.java`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/parse/ArgumentParserRegistry.java`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/help/SuggestionEngine.java`
- Test: `modules/core/src/test/java/dev/riege/buildmycommand/core/CommandFrameworkTest.java`

- [ ] **Step 1: Add failing tests for custom parser registration**

Test a custom `Rank` parser:

- accepts `admin`
- rejects `owner`
- suggests `admin`, `mod`, `helper`

Run:

```powershell
.\gradlew.bat :core:test --tests dev.riege.buildmycommand.core.CommandFrameworkTest
```

Expected: FAIL until public parser registration exists.

- [ ] **Step 2: Add public parser contracts**

Expose parser interfaces in `api`, then adapt internal parser registry to use them.

Required behavior:

- Parser receives token text and context.
- Parser returns typed value or stable error message.
- Parser can provide suggestions.

- [ ] **Step 3: Upgrade suggestions**

Ensure `Suggestion` supports:

- replacement text
- tooltip
- replacement start/end
- type
- priority

Keep `framework.suggest(...)` as a convenience `List<String>` API backed by `suggestRich(...)`.

- [ ] **Step 4: Add parser coverage**

Add built-ins:

- `Float`
- `Duration`
- `LocalDate`
- `LocalDateTime`
- `Path`
- `URI`
- `URL`

- [ ] **Step 5: Run tests and commit**

```powershell
.\gradlew.bat :api:test :core:test
git add modules/api modules/core
git commit -m "feat: expose parsers and rich suggestions"
```

---

## Slice 3: DSL Module Split And Route Analysis

**Goal:** Move DSL parsing out of `core.route` into a dedicated module with canonicalization, validation, and conflict analysis.

**Files:**
- Create module: `modules/dsl`
- Move/own: `modules/core/src/main/java/dev/riege/buildmycommand/core/route/*`
- Create: `modules/dsl/src/main/java/dev/riege/buildmycommand/dsl/RouteParser.java`
- Create: `modules/dsl/src/main/java/dev/riege/buildmycommand/dsl/RouteCanonicalizer.java`
- Create: `modules/dsl/src/main/java/dev/riege/buildmycommand/dsl/RouteConflictAnalyzer.java`
- Modify: `settings.gradle.kts`
- Modify: `modules/core/build.gradle.kts`
- Test: `modules/dsl/src/test/java/dev/riege/buildmycommand/dsl/RouteParserTest.java`
- Test: `modules/core/src/test/java/dev/riege/buildmycommand/core/CommandFrameworkTest.java`

- [ ] **Step 1: Add failing DSL module tests**

Test:

- `ban|block <target:String>`
- `rank|roles set|put`
- `<mode:enum(EASY,NORMAL,HARD)>`
- `<amount:int{1..64}>`
- conflict detection for equivalent alias routes.

- [ ] **Step 2: Create `dsl` module**

Add the module to `settings.gradle.kts`, wire dependencies, and move public DSL types under `dev.riege.buildmycommand.dsl`.

- [ ] **Step 3: Keep core compatibility**

Core registry `.route("...")` should delegate to `dsl` and keep existing public behavior.

- [ ] **Step 4: Add canonical route identity**

Every parsed route should expose:

- canonical root
- canonical path
- aliases
- argument names/types
- option names/aliases

- [ ] **Step 5: Run tests and commit**

```powershell
.\gradlew.bat :dsl:test :core:test :annotations:test :intellij-plugin:test
git add settings.gradle.kts modules/dsl modules/core modules/annotations modules/intellij-plugin
git commit -m "feat: split route dsl module"
```

---

## Slice 4: Annotation Compiler Refactor

**Goal:** Remove scanner god-class behavior and compile annotations into command models with validation before registry mutation.

**Files:**
- Modify: `modules/annotations/src/main/java/dev/riege/buildmycommand/annotation/AnnotationCommandScanner.java`
- Create: `modules/annotations/src/main/java/dev/riege/buildmycommand/annotation/binding/MethodCommandBinder.java`
- Create: `modules/annotations/src/main/java/dev/riege/buildmycommand/annotation/binding/AnnotationCommandCompiler.java`
- Create: `modules/annotations/src/main/java/dev/riege/buildmycommand/annotation/binding/AnnotationRouteValidator.java`
- Create annotations:
  - `CommandGroup.java`
  - `Cooldown.java`
  - `Suggest.java`
  - `Hidden.java`
  - `Example.java`
  - `Usage.java`
  - `Require.java`
- Test: `modules/annotations/src/test/java/dev/riege/buildmycommand/annotation/AnnotationCommandScannerTest.java`

- [ ] **Step 1: Add failing tests for missing annotations**

Test:

- `@Hidden` hides help/suggestions.
- `@Example` and `@Usage` appear in help/schema.
- `@Suggest("providerName")` binds parameter suggestions.
- `@Cooldown` emits metadata for middleware.
- `@Require("perm.a && perm.b")` is parsed as permission expression metadata.

- [ ] **Step 2: Extract binder/compiler**

Split responsibilities:

- Scanner discovers classes/methods.
- Binder maps methods/parameters to route specs.
- Compiler produces `CommandNode` or route registration plan.
- Validator checks DSL args match method parameters.

- [ ] **Step 3: Add parameter inference**

When compiled with `-parameters`, allow:

```java
@Command("ban")
void ban(CommandSource source, String target, @Greedy String reason)
```

without requiring `@Arg("target")` on every obvious parameter.

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat :annotations:test :core:test
git add modules/annotations modules/core
git commit -m "feat: compile annotation commands with validation"
```

---

## Slice 5: Middleware, Errors, Cooldowns, Lifecycle

**Goal:** Add cross-cutting command behavior without baking everything into dispatch.

**Files:**
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandMiddleware.java`
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandErrorHandler.java`
- Create: `modules/api/src/main/java/dev/riege/buildmycommand/api/CommandLifecycleListener.java`
- Create: `modules/core/src/main/java/dev/riege/buildmycommand/core/middleware/MiddlewareChain.java`
- Create: `modules/core/src/main/java/dev/riege/buildmycommand/core/middleware/CooldownMiddleware.java`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/dispatch/CommandDispatcher.java`
- Test: `modules/core/src/test/java/dev/riege/buildmycommand/core/CommandFrameworkTest.java`

- [ ] **Step 1: Add failing middleware tests**

Test:

- middleware can block execution
- middleware can wrap execution
- cooldown denies repeated command for same source
- custom error handler maps thrown exception to `CommandResult.failure`

- [ ] **Step 2: Implement middleware chain**

Middleware order:

1. framework global middleware
2. command/node middleware
3. executor
4. error handler around the chain

- [ ] **Step 3: Add lifecycle hooks**

Add events for:

- command registered
- command updated
- command unregistered
- registry rebuilt

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat :api:test :core:test :annotations:test
git add modules/api modules/core modules/annotations
git commit -m "feat: add middleware and command lifecycle"
```

---

## Slice 6: Generic Adapter SDK

**Goal:** Stop every platform from inventing its own bridge shape.

**Files:**
- Create: `modules/adapters/src/main/java/dev/riege/buildmycommand/adapters/CommandAdapter.java`
- Create: `modules/adapters/src/main/java/dev/riege/buildmycommand/adapters/AdapterConfig.java`
- Create: `modules/adapters/src/main/java/dev/riege/buildmycommand/adapters/AdapterRuntime.java`
- Create: `modules/adapters/src/main/java/dev/riege/buildmycommand/adapters/AdapterRenderer.java`
- Modify: `modules/adapters/minecraft/common/*`
- Modify: `modules/terminal-adapter/*`
- Test: `modules/adapters/src/test/java/dev/riege/buildmycommand/adapters/CommandAdapterContractTest.java`
- Test: `modules/adapters/minecraft/common/src/test/java/dev/riege/buildmycommand/adapters/minecraft/common/MinecraftCommandBridgeTest.java`

- [ ] **Step 1: Add failing adapter contract tests**

Contract:

- adapter maps native source into `CommandSource`
- adapter maps native input into `CommandInput`
- adapter dispatches through `CommandFramework`
- adapter renders `CommandResult`
- adapter exposes capabilities and registration labels

- [ ] **Step 2: Retrofit terminal adapter**

Terminal adapter should use the generic SDK for input/source/result instead of a bespoke flow.

- [ ] **Step 3: Retrofit Minecraft common**

`MinecraftNativeCommandAdapter` and `MinecraftBrigadierBridge` should share SDK concepts where useful, while preserving Minecraft-specific edge models.

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat :adapters:test :terminal-adapter:test :adapters:minecraft:common:test
git add modules/adapters modules/terminal-adapter
git commit -m "feat: add generic adapter sdk"
```

---

## Slice 7: Native Minecraft Implementations

**Goal:** Turn Minecraft modules from profiles/factories into real integration surfaces.

**Files:**
- Modify: `modules/adapters/minecraft/spigot/build.gradle.kts`
- Modify: `modules/adapters/minecraft/paper/build.gradle.kts`
- Modify: `modules/adapters/minecraft/bungee/build.gradle.kts`
- Modify: `modules/adapters/minecraft/velocity/build.gradle.kts`
- Use: `modules/adapters/brigadier/build.gradle.kts` for Fabric, Forge, and NeoForge command dispatcher integration.
- Create native registration classes per platform under each module.
- Test: one test class per platform module.

- [ ] **Step 1: Spigot/Bukkit native registration**

Add:

- plugin command registration/unregistration facade
- `CommandExecutor` bridge
- `TabCompleter` bridge
- permission mapping from sender
- alias label registration

Run:

```powershell
.\gradlew.bat :adapters:minecraft:spigot:test
```

Commit:

```powershell
git add modules/adapters/minecraft/spigot
git commit -m "feat: add spigot native command integration"
```

- [ ] **Step 2: Paper native/hybrid registration**

Add:

- Brigadier lifecycle registration facade
- fallback native adapter registration
- documented choice between `BRIGADIER_PROJECTION`, `NATIVE_COMMAND`, and future `HYBRID`

Run:

```powershell
.\gradlew.bat :adapters:minecraft:paper:test
```

Commit:

```powershell
git add modules/adapters/minecraft/paper
git commit -m "feat: add paper command integration"
```

- [ ] **Step 3: Velocity native registration**

Add:

- simple command adapter
- Brigadier command adapter where possible
- proxy permission source
- async suggestion handling if API requires it

- [ ] **Step 4: Bungee native registration**

Add:

- command registration/unregistration
- tab completion bridge
- proxy sender renderer

- [ ] **Step 5: Brigadier mod-loader integration**

Add real registration helpers around:

- Document direct `BrigadierCommandAdapter` use from Fabric command callbacks.
- Document direct `BrigadierCommandAdapter` use from Forge and NeoForge `RegisterCommandsEvent`.

Keep Brigadier projection explicit and document case-sensitivity limits.

- [ ] **Step 6: Add missing Minecraft platforms**

Add modules if still desired:

- `modules/adapters/minecraft/minestom`
- `modules/adapters/minecraft/sponge`

---

## Slice 8: IntelliJ Plugin Completion, Validation, And Theme Polish

**Goal:** Make the plugin useful beyond coloring strings.

**Files:**
- Modify: `modules/intellij-plugin/src/main/java/dev/riege/buildmycommand/intellij/*`
- Modify: `modules/intellij-plugin/src/main/resources/META-INF/plugin.xml`
- Modify: `modules/intellij-plugin/src/main/resources/textmate/*`
- Test: `modules/intellij-plugin/src/test/java/dev/riege/buildmycommand/intellij/*`
- Modify: `scripts/setup-intellij-plugin.ps1`
- Modify: `scripts/setup-intellij-plugin.sh`

- [ ] **Step 1: Add DSL validation inspections**

Inspections:

- unknown type
- malformed option
- duplicate alias
- required arg after optional arg
- non-string greedy arg

- [ ] **Step 2: Add completion**

Completion targets:

- type names after `:`
- option aliases after `|-`
- annotation route strings
- builder `.route("...")` strings

- [ ] **Step 3: Add annotation-aware references**

Resolve route argument names to method parameters when feasible.

- [ ] **Step 4: Add theme settings docs and verification**

Keep scripts installing the local plugin and document restart/reload behavior.

- [ ] **Step 5: Run tests and commit**

```powershell
.\gradlew.bat :intellij-plugin:test :intellij-plugin:buildPlugin
git add modules/intellij-plugin scripts docs/intellij-plugin.md
git commit -m "feat: add intellij dsl completion and inspections"
```

---

## Slice 9: Schema, Debug, And Studio Foundations

**Goal:** Make routes inspectable and exportable for humans, tests, adapters, and future UI/studio tooling.

**Files:**
- Create module: `modules/schema`
- Create: `modules/schema/src/main/java/dev/riege/buildmycommand/schema/CommandSchemaExporter.java`
- Create: `modules/schema/src/main/java/dev/riege/buildmycommand/schema/RouteInspector.java`
- Create: `modules/schema/src/main/java/dev/riege/buildmycommand/schema/ConflictReport.java`
- Modify: `settings.gradle.kts`
- Modify: `modules/core/src/main/java/dev/riege/buildmycommand/core/help/SchemaExporter.java`
- Test: `modules/schema/src/test/java/dev/riege/buildmycommand/schema/CommandSchemaExporterTest.java`

- [ ] **Step 1: Add JSON schema export tests**

Schema must include:

- command path
- aliases
- args/options
- permissions
- descriptions
- examples/usages
- hidden flag
- suggestion metadata

- [ ] **Step 2: Add route inspector**

Expose a debug trace:

- tokens
- selected literal/alias
- parsed args
- parsed options
- permission checks
- executor target

- [ ] **Step 3: Add Mermaid graph export**

Generate a stable command graph for docs.

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat :schema:test :core:test
git add settings.gradle.kts modules/schema modules/core
git commit -m "feat: add schema and route inspection"
```

---

## Slice 10: Terminal Adapter Completion

**Goal:** Make terminal adapter a serious local/debug platform.

**Files:**
- Modify: `modules/terminal-adapter/src/main/java/dev/riege/buildmycommand/terminal/*`
- Test: `modules/terminal-adapter/src/test/java/dev/riege/buildmycommand/terminal/*`

- [ ] **Step 1: Add prompt loop tests**

Test:

- repeated command input
- exit command
- rendered success/failure
- history disabled by default

- [ ] **Step 2: Add optional JLine integration**

Add history and autocomplete behind optional dependency/config.

- [ ] **Step 3: Run tests and commit**

```powershell
.\gradlew.bat :terminal-adapter:test
git add modules/terminal-adapter
git commit -m "feat: improve terminal adapter loop and completion"
```

---

## Slice 11: Discord Adapter

**Goal:** Add a non-Minecraft adapter to prove the framework is truly platform-independent.

**Files:**
- Create module: `modules/discord-adapter`
- Modify: `settings.gradle.kts`
- Create Discord adapter contracts without hard-binding core to JDA.
- Test: `modules/discord-adapter/src/test/java/dev/riege/buildmycommand/discord/*`

- [ ] **Step 1: Add module and contract tests**

Test:

- text command dispatch
- slash command schema sync model
- ephemeral/public rendering
- Discord permission mapping

- [ ] **Step 2: Implement text command adapter**

Convert Discord message events into `CommandInput`.

- [ ] **Step 3: Implement slash sync model**

Export framework graph into Discord slash command definitions.

- [ ] **Step 4: Run tests and commit**

```powershell
.\gradlew.bat :discord-adapter:test
git add settings.gradle.kts modules/discord-adapter
git commit -m "feat: add discord adapter"
```

---

## Slice 12: Testkit Expansion

**Goal:** Make framework users able to test commands cleanly.

**Files:**
- Modify: `modules/testkit/src/main/java/dev/riege/buildmycommand/testkit/*`
- Test: `modules/testkit/src/test/java/dev/riege/buildmycommand/testkit/*`

- [ ] **Step 1: Add fluent assertion tests**

Add APIs:

- `assertDispatch("ban Ada").succeedsWith("...")`
- `assertDispatch("ban").failsWith("...")`
- `assertSuggestions("b").contains("ban")`
- `assertSchema().containsCommand("ban")`

- [ ] **Step 2: Add fake source/platform helpers**

Helpers:

- source with permissions
- source without permissions
- source with locale
- fake platform descriptor

- [ ] **Step 3: Run tests and commit**

```powershell
.\gradlew.bat :testkit:test :core:test
git add modules/testkit
git commit -m "feat: expand command testkit"
```

---

## Slice 13: Documentation Pass

**Goal:** Make the project understandable without reading tests.

**Files:**
- Create/update docs under `docs/`
- Modify: `README.md`

- [ ] **Step 1: Write user docs**

Create:

- `docs/getting-started.md`
- `docs/core-concepts.md`
- `docs/route-dsl.md`
- `docs/annotations.md`
- `docs/builder-api.md`
- `docs/manual-api.md`
- `docs/suggestions.md`
- `docs/errors-and-middleware.md`
- `docs/adapter-sdk.md`
- `docs/testing.md`

- [ ] **Step 2: Write adapter docs**

Create:

- `docs/adapters/terminal.md`
- `docs/adapters/minecraft-spigot-paper.md`
- `docs/adapters/minecraft-proxy.md`
- `docs/adapters/minecraft-modloaders.md`
- `docs/adapters/discord.md`

- [ ] **Step 3: Verify snippets compile**

Add snippet tests or examples where possible.

Run:

```powershell
.\gradlew.bat test
```

- [ ] **Step 4: Commit**

```powershell
git add README.md docs modules/examples
git commit -m "docs: add framework usage guides"
```

---

## Slice 14: Quality Gates And CI

**Goal:** Make the repo maintainable at “framework” quality instead of prototype quality.

**Files:**
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `.github/workflows/ci.yml`
- Create: `config/checkstyle/*` or formatter config
- Optional create: `gradle/libs.versions.toml` if not already present

- [ ] **Step 1: Add formatter/checkstyle**

Choose one consistent Java style gate and apply it to all non-generated Java modules.

- [ ] **Step 2: Add static analysis**

Add at least one:

- Error Prone
- SpotBugs
- NullAway or Checker Framework

- [ ] **Step 3: Raise JaCoCo thresholds**

Start conservative:

- core: 80%
- annotations: 75%
- adapters common: 75%
- platform modules: 60% until native integration lands

- [ ] **Step 4: Add GitHub Actions**

Matrix:

- Windows, Linux
- Java 21
- optional Java 17 only if source compatibility is intentionally supported

- [ ] **Step 5: Run and commit**

```powershell
.\gradlew.bat clean check
git add build.gradle.kts settings.gradle.kts .github config
git commit -m "ci: add quality gates"
```

---

## Slice 15: Publishing And Versioning

**Goal:** Make the project releasable as real modules.

**Files:**
- Modify: `build.gradle.kts`
- Create/modify: `gradle/publishing.gradle.kts`
- Create: `docs/releasing.md`
- Create: `docs/api-compatibility.md`

- [ ] **Step 1: Add publication metadata**

Each publishable module needs:

- group
- artifact id
- version
- license
- sources jar
- javadoc jar
- pom metadata

- [ ] **Step 2: Add binary compatibility checks**

Use a binary compatibility tool or document that `0.x` is unstable until API freeze.

- [ ] **Step 3: Add release workflow**

Support:

- local publish to Maven local
- GitHub Packages or Maven Central staging later

- [ ] **Step 4: Run and commit**

```powershell
.\gradlew.bat publishToMavenLocal
git add build.gradle.kts gradle docs
git commit -m "build: add publishing configuration"
```

---

## Recommended Execution Order

1. Slice 1: Universal Core Primitives
2. Slice 2: Public Parser And Rich Suggestion API
3. Slice 3: DSL Module Split And Route Analysis
4. Slice 4: Annotation Compiler Refactor
5. Slice 5: Middleware, Errors, Cooldowns, Lifecycle
6. Slice 6: Generic Adapter SDK
7. Slice 7: Native Minecraft Implementations
8. Slice 8: IntelliJ Plugin Completion, Validation, And Theme Polish
9. Slice 9: Schema, Debug, And Studio Foundations
10. Slice 10: Terminal Adapter Completion
11. Slice 11: Discord Adapter
12. Slice 12: Testkit Expansion
13. Slice 13: Documentation Pass
14. Slice 14: Quality Gates And CI
15. Slice 15: Publishing And Versioning

## Commit Policy

Commit after every slice and after every native platform integration inside Slice 7.

Do not push until a remote exists:

```powershell
git remote -v
```

If no remote appears, configure it before pushing:

```powershell
git remote add origin <repo-url>
git push -u origin feature/java-command-framework
```

## Verification Before Completion

Before claiming the full roadmap is complete:

```powershell
.\gradlew.bat clean check
.\gradlew.bat :intellij-plugin:buildPlugin
git status -sb
```

Expected:

- Gradle build succeeds.
- Plugin build succeeds.
- Worktree is clean.
- Every implemented slice has its own commit.
