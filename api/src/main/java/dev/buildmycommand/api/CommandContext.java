package dev.buildmycommand.api;

import java.util.Objects;

public record CommandContext(CommandSource source, String input) {
    public CommandContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
    }
}
