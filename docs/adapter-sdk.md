# Adapter SDK

Adapters convert native platform concepts into framework primitives.

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
