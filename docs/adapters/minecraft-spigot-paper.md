# Minecraft Spigot And Paper

Spigot uses native `label + args[]` style registration. The adapter reconstructs framework input, preserves the invoked label, maps sender permissions, and exposes tab completion through core suggestions.

Paper can use either:

- native command registration for framework-owned parsing and case policy
- Brigadier projection for client-visible command trees and native completions

Brigadier literal parsing is exact before execution reaches BuildMyCommand. Use the native adapter path when case-insensitive command names matter more than a native Brigadier tree.
