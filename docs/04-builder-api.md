# 04 - Builder API

The builder API is the programmatic command declaration style. It is powerful and type-friendly, but for hand-written application commands the route annotations are usually clearer.

## When To Use Builders

| Use builders when | Prefer routes when |
| --- | --- |
| Commands are generated from config/data. | Commands are written by humans. |
| You need dynamic child creation. | You want the shortest readable declaration. |
| Adapter internals must build/merge nodes. | You want IntelliJ DSL highlighting. |
| Tests need synthetic command graphs. | You want metadata next to a method. |

## Simple Command

```java
framework.registry().command("ping", command -> command
    .description("Health check")
    .executes(ctx -> Results.success("Pong")));
```

## Arguments, Options, Flags

```java
framework.registry().command("give", command -> command
    .argument("target", String.class)
    .argument("item", String.class)
    .option("amount", Integer.class)
    .flag("silent", "s")
    .executes(ctx -> Results.success("ok")));
```

| Builder call | DSL equivalent |
| --- | --- |
| `.argument("target", String.class)` | `<target:String>` |
| `.greedyArgument("reason", String.class)` | `<reason:String...>` |
| `.option("amount", Integer.class)` | `[--amount:Integer]` |
| `.flag("silent", "s")` | `[--silent|-s]` |
| `.alias("p")` / `.aliases(...)` | `root|p` |

## Deep Nesting

Builders support arbitrary depth:

```java
framework.registry().command("player", player -> player
    .subcommand("moderation", moderation -> moderation
        .subcommand("punish", punish -> punish
            .subcommand("temporary", temporary -> temporary
                .subcommand("add", add -> add
                    .argument("target", String.class)
                    .greedyArgument("reason", String.class)
                    .option("duration", Integer.class)
                    .flag("silent", "s")
                    .executes(ctx -> Results.success("ok")))))));
```

This works, but `@SubRoute("moderation punish temporary add ...")` is easier to read for static command trees.

## Route Builder

The registry also exposes the route DSL without annotations:

```java
framework.registry()
    .route("give <target:String> <item:String> [--amount:Integer|-a] [--silent|-s]")
    .description("Give an item")
    .permission("inventory.give")
    .executes(ctx -> Results.success("ok"));
```

| API | Purpose |
| --- | --- |
| `.route(String)` | Parse and register a DSL route. |
| `.description(String)` | Help/schema description. |
| `.permission(String)` | Simple permission node. |
| `.requirement(String)` | Requirement expression metadata. |
| `.usage(String)` | Explicit usage. |
| `.example(String)` | Example line. |
| `.cooldown(Duration)` | Cooldown metadata. |
| `.middleware(CommandMiddleware)` | Leaf middleware. |

## Manual Command Nodes

`Commands.literal(...)` builds detached `CommandNode` graphs. This is useful for tests and external registries:

```java
CommandNode node = Commands.literal("manual")
    .metadata(new CommandMetadata.Builder()
        .middleware(new AuditMiddleware())
        .build())
    .handler(ctx -> Results.success("ok"))
    .build();

framework.registry().register(node);
```

Manual nodes preserve metadata, aliases, arguments, options, children, and middleware through registry import/export.

## Metadata Merge Behavior

| Merge case | Result |
| --- | --- |
| Existing root + incoming child | Child is merged into existing root. |
| Existing empty metadata + incoming metadata | Incoming metadata wins. |
| Same metadata on both sides | Accepted. |
| Conflicting metadata | Registration fails. |
| Both existing and incoming executable at same path | Registration fails. |

## Lifecycle Hooks

`CommandLifecycleListener` can observe command registration, update, unregistration, and registry rebuild snapshots. Adapters can use this to keep platform registrations synchronized.
