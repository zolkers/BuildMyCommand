# 02 - Route And SubRoute

`@Route` and `@SubRoute` are the primary style for BuildMyCommand. They are strongly recommended for application code because they keep command shape, aliases, arguments, options, and flags in one inspectable DSL string.

## Why This Style Is Preferred

| Criterion | `@Route` / `@SubRoute` | Parameter annotations | Builder API |
| --- | --- | --- | --- |
| Readability | Entire command shape in one line | Shape spread across method and params | Shape spread across chained calls |
| Deep nesting | Excellent | Verbose with nested classes | Good but longer |
| IntelliJ support | Best: injection, color, inspections | Limited to Java annotations | Java completion only |
| Refactor safety | Route string still needs tests | Param names/annotations are explicit | Strong programmatic control |
| Best use | Most user-facing commands | Very simple commands | Dynamic/generated commands |

## Route vs SubRoute

| Annotation | Target | Meaning | Example |
| --- | --- | --- | --- |
| `@Route` | Method | Full command path from root. | `@Route("ban <target:String>")` |
| `@SubRoute` | Method inside `@Command` class | Path after the class root. | `@Command("admin")` + `@SubRoute("reload")` |

## Grammar Overview

| DSL part | Meaning | Example |
| --- | --- | --- |
| `literal` | Required command word. | `moderation` |
| `a|b|c` | Literal aliases at the same position. | `moderation|mod` |
| `<name:Type>` | Required argument. | `<target:String>` |
| `<name:Type...>` | Greedy argument consuming remaining input. | `<reason:String...>` |
| `[<name:Type>]` | Optional argument. | `[<world:String>]` |
| `[--name:Type]` | Named option with value. | `[--duration:Integer]` |
| `[--name:Type|-x]` | Named option with short alias. | `[--amount:Integer|-a]` |
| `[--flag]` | Boolean flag. | `[--silent]` |
| `[--flag|-s]` | Boolean flag with short alias. | `[--silent|-s]` |

## Argument Types

Built-in parsers are provided by core. Custom parsers can be registered through the framework parser registry.

| Type token | Java type | Notes |
| --- | --- | --- |
| `String` | `String` | Single token unless greedy. |
| `String...` | `String` | Greedy tail string. |
| `Integer` | `Integer` | Integer parser. |
| `Long` | `Long` | Long parser. |
| `Double` | `Double` | Double parser. |
| `Boolean` | `Boolean` | Boolean parser. |
| `UUID` | `UUID` | UUID parser. |
| `URL` | `URL` | URL parser. |
| `URI` | `URI` | URI parser. |
| `Path` | `Path` | Path-like token parser. |
| `LocalDate` | `LocalDate` | ISO date. |
| `LocalDateTime` | `LocalDateTime` | ISO date-time. |

## Good Route Design

Prefer nouns and stable subtrees:

```java
@Command("player")
static final class PlayerCommands {
    @SubRoute("profile view <target:String>")
    CommandResult view(@RouteCtx CommandContext ctx) { ... }

    @SubRoute("moderation punish temp add <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    CommandResult tempPunish(@RouteCtx CommandContext ctx) { ... }
}
```

Avoid encoding too much behavior into flags when a literal is clearer:

| Prefer | Avoid |
| --- | --- |
| `punish temp add <target> ...` | `punish <target> --temp --add ...` |
| `rank set <target> <rank>` | `rank <target> <rank> --set` |
| `profile view <target>` | `profile <target> --view` |

## RouteCtx Pattern

For route methods, prefer a single `@RouteCtx CommandContext` parameter:

```java
@SubRoute("give <target:String> <item:String> [--amount:Integer|-a]")
CommandResult give(@RouteCtx CommandContext ctx) {
    String target = ctx.arg("target", String.class);
    String item = ctx.arg("item", String.class);
    int amount = ctx.option("amount", Integer.class).orElse(1);
    return Results.success("Gave " + amount + " " + item + " to " + target);
}
```

This avoids duplicated annotations like `@Arg("target")`, `@Option("amount")`, and `@Flag("silent")`. Parameter annotations are still supported, but they are no longer the recommended route style.

## Validation Rules

| Rule | Reason |
| --- | --- |
| Required arguments cannot follow optional arguments | Avoid ambiguous parse order. |
| Greedy arguments must be last among positionals | Greedy consumes the remaining input. |
| Option and flag names must not be blank | Runtime metadata must be stable. |
| Short aliases are declared with `|-x` | Keeps aliases local to the option/flag. |
| Use `@SubRoute`, not deeply nested classes, for deep command paths | Reduces boilerplate and improves IDE assistance. |

## Examples

| Use case | Route |
| --- | --- |
| Simple command | `ping` |
| Alias root | `moderation|mod status` |
| Required args | `ban <target:String> <reason:String...>` |
| Optional arg | `teleport <target:String> [<world:String>]` |
| Option | `mute <target:String> [--minutes:Integer|-m] <reason:String...>` |
| Flag | `give <target:String> <item:String> [--silent|-s]` |
| Deep tree | `admin moderation punish temporary add <target:String> <reason:String...>` |

## IntelliJ Support

The IntelliJ plugin injects the route DSL into Java strings used by `@Route`, `@SubRoute`, and builder `.route(...)` calls. It provides syntax coloring, route inspections, and a dedicated TextMate grammar/theme.
