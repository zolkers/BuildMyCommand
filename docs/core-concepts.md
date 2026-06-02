# Core Concepts

The framework is centered on a few public primitives.

`CommandFramework` owns the registry, dispatcher, suggestions, help, schema, middleware, and lifecycle listeners.

`CommandSource` represents the caller. It exposes optional identity, locale, metadata, permission checks, native unwrapping, and reply hooks.

`CommandInput` preserves the raw platform input, normalized command text, cursor, prefix, and platform descriptor.

`CommandResult` carries a status and an optional `CommandMessage`. String helpers in `Results` remain available for simple commands.

`CommandNode` is the platform-neutral command graph. Manual nodes, builder registrations, route DSL strings, and annotations all compile into this shape.

`CommandPlatform` describes adapter capabilities such as rich messages, autocomplete, and permissions. Core dispatch does not depend on platform APIs.
