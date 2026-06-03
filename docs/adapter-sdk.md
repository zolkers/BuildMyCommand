# Adapter SDK

Adapters convert native platform concepts into framework primitives.

The SDK lives in `modules/adapters/core`. First-party adapters live next to it as sibling modules:
`modules/adapters/brigadier`, `modules/adapters/terminal`, `modules/adapters/discord`, and
`modules/adapters/minecraft/*`.

`modules/adapters/brigadier` is the direct adapter for any platform exposing a Mojang
`CommandDispatcher<S>`. Use `BrigadierCommandAdapter.create(framework, sourceMapper)` and
`adapter.registration().register(dispatcher)` when the host already owns Brigadier registration.

Implement `IAdapter<S, I, R>` directly when building a first-class adapter, or implement
`CommandAdapter<S, I, R>` when the default SDK behavior is enough:

- `S`: native source, such as a sender, player, terminal session, or Discord user.
- `I`: native input, such as a raw string or platform command event.
- `R`: native render result.

The adapter contract exposes:

- `mapSource(S)` to create a `CommandSource`
- `mapInput(S, I)` to create a `CommandInput`
- `renderer()` to convert `CommandResult` into platform output
- `runtime()` to dispatch through `CommandFramework`
- `config()` to expose capabilities
- `matchingPolicy()` to declare literal, option, and argument case behavior
- `registrationLabels()` to expose root literals and aliases as a structured SDK value
- `suggestRich(S, I, cursor)` and `suggest(S, I, cursor)` to route platform completion through the framework

`CommandAdapter` extends `IAdapter` and provides the standard implementation for dispatch,
rendering, suggestions, registration labels, capabilities, and matching policy. Use it unless a
platform has unusual dispatch or suggestion semantics.

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

The Brigadier adapter follows the same architecture as frameworks such as Imperat: keep a framework
command model, project that model into Brigadier nodes, wrap native sources into framework sources,
map argument types at the projection boundary, and delegate execution/suggestions back to the
framework. In BuildMyCommand this lives in `BrigadierCommandAdapter` and `BrigadierRegistration`.
