# 08 - Errors, Middleware, Permissions

This page explains execution policy: permissions, requirements, middleware, cooldowns, and exception handling.

## Execution Order

| Order | Step |
| --- | --- |
| 1 | Tokenize input. |
| 2 | Match command path. |
| 3 | Check permissions on matched nodes. |
| 4 | Parse options and flags. |
| 5 | Parse arguments. |
| 6 | Build `CommandContext`. |
| 7 | Execute global middleware. |
| 8 | Execute metadata/path middleware in matched path order. |
| 9 | Execute command handler. |
| 10 | Map exceptions through the configured error handler. |

## Permissions

`@Permission` is for simple platform permission nodes:

```java
@Permission("admin.reload")
```

It maps to `CommandSource.hasPermission("admin.reload")`.

| Permission location | Applies to |
| --- | --- |
| Class `@Command` | Root and all matching children. |
| Class `@Subcommand` | That subtree. |
| Method command/route | Executable leaf. |
| Builder `.permission(...)` | That node/route. |

## Requirements

`@Require` stores requirement expression metadata:

```java
@Require("staff || owner")
```

Use it when access logic is more expressive than one permission node.

Requirement expressions are evaluated by the runtime before parsing command arguments.

| Syntax | Meaning |
| --- | --- |
| `staff` | Source must have `staff`. |
| `staff && mod.punish` | Source must have both permissions. |
| `staff || owner` | Source needs either permission. |
| `!banned` | Source must not have `banned`. |
| `staff && (!banned || owner)` | Parentheses control grouping. |

Malformed requirement expressions fail closed: the command is treated as denied rather than allowed.

| Use | Annotation |
| --- | --- |
| One permission | `@Permission("mod.punish")` |
| Boolean expression | `@Require("staff || owner")` |
| Runtime custom guard | `@Middleware(MyGuard.class)` |

## Middleware

Middleware can wrap execution, block commands, add auditing, perform cooldowns, or map contextual behavior.

```java
public final class StaffOnlyMiddleware implements CommandMiddleware {
    @Override
    public CommandResult execute(CommandContext context, CommandNode command, List<String> path, Chain next) {
        if (!context.source().hasPermission("staff")) {
            return Results.failure("Missing permission: staff");
        }
        return next.proceed(context);
    }
}
```

Attach globally:

```java
CommandFramework framework = CommandFramework.builder()
    .middleware(new StaffOnlyMiddleware())
    .build();
```

Attach by annotation:

```java
@Middleware(StaffOnlyMiddleware.class)
@SubRoute("warn <target:String> <reason:String...>")
CommandResult warn(@RouteCtx CommandContext ctx) { ... }
```

Attach by builder:

```java
framework.registry()
    .route("admin reload")
    .middleware(new AuditMiddleware())
    .executes(ctx -> Results.success("Reloaded"));
```

## Middleware Rules

| Rule | Reason |
| --- | --- |
| Return a non-null `CommandResult` | Dispatcher requires explicit outcome. |
| Call `next.proceed(...)` at most once | Prevents duplicate command execution. |
| Preserve failure status when decorating replies | Audit/logging should not turn failures into success. |
| Prefer annotation middleware for command-local policy | Keeps policy near the command. |
| Prefer global middleware for cross-cutting policy | Metrics/logging/correlation ids belong globally. |

## Exception Handling

| API | Use |
| --- | --- |
| `CommandExceptionHandler` | Full exception context handler. |
| `CommandErrorHandler` | Legacy/simple execution error mapper. |
| `CommandExceptionHandlers.failureMessage()` | Default failure message behavior. |

Example:

```java
CommandFramework framework = CommandFramework.builder()
    .exceptionHandler((context, error) -> Results.failure("Command failed: " + error.getMessage()))
    .build();
```

## Built-in Exception Types

| Exception | Meaning |
| --- | --- |
| `UnknownCommandException` | Root command was not found. |
| `CommandSyntaxException` | Command shape or tokenization failed. |
| `ArgumentParseException` | Positional argument parse failed. |
| `OptionParseException` | Option/flag parse failed. |
| `PermissionDeniedException` | Permission check failed. |

## Cooldowns

Cooldown metadata can be attached through annotations or builders.

| Declaration | Use |
| --- | --- |
| `@Cooldown(...)` | Annotation commands. |
| `.cooldown(Duration)` | Builder/route commands. |
| `CooldownMiddleware` | Runtime implementation. |

Cooldown failures should be treated as command failures, not parser errors.
