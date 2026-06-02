package dev.riege.buildmycommand.adapters.minecraft.common;

import java.util.Objects;
import java.util.Optional;

public record MinecraftRenderedResult(
    int numericResult,
    String renderedMessage
) {
    public MinecraftRenderedResult {
        if (numericResult < 0) {
            throw new IllegalArgumentException("numericResult must be positive or zero");
        }
    }

    public Optional<String> message() {
        return Optional.ofNullable(renderedMessage);
    }

    public static MinecraftRenderedResult of(int numericResult, String message) {
        if (message != null) {
            Objects.requireNonNull(message, "message");
        }
        return new MinecraftRenderedResult(numericResult, message);
    }
}
