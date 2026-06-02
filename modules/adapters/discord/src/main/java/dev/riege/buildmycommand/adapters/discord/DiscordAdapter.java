package dev.riege.buildmycommand.adapters.discord;

import dev.riege.buildmycommand.adapters.AdapterCapabilities;
import dev.riege.buildmycommand.adapters.AdapterConfig;
import dev.riege.buildmycommand.adapters.AdapterRenderer;
import dev.riege.buildmycommand.adapters.AdapterRuntime;
import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class DiscordAdapter implements CommandAdapter<DiscordUser, DiscordTextCommand, DiscordResponse> {
    private final AdapterRuntime runtime;
    private final AdapterConfig config;
    private final DiscordResponseRenderer renderer = new DiscordResponseRenderer();
    private final DiscordSlashCommandSync slashCommandSync = new DiscordSlashCommandSync();
    private String prefix = "!";

    private DiscordAdapter(CommandFramework framework) {
        CommandPlatform platform = new CommandPlatform("discord", "Discord", true, true, true);
        this.runtime = new AdapterRuntime(framework, platform);
        this.config = new AdapterConfig("discord", "Discord", AdapterCapabilities.from(platform));
    }

    public static DiscordAdapter attach(CommandFramework framework) {
        return new DiscordAdapter(Objects.requireNonNull(framework, "framework"));
    }

    public DiscordAdapter prefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
        return this;
    }

    public String prefix() {
        return prefix;
    }

    public List<DiscordSlashCommand> slashCommands() {
        return slashCommandSync.commands(runtime.framework().graph());
    }

    @Override
    public AdapterRuntime runtime() {
        return runtime;
    }

    @Override
    public AdapterConfig config() {
        return config;
    }

    @Override
    public AdapterRenderer<DiscordResponse> renderer() {
        return renderer;
    }

    @Override
    public CommandSource mapSource(DiscordUser nativeSource) {
        return new DiscordCommandSource(nativeSource);
    }

    @Override
    public CommandInput mapInput(DiscordUser nativeSource, DiscordTextCommand nativeInput) {
        return new CommandInput(
            mapSource(nativeSource),
            nativeInput.content(),
            nativeInput.cursor(),
            prefix,
            runtime.platform()
        );
    }
}
