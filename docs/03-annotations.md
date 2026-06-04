<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Annotations

Annotations are intentionally split into command shape, metadata, execution policy, and tooling hints.

## Shape

| Annotation | Target | Rule |
| --- | --- | --- |
| `@Command("root")` | type, method | One literal only. On a class, owns a root for `@SubRoute`. |
| `@Route("root leaf <arg:String>")` | method | Full route DSL. |
| `@SubRoute("leaf <arg:String>")` | method | Route DSL under a class `@Command`. |
| `@Subcommand("literal")` | type, method | Legacy/nested literal node. Supported but less recommended than `@SubRoute`. |
| `@RouteCtx` | parameter | Must annotate a `CommandContext` parameter on `@Route`/`@SubRoute` methods. |

Command methods must return `CommandResult` and must be public or package-private. Private/protected command methods are rejected by runtime and flagged by the IntelliJ plugin.

## Metadata

| Annotation | Target | Meaning |
| --- | --- | --- |
| `@Description("...")` | type, method | Help/schema description. |
| `@Usage("...")` | type, method | Custom usage line. |
| `@Example("...")` | type, method | Help examples. |
| `@Hidden` | type, method | Hide from public help/suggestions where supported. |
| `@CommandGroup("...")` | type | Logical grouping metadata. |

Blank metadata is invalid.

## Permissions And Requirements

| Annotation | Use |
| --- | --- |
| `@Permission("mod.punish")` | One simple permission node. |
| `@Require("staff || owner")` | Boolean permission expression. |

Do not put boolean expressions in `@Permission`; use `@Require`.

```java
@SubRoute("reload")
@Permission("admin.reload")
CommandResult reload(@RouteCtx CommandContext ctx) {
    return Results.success("reloaded");
}

@SubRoute("debug")
@Require("staff && !banned")
CommandResult debug(@RouteCtx CommandContext ctx) {
    return Results.success("debug");
}
```

## Suggestions

| Annotation | Rule |
| --- | --- |
| `@Suggest("target")` | Method provides suggestions for a route argument/option named `target`. |
| `@SuggestAliases(false)` | Do not suggest aliases for this route/class, while still accepting them. |

Valid provider signatures:

```java
@Suggest("target")
List<String> targets()

@Suggest("target")
List<Suggestion> targets(ArgumentParseContext ctx)

@Suggest("target")
SuggestionSet targets(SuggestionContext ctx)
```

The provider name must match at least one `@Route` or `@SubRoute` argument/option in the same command class. It is applied only to matching leaves.

## Middleware And Cooldown

| Annotation | Rule |
| --- | --- |
| `@Middleware(MyMiddleware.class)` | Class must implement `CommandMiddleware` and have a no-arg constructor. |
| `@Cooldown(5)` | Cooldown must be positive. |

```java
@Middleware(ReplyResultMiddleware.class)
@SubRoute("ping")
CommandResult ping(@RouteCtx CommandContext ctx) {
    return Results.success("pong");
}
```

## IntelliJ Rules

The plugin highlights route/requirement strings and reports concrete mistakes:

| Mistake | Reported |
| --- | --- |
| `@Route` method without `@RouteCtx CommandContext` | Yes |
| `@RouteCtx String ctx` | Yes |
| `@Command("root <arg:String>")` | Yes, use `@Route`. |
| `@Subcommand("leaf <arg:String>")` | Yes, use `@SubRoute`. |
| `@Permission("staff || owner")` | Yes, use `@Require`. |
| `@Suggest("missing")` with no route binding | Yes |
| `@Middleware(Bad.class)` with no no-arg constructor | Yes |
