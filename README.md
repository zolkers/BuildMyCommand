# BuildMyCommand

BuildMyCommand is a universal, platform-neutral Java command framework.

The project follows a simple rule: every public declaration style eventually compiles to the same internal command model. The first implementation slice focuses on the core runtime:

- `api` for public contracts
- `core` for registration, routing, dispatch, parsing, and suggestions
- `adapters` for platform integrations, including the Minecraft adapter family
- `intellij-plugin` for IDE support around annotations and route DSL strings
- `testkit` for platform-free command tests

The long-form architecture plan is tracked in `java_command_framework_master_plan (1).md`.

## Annotation DSL Example

```java
@Route("moderation punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
@Description("Punish a user")
@Permission("mod.punish")
CommandResult punish(
    @Arg("target") String target,
    @Arg("reason") String reason,
    @Option("duration") Integer duration,
    @Flag("silent") boolean silent
) {
    int minutes = duration == null ? 60 : duration;
    return Results.success(target + " punished for " + minutes + "m: " + reason);
}
```

Annotation commands can also use the non-DSL style when that is clearer:

```java
@Command("msg")
@Alias("message")
CommandResult msg(
    @Arg("target") String target,
    @Arg("message") @OptionalArg @Greedy @Default("No message") String message
) {
    return Results.success(target + ": " + message);
}
```

## Matching Policy

Matching is strict by default. Platforms that need friendlier casing can opt in explicitly:

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();
```

Literal matching covers command names, subcommands, and aliases. Option matching covers long and short options. Argument values are never case-normalized.

## Build

```powershell
.\gradlew.bat test
```

## Current Scope

The repository is being built incrementally from a minimal command dispatcher toward the full roadmap:

1. literal command dispatch: implemented
2. typed arguments: implemented
3. subcommands and aliases: implemented
4. flags and options: in progress
5. suggestions and help: suggestions in progress, help later
6. DSL, annotations, adapters, and studio modules: in progress

## Architecture Notes

- Core runtime internals are grouped by responsibility under packages such as `registry`, `parse`, `dispatch`, `route`, and `help`.
- Minecraft support lives under `modules/adapters/minecraft`, with a `common` bridge, a Brigadier projection layer, and backend modules for Paper, Spigot, BungeeCord, Velocity, Fabric, Forge, and NeoForge.
- IntelliJ support includes a lightweight BuildMyCommand route language, IntelliLang injection for `@Command`, `@Route`, and `CommandRegistry#route(String)`, a syntax highlighter, a TextMate grammar, and light/dark color schemes. Rich validation and completion should be added inside `modules/intellij-plugin`, not in runtime modules.
