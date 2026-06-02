# Discord Adapter

`modules/discord-adapter` proves commands are not Minecraft-specific.

The module intentionally avoids a hard dependency on JDA or Discord4J. It provides neutral boundary types:

- `DiscordUser`
- `DiscordTextCommand`
- `DiscordCommandSource`
- `DiscordResponse`
- `DiscordSlashCommand`

Text command dispatch:

```java
DiscordAdapter adapter = DiscordAdapter.attach(framework).prefix("!");
DiscordResponse response = adapter.execute(
    DiscordUser.of("42", "Ada", "cmd.admin"),
    DiscordTextCommand.of("!ping")
);
```

Slash command sync:

```java
List<DiscordSlashCommand> commands = adapter.slashCommands();
```

Messages are public by default. Use `DiscordMessages.ephemeral(...)` to request an ephemeral response through command message metadata.
