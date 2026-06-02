# Builder API

The builder API is explicit and concise for runtime registration:

```java
framework.registry().command("give", command -> command
    .description("Give an item")
    .alias("grant")
    .permission("inventory.give")
    .argument("target", String.class)
    .argument("item", String.class)
    .option("amount", Integer.class, "a")
    .flag("silent", "s")
    .executes(ctx -> Results.success(ctx.arg("target", String.class))));
```

Use it when command structure is assembled in Java code, or when an adapter needs to register commands dynamically.

The route builder is a DSL-oriented sibling:

```java
framework.registry()
    .route("give|grant <target:String> <item:String> [--amount:Integer|-a]")
    .executes(ctx -> Results.success("ok"));
```
