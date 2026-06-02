package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Objects;
import java.util.Optional;

public record MinecraftSourceDescriptor(
    MinecraftSourceKind kind,
    String sourceName,
    Object nativeHandle
) {
    public MinecraftSourceDescriptor {
        Objects.requireNonNull(kind, "kind");
    }

    public Optional<String> name() {
        return Optional.ofNullable(sourceName);
    }

    public Optional<Object> nativeHandleOptional() {
        return Optional.ofNullable(nativeHandle);
    }
}
