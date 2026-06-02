# Errors And Middleware

Middleware wraps command execution without bloating the dispatcher:

```java
CommandFramework framework = CommandFramework.builder()
    .middleware((ctx, command, path, next) -> {
        if (!ctx.source().hasPermission("commands.use")) {
            return Results.failure("Missing permission");
        }
        return next.execute(ctx);
    })
    .build();
```

Built-in cooldown support reads command metadata emitted by annotations or builders:

```java
framework.registry()
    .route("daily")
    .cooldown(Duration.ofHours(24))
    .executes(ctx -> Results.success("ok"));
```

Use a custom error handler to map thrown exceptions to user-facing failures:

```java
CommandFramework.builder()
    .errorHandler((ctx, command, path, error) -> Results.failure(error.getMessage()))
    .build();
```
