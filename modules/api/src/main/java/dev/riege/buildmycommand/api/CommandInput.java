package dev.riege.buildmycommand.api;

import java.util.Objects;

public record CommandInput(
    CommandSource source,
    String rawInput,
    String normalizedInput,
    int cursor,
    String prefix,
    CommandPlatform platform
) {
    public CommandInput {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(rawInput, "rawInput");
        Objects.requireNonNull(normalizedInput, "normalizedInput");
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(platform, "platform");
        cursor = Math.max(0, Math.min(cursor, rawInput.length()));
    }

    public CommandInput(CommandSource source, String rawInput, int cursor, String prefix, CommandPlatform platform) {
        this(source, rawInput, normalize(rawInput, prefix), cursor, prefix, platform);
    }

    public static CommandInput raw(CommandSource source, String raw) {
        return new CommandInput(source, raw, raw.length(), "", CommandPlatform.test());
    }

    public static CommandInput normalized(CommandSource source, String normalizedInput) {
        return new CommandInput(source, normalizedInput, normalizedInput, normalizedInput.length(), "", CommandPlatform.test());
    }

    public int normalizedCursor() {
        if (!prefix.isEmpty() && rawInput.startsWith(prefix)) {
            return Math.max(0, Math.min(cursor - prefix.length(), normalizedInput.length()));
        }
        return Math.max(0, Math.min(cursor, normalizedInput.length()));
    }

    private static String normalize(String rawInput, String prefix) {
        Objects.requireNonNull(rawInput, "rawInput");
        Objects.requireNonNull(prefix, "prefix");
        if (!prefix.isEmpty() && rawInput.startsWith(prefix)) {
            return rawInput.substring(prefix.length());
        }
        return rawInput;
    }
}
