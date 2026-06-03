# Minecraft Mod Loader Adapters

Fabric, Forge, and NeoForge expose Mojang Brigadier dispatchers during their command registration events.

BuildMyCommand does not publish separate Fabric, Forge, or NeoForge adapter modules. Use `modules/adapters/brigadier` directly from the loader event:

```java
BrigadierCommandAdapter<NativeSource> adapter = BrigadierCommandAdapter.create(framework, sourceMapper);
adapter.registration().register(dispatcher);
```

This keeps the framework command graph as the source of truth without wrapping each mod loader in a module that only forwards to `CommandDispatcher<S>`.

Key constraints:

- registration happens during platform command events
- reloads may rebuild the native command tree
- native source/context signatures vary by version
- Brigadier literal matching stays exact unless a future hybrid catch-all strategy is selected

Use `CommandFramework.builder().caseInsensitiveLiterals()` for framework-level matching when input reaches BuildMyCommand directly.
