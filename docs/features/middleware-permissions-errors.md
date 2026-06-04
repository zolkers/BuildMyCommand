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

It can also be a Java regex when one command should accept a family of
permission nodes:

```java
@Permission(value = "wecc\\.admin\\..*", regex = true)
@SubRoute("admin audit")
CommandResult audit(@RouteCtx CommandContext ctx) {
    return Results.success("audit");
}
```

The builder equivalent is:

```java
framework.registry()
    .route("admin audit")
    .permissionRegex("wecc\\.admin\\..*")
    .executes(ctx -> Results.success("audit"));
```

`@Require` is a boolean expression:

```java
@Require("staff || owner")
@SubRoute("admin reload")
CommandResult reload(@RouteCtx CommandContext ctx) {
    return Results.success("reloaded");
}
```

Expressions support `&&`, `||`, `!`, and parentheses. Keep boolean logic in
`@Require`; use regex permissions only for matching permission names.

## CommandSource Permission Policy

Exact permissions call `CommandSource.hasPermission(String)`.

```java
@Override
public boolean hasPermission(String permission) {
    return permission == null || permission.isBlank() || elevated;
}
```

Regex permissions call `CommandSource.hasPermissionMatching(Pattern)`. The
default implementation checks the values returned by `permissions()`:

```java
@Override
public Set<String> permissions() {
    return Set.copyOf(permissionNodes);
}
```

If your platform has a native way to check patterns, override the matcher:

```java
@Override
public boolean hasPermissionMatching(Pattern pattern) {
    return nativePermissions().stream().anyMatch(permission -> pattern.matcher(permission).matches());
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
