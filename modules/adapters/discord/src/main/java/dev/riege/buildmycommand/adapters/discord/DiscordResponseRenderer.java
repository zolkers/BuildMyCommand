package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.CommandResult;

import java.util.Optional;

public final class DiscordResponseRenderer implements AdapterRenderer<DiscordResponse> {
    public static final String EPHEMERAL_METADATA_KEY = "discord.ephemeral";

    @Override
    public DiscordResponse render(CommandResult result) {
        if (result.status() == CommandResult.Status.SILENT || result.message().isEmpty()) {
            return DiscordResponse.silent();
        }
        CommandMessage message = result.message().orElseThrow();
        return new DiscordResponse(
            result.status(),
            Optional.of(message.text()),
            visibility(message)
        );
    }

    private static DiscordMessageVisibility visibility(CommandMessage message) {
        Object value = message.metadata().get(EPHEMERAL_METADATA_KEY);
        if (Boolean.TRUE.equals(value)) {
            return DiscordMessageVisibility.EPHEMERAL;
        }
        return DiscordMessageVisibility.PUBLIC;
    }
}
