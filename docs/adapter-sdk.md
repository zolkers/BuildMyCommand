# Adapter SDK

Adapters convert native platform concepts into framework primitives.

The SDK lives in `modules/adapters/core`. First-party adapters live next to it as sibling modules:
`modules/adapters/brigadier`, `modules/adapters/terminal`, `modules/adapters/discord`, and
`modules/adapters/minecraft/*`.

`modules/adapters/brigadier` is the direct adapter for any platform exposing a Mojang
`CommandDispatcher<S>`. Use `BrigadierCommandAdapter.create(framework, sourceMapper)` and
`adapter.registration().register(dispatcher)` when the host already owns Brigadier registration.

Implement `CommandAdapter<S, I, R>`:

- `S`: native source, such as a sender, player, terminal session, or Discord user.
- `I`: native input, such as a raw string or platform command event.
- `R`: native render result.

The adapter owns:

- `mapSource(S)` to create a `CommandSource`
- `mapInput(S, I)` to create a `CommandInput`
- `renderer()` to convert `CommandResult` into platform output
- `runtime()` to dispatch through `CommandFramework`
- `config()` to expose capabilities

Keep native APIs at adapter boundaries. `core` and `api` should never depend on Bukkit, Brigadier, JDA, terminal libraries, or platform event types.

For simple platforms, use `SimpleCommandAdapter` instead of writing a full class:

```java
CommandAdapter<MyUser, String, MyReply> adapter =
    SimpleCommandAdapter.<MyUser, String, MyReply>builder(framework, platform)
        .sourceMapper(user -> new MyCommandSource(user))
        .inputMapper((user, input, runtime, mapper) ->
            new CommandInput(mapper.map(user), input, input.length(), "", runtime.platform()))
        .renderer(result -> MyReply.from(result))
        .build();
```
