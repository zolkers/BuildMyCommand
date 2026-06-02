package dev.riege.buildmycommand.api;

import java.util.Objects;

public record CommandInput(CommandSource source, String raw, int cursor, String prefix, CommandPlatform platform) {
    public CommandInput {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(platform, "platform");
        cursor = Math.max(0, Math.min(cursor, raw.length()));
    }

    public static CommandInput raw(CommandSource source, String raw) {
        return new CommandInput(source, raw, raw.length(), "", CommandPlatform.test());
    }

    public String normalizedInput() {
        if (!prefix.isEmpty() && raw.startsWith(prefix)) {
            return raw.substring(prefix.length());
        }
        return raw;
    }

    public int normalizedCursor() {
        if (!prefix.isEmpty() && raw.startsWith(prefix)) {
            return Math.max(0, cursor - prefix.length());
        }
        return cursor;
    }
}
