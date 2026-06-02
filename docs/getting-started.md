# Getting Started

BuildMyCommand can be used through the builder API, the route DSL, annotations, or manual command nodes. All paths register into the same `CommandFramework`.

```java
CommandFramework framework = CommandFramework.create();

framework.registry()
    .route("give <target:String> <item:String> [--amount:Integer|-a]")
    .description("Give an item")
    .permission("inventory.give")
    .executes(ctx -> Results.success(
        ctx.arg("target", String.class)
            + " gets "
            + ctx.option("amount", Integer.class).orElse(1)
            + " "
            + ctx.arg("item", String.class)));
```

Dispatch directly in tests or local tools:

```java
CommandResult result = framework.dispatch(new CommandSource() {}, "give Ada diamond --amount 3");
```

Attach an adapter when a platform owns input and output:

```java
dev.riege.buildmycommand.adapters.terminal.TerminalAdapter terminal =
    dev.riege.buildmycommand.adapters.terminal.TerminalAdapter.attach(framework);
terminal.runLoop();
```

Use `CommandFramework.builder()` when the platform needs case-insensitive literals or options:

```java
CommandFramework framework = CommandFramework.builder()
    .caseInsensitiveLiterals()
    .caseInsensitiveOptions()
    .build();
```
