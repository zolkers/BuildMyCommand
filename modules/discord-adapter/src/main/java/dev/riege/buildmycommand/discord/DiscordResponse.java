package dev.riege.buildmycommand.discord;

import dev.riege.buildmycommand.api.CommandResult;

import java.util.Objects;
import java.util.Optional;

public record DiscordResponse(
    CommandResult.Status status,
    Optional<String> content,
    DiscordMessageVisibility visibility
) {
    public DiscordResponse {
        Objects.requireNonNull(status, "status");
        content = Objects.requireNonNull(content, "content");
        Objects.requireNonNull(visibility, "visibility");
    }

    public static DiscordResponse silent() {
        return new DiscordResponse(CommandResult.Status.SILENT, Optional.empty(), DiscordMessageVisibility.EPHEMERAL);
    }
}
