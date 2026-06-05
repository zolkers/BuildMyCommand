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

## Native Types

BuildMyCommand ships these route DSL type names out of the box:

| DSL type | Java type | Example |
| --- | --- | --- |
| `String` | `java.lang.String` | `<name:String>` |
| `Integer` | `java.lang.Integer` | `<amount:Integer>` |
| `int` | `int` | `<amount:int>` |
| `Long` | `java.lang.Long` | `<ticks:Long>` |
| `long` | `long` | `<ticks:long>` |
| `Float` | `java.lang.Float` | `<speed:Float>` |
| `float` | `float` | `<speed:float>` |
| `Double` | `java.lang.Double` | `<ratio:Double>` |
| `double` | `double` | `<ratio:double>` |
| `Boolean` | `java.lang.Boolean` | `[--enabled:Boolean]` |
| `boolean` | `boolean` | `[--enabled:boolean]` |
| `UUID` | `java.util.UUID` | `<id:UUID>` |
| `Duration` | `java.time.Duration` | `<ttl:Duration>` with ISO-8601 values such as `PT5M` |
| `LocalDate` | `java.time.LocalDate` | `<day:LocalDate>` with values such as `2026-06-05` |
| `LocalDateTime` | `java.time.LocalDateTime` | `<at:LocalDateTime>` with values such as `2026-06-05T18:30:00` |
| `Path` | `java.nio.file.Path` | `<file:Path>` |
| `URI` | `java.net.URI` | `<uri:URI>` |
| `URL` | `java.net.URL` | `<url:URL>` |

Numeric types can also use ranges:

```java
@SubRoute("volume <percent:Integer{0..100}>")
CommandResult volume(@RouteCtx CommandContext ctx) {
    int percent = ctx.arg("percent", Integer.class);
    return Results.success("volume=" + percent);
}
```

Inline enum analysis is available for tooling and conflict checks:

```java
@SubRoute("mode <value:enum(survival,creative,adventure)>")
```

For runtime enum parsing, prefer registering a named type alias for your enum class. That keeps the DSL, runtime parser, and IntelliJ inspection model aligned.

## Custom Types

Custom route types are the clean path for platform objects such as `Material`, `ItemStack`, `Player`, `World`, or your own domain types. Register the alias once when creating the framework:

```java
CommandFramework framework = CommandFramework.builder()
    .types(types -> types
        .register("Material", Material.class, new MaterialParser())
        .register("ItemStack", ItemStack.class, new ItemStackParser()))
    .build();
```

Then use the alias directly in routes:

```java
@Command("shop")
final class ShopCommands {
    @SubRoute("give <item:Material> [--fallback:Material|-f]")
    CommandResult give(@RouteCtx CommandContext ctx) {
        Material item = ctx.arg("item", Material.class);
        Material fallback = ctx.option("fallback", Material.class).orElse(item);
        return Results.success("giving " + item + " fallback=" + fallback);
    }
}
```

A parser converts raw input into the Java type and can optionally provide suggestions:

```java
final class MaterialParser implements ArgumentParser<Material> {
    @Override
    public ArgumentParseResult<Material> parse(String rawToken, ArgumentParseContext context) {
        Material material = Material.matchMaterial(rawToken);
        if (material == null) {
            return ArgumentParseResult.failure("Unknown material: " + rawToken);
        }
        return ArgumentParseResult.success(material);
    }

    @Override
    public List<Suggestion> suggestions(ArgumentParseContext context) {
        String prefix = context.rawToken().toLowerCase(Locale.ROOT);
        return Arrays.stream(Material.values())
            .map(Material::name)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .map(Suggestion::of)
            .toList();
    }
}
```

Aliases must be unique. Built-in names such as `String`, `Integer`, `Boolean`, `UUID`, and `Duration` cannot be replaced, and a Java type can only be registered once in a framework instance.

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
