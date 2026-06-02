# Manual API

Manual nodes are useful for generated commands, schema transforms, and adapter-level tests.

```java
framework.registry().register(Commands.literal("ban")
    .description("Ban a user")
    .permission("mod.ban")
    .argument(Arguments.required("target", String.class))
    .argument(Arguments.greedyOptional("reason", String.class))
    .flag(Flags.bool("silent").alias("s"))
    .handler(ctx -> Results.success("Banned " + ctx.arg("target", String.class)))
    .build());
```

Manual registration bypasses route string parsing but still uses the same dispatcher, parser registry, permissions, suggestions, metadata, and middleware.
