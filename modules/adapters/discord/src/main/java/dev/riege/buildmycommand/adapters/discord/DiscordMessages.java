package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.api.CommandMessage;
import dev.riege.buildmycommand.api.MessageLevel;

import java.util.Map;

public final class DiscordMessages {
    private DiscordMessages() {
    }

    public static CommandMessage publicMessage(String text, MessageLevel level) {
        return new CommandMessage(text, level, Map.of(DiscordResponseRenderer.EPHEMERAL_METADATA_KEY, false));
    }

    public static CommandMessage ephemeral(String text, MessageLevel level) {
        return new CommandMessage(text, level, Map.of(DiscordResponseRenderer.EPHEMERAL_METADATA_KEY, true));
    }
}
