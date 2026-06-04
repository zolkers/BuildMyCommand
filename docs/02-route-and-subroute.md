<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Route And SubRoute

`@Route` and `@SubRoute` are the recommended declaration API.

Use:

| Annotation | When |
| --- | --- |
| `@Route("root leaf <arg:String>")` | The method declares the full root path itself. |
| `@Command("root")` + `@SubRoute("leaf <arg:String>")` | The class owns one root and methods declare leaves. Recommended for real projects. |

## Route Syntax

| Syntax | Meaning |
| --- | --- |
| `literal` | Required literal token. |
| `literal|alias` | Literal with alias. |
| `<name:String>` | Required argument. |
| `<name:String...>` | Required greedy string argument. |
| `[name:String]` | Optional positional argument. |
| `[--flag]` | Boolean flag. |
| `[--option:Integer]` | Optional named value. |
| `[--silent|-s]` | Long option/flag with short alias. |

Example:

```java
@SubRoute("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
CommandResult punish(@RouteCtx CommandContext ctx) {
    String target = ctx.arg("target", String.class);
    String reason = ctx.arg("reason", String.class);
    int duration = ctx.option("duration", Integer.class).orElse(60);
    boolean silent = ctx.flag("silent");
    return Results.success("Punished " + target + " for " + duration + "m: " + reason);
}
```

## Route Context

Route DSL methods should receive exactly one route context:

```java
CommandResult run(@RouteCtx CommandContext ctx)
```

Read values from the context:

| Call | Reads |
| --- | --- |
| `ctx.arg("target", String.class)` | Required argument. |
| `ctx.optionalArg("target", String.class)` | Optional positional argument. |
| `ctx.option("duration", Integer.class)` | Named option value. |
| `ctx.flag("silent")` | Boolean flag. |
| `ctx.source()` | Current `CommandSource`. |
| `ctx.unwrapSource(FabricClientCommandSource.class)` | Native source if available. |

## Alias Suggestions

Aliases execute by default. Suggestions can be controlled:

```java
@SuggestAliases(false)
@SubRoute("bang|b <target:String>")
CommandResult bang(@RouteCtx CommandContext ctx) {
    return Results.success("bang " + ctx.arg("target", String.class));
}
```

With `@SuggestAliases(false)`, `bang` can still be typed as `b`, but the alias does not have to appear in completion lists.

## What Not To Do

This works, but it is not the preferred style for large trees:

```java
@Command("team")
static final class TeamCommands {
    @Subcommand("member")
    static final class MemberCommands {
        @Subcommand("permission")
        static final class PermissionCommands {
            @Subcommand("grant")
            CommandResult grant(CommandContext ctx) {
                return Results.success("ok");
            }
        }
    }
}
```

Prefer:

```java
@Command("team")
static final class TeamCommands {
    @SubRoute("member permission grant <target:String> <permission:String>")
    CommandResult grant(@RouteCtx CommandContext ctx) {
        return Results.success("ok");
    }
}
```
