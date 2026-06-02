# Terminal Adapter

`modules/terminal-adapter` is a local/debug adapter.

```java
TerminalAdapter adapter = TerminalAdapter.attach(framework)
    .exitCommand("quit")
    .historyEnabled(true);

adapter.runLoop();
```

The adapter supports:

- one-shot dispatch through `runOnce`
- repeated dispatch through `runLoop`
- configurable exit command
- disabled-by-default history
- completion delegated to `CommandFramework.suggest`

It uses the generic adapter SDK, so terminal input still flows through `CommandInput`, platform metadata, core dispatch, and result rendering.
