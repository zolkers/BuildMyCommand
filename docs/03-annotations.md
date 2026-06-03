# 03 - Annotations

The annotations module turns Java classes into command registrations. The recommended declaration style is `@Command` on a class plus `@SubRoute` on methods, or plain `@Route` on standalone methods.

## Style Recommendation

| Rank | Style | Use it for |
| --- | --- | --- |
| 1 | `@Command` + `@SubRoute` + `@RouteCtx` | Most production commands, especially nested commands. |
| 2 | `@Route` + `@RouteCtx` | Standalone commands or examples. |
| 3 | `@Command` / `@Subcommand` + parameter annotations | Simple Java-native commands. |
| 4 | Builder API | Dynamic/generated commands or adapter internals. |

## Registration

```java
CommandFramework framework = CommandFramework.create();
AnnotationCommandScanner.register(framework.registry(), new ModerationCommands());
```

## Command Shape Annotations

| Annotation | Target | Description | Recommended |
| --- | --- | --- | --- |
| `@Command("root")` | Class, method | Root command literal. On class, groups subcommands. | Yes for class roots. |
| `@Route("full path")` | Method | Full route DSL from root. | Yes for standalone commands. |
| `@SubRoute("path")` | Method | Route DSL relative to class `@Command`. | Yes, most common. |
| `@Subcommand("literal")` | Class, method | Single literal segment. Can nest through classes. | Supported, but less ergonomic. |
| `@Alias("x")` | Class, method | Alias for command/subcommand literal. | Useful for public command aliases. |

## Parameter Binding Annotations

| Annotation | Target | Meaning | RouteCtx alternative |
| --- | --- | --- | --- |
| `@RouteCtx` | Parameter | Injects `CommandContext`. | Preferred. |
| `@Arg("name")` | Parameter | Required positional arg. | `ctx.arg("name", Type.class)` |
| `@OptionalArg("name")` | Parameter | Optional positional arg. | `ctx.optionalArg("name", Type.class)` |
| `@Option("name")` | Parameter | Named option value. | `ctx.option("name", Type.class)` |
| `@Flag("name")` | Parameter | Boolean flag. | `ctx.flag("name")` |
| `@Default("value")` | Parameter | Default for optional binding. | `orElse(...)` in Java. |
| `@Greedy` | Parameter | Greedy string binding. | `<name:String...>` in route DSL. |
| `@Suggest(...)` | Parameter | Suggestion provider binding. | Same command graph metadata. |

When using `@Route` or `@SubRoute`, prefer `@RouteCtx`. Mixing route DSL and repeated parameter annotations is supported for compatibility, but the route already contains the command schema.

## Metadata Annotations

| Annotation | Target | Runtime effect |
| --- | --- | --- |
| `@Description` | Class, method | Help/schema description. |
| `@Permission` | Class, method | Simple platform permission node. |
| `@Require` | Class, method | Requirement expression string. |
| `@Usage` | Class, method | Explicit usage string. |
| `@Example` | Class, method | One or more examples. |
| `@Hidden` | Class, method | Hide from help/schema. |
| `@Cooldown` | Class, method | Cooldown metadata/runtime middleware. |
| `@CommandGroup` | Class, method | Logical help/schema group. |
| `@CaseInsensitive` | Class, method | Literal/option matching policy. |
| `@Middleware` | Class, method | Adds command middleware to the matched path. |

## Permission vs Require

| Annotation | Intended use | Example |
| --- | --- | --- |
| `@Permission` | One simple permission node. | `@Permission("admin.reload")` |
| `@Require` | Boolean/logical requirement expression. | `@Require("staff || owner")` |

Future IntelliJ inspections should warn when `@Permission` contains boolean operators and suggest `@Require`.

## Middleware Annotation

```java
@Command("moderation")
@Middleware(AuditMiddleware.class)
static final class ModerationCommands {
    @SubRoute("warn <target:String> <reason:String...>")
    @Middleware(StaffOnlyMiddleware.class)
    CommandResult warn(@RouteCtx CommandContext ctx) {
        return Results.success("Warned " + ctx.arg("target", String.class));
    }
}
```

| Placement | Applies to |
| --- | --- |
| Class `@Command` | Root command path and all matching subcommands. |
| Nested class `@Subcommand` | That subtree. |
| Method `@Route` | The executable route. |
| Method `@SubRoute` / `@Subcommand` | The executable leaf. |

Middleware classes must implement `CommandMiddleware` and expose a no-arg constructor.

## Case Sensitivity

```java
@CaseInsensitive(literals = true, options = true)
@Command("moderation")
static final class ModerationCommands { ... }
```

| Property | Effect |
| --- | --- |
| `literals = true` | Command and subcommand literals match case-insensitively. |
| `options = true` | Option and flag labels match case-insensitively. |

Arguments remain parser-specific. This is intentional: player names, item ids, and custom parsers may be case-sensitive.

## Annotation Interaction Rules

| Situation | Rule |
| --- | --- |
| `@Route` method | Do not also use `@Command`, `@Subcommand`, or `@SubRoute` on the same method. |
| `@SubRoute` method | Must live under a class root command. |
| Class metadata and method metadata | Root/class metadata applies to subtree; method metadata applies to leaf. |
| Duplicate command shape | Registration fails with conflict. |
| Blank metadata | Registration fails early. |

## Recommended Template

```java
@Command("admin")
@Alias("a")
@CaseInsensitive(literals = true, options = true)
static final class AdminCommands {
    @SubRoute("moderation punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
    @Description("Punish a user")
    @Permission("mod.punish")
    @Usage("/admin moderation punish <target> <reason> [--duration <minutes>] [--silent]")
    @Example("/a moderation punish Ada spam -d 30 -s")
    CommandResult punish(@RouteCtx CommandContext ctx) {
        ...
    }
}
```
