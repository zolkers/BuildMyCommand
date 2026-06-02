# Testing

`modules/testkit` provides fluent framework tests without a platform adapter.

```java
CommandFramework framework = CommandFramework.create();
framework.registry()
    .command("ban", command -> command
        .permission("cmd.ban")
        .executes(ctx -> Results.success("banned")));

CommandTestKit kit = CommandTestKit.create(
    framework,
    TestCommandSource.builder().permission("cmd.ban").build()
);

kit.assertDispatch("ban").succeedsWith("banned");
kit.assertSuggestions("b").contains("ban");
kit.assertSchema().containsCommand("ban");
```

`TestCommandSource` can model ids, names, locales, metadata, and permissions. `TestPlatforms.fake(...)` verifies platform-aware dispatch paths.
