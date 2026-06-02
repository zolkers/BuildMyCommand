# Minecraft Proxy Adapters

BungeeCord and Velocity live under `modules/adapters/minecraft`.

Bungee support focuses on:

- root command registration and unregistration
- alias-aware command objects
- tab completion through core suggestions
- sender permission mapping

Velocity support focuses on:

- simple command dispatch
- Brigadier command projection where useful
- proxy source permission checks
- suggestion ownership through the framework

Both adapters keep proxy APIs outside `core`.
