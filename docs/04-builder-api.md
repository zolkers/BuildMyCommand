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

## Recommendation

Do not duplicate every annotation example in builder style. Keep the builder for dynamic cases. For a normal mod or app command registry, annotation route classes are easier to read, easier to inspect, and easier to test.
