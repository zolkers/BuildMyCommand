# Minecraft Mod Loader Adapters

Fabric, Forge, and NeoForge expose registration helpers around their command registration events.

The mod loader adapters use the common Brigadier bridge so the framework command graph remains the source of truth.

Key constraints:

- registration happens during platform command events
- reloads may rebuild the native command tree
- native source/context signatures vary by version
- Brigadier literal matching stays exact unless a future hybrid catch-all strategy is selected

Use `CommandFramework.builder().caseInsensitiveLiterals()` for framework-level matching when input reaches BuildMyCommand directly.
