<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Builder API

The builder API exists for dynamic or generated command trees. For ordinary command classes, prefer annotations with `@Command` and `@SubRoute`.

## When To Use It

| Use builder when | Prefer annotations when |
| --- | --- |
| Commands are generated from config. | Commands are static Java methods. |
| A plugin/module contributes routes at runtime. | You want IntelliJ route highlighting on annotations. |
| You need loops/conditionals during registration. | You want compact command classes. |

## Route Builder

The route builder accepts the same DSL:

```java
CommandFramework framework = CommandFramework.create();

framework.registry()
    .route("wecc bang|b <target:String>")
    .description("Bang a player")
    .suggestAliases(false)
    .argumentSuggestions("target", "target", ctx -> List.of("Ada", "Alex"))
    .executes(ctx -> Results.success("bang " + ctx.arg("target", String.class)));
```

## Command Tree Builder

Use `.path(...)` for literal-only paths and `.subRoute(...)` for DSL routes.

```java
framework.registry().command("admin", admin -> admin
    .path("rank set promote", promote -> promote.executes(ctx -> Results.success("promoted")))
    .subRoute("player give <target:String> <item:String>", give -> give.executes(ctx ->
        Results.success("gave " + ctx.arg("item", String.class))
    )));
```

`path("rank set promote")` is literal-only. It should not contain aliases, arguments, options, or optional syntax. Use `subRoute(...)` for that.

## Middleware

```java
framework.registry()
    .route("moderation punish <target:String>")
    .middleware((context, command, path, next) -> {
        if (!context.source().hasPermission("staff")) {
            return Results.failure("Missing permission: staff");
        }
        return next.proceed(context);
    })
    .executes(ctx -> Results.success("punished " + ctx.arg("target", String.class)));
```

## Custom Route Types

Use `.type(...)` for one alias or `.types(...)` for a small registry block. The alias is what users write in the route DSL; the Java class is what commands read from `CommandContext`.

```java
CommandFramework framework = CommandFramework.builder()
    .types(types -> types
        .register("Material", Material.class, new MaterialParser())
        .register("ItemStack", ItemStack.class, new ItemStackParser()))
    .build();

framework.registry()
    .route("shop give <item:Material> [--fallback:Material|-f]")
    .executes(ctx -> {
        Material item = ctx.arg("item", Material.class);
        Material fallback = ctx.option("fallback", Material.class).orElse(item);
        return Results.success("giving " + item + " fallback=" + fallback);
    });
```

Aliases must be unique and cannot replace built-in names such as `String`, `Integer`, `Boolean`, `double`, or `UUID`. Java types are also unique, so a framework cannot accidentally register the same parser target under two DSL names.

## Recommendation

Do not duplicate every annotation example in builder style. Keep the builder for dynamic cases. For a normal mod or app command registry, annotation route classes are easier to read, easier to inspect, and easier to test.
