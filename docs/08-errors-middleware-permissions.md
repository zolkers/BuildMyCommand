<!--
Copyright (c) 2026 Zolkers

Licensed under the MIT License.
SPDX-License-Identifier: MIT
-->

# Middleware, Permissions, And Errors

BuildMyCommand keeps command logic small by pushing cross-cutting behavior into middleware and source policies.

## Permissions

`@Permission` is a single permission node:

```java
@Permission("wecc.ping")
@SubRoute("ping")
CommandResult ping(@RouteCtx CommandContext ctx) {
    return Results.success("pong");
}
```

`@Require` is a boolean expression:

```java
@Require("staff || owner")
@SubRoute("admin reload")
CommandResult reload(@RouteCtx CommandContext ctx) {
    return Results.success("reloaded");
}
```

Expressions support `&&`, `||`, `!`, and parentheses.

## CommandSource Permission Policy

Permissions call `CommandSource.hasPermission(String)`.

```java
@Override
public boolean hasPermission(String permission) {
    return permission == null || permission.isBlank() || elevated;
}
```

For client-only Fabric mods, a simple elevated source is often enough. For server/proxy plugins, forward to the platform permission API.

## Middleware

Middleware can wrap execution:

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

Attach it:

```java
@Middleware(StaffOnlyMiddleware.class)
@SubRoute("punish <target:String>")
CommandResult punish(@RouteCtx CommandContext ctx) {
    return Results.success("punished " + ctx.arg("target", String.class));
}
```

Middleware classes used by `@Middleware` must have a no-arg constructor.

## Replies

Commands should return framework results:

```java
return Results.success("done");
return Results.failure("nope");
return Results.silent();
```

Adapters or source wrappers render `CommandMessage` to native output. In Fabric, a source wrapper can send errors through `sendError` and success/info through `sendFeedback`.

## Exceptions

Runtime errors should be converted at the adapter boundary into user-friendly failures. Do not let platform-specific stack traces leak to players/users. Keep command methods focused on domain checks and return `Results.failure(...)` for expected user errors.
