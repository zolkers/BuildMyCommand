package dev.riege.buildmycommand.examples.adapters;

import dev.riege.buildmycommand.adapters.CommandAdapter;
import dev.riege.buildmycommand.adapters.SimpleCommandAdapter;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Optional;

public final class SimpleAdapterExample {
    private static final CommandPlatform PLATFORM = new CommandPlatform("chat", "Chat", true, true, true);

    private SimpleAdapterExample() {
    }

    public static CommandAdapter<ChatUser, ChatMessage, ChatReply> create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().route("hello").executes(ctx -> Results.success("Hello " + ctx.source().name().orElse("there")));
        return SimpleCommandAdapter.<ChatUser, ChatMessage, ChatReply>builder(framework, PLATFORM)
            .sourceMapper(user -> new CommandSource() {
                @Override
                public Optional<String> name() {
                    return Optional.of(user.name());
                }

                @Override
                public boolean hasPermission(String permission) {
                    return user.permissions().contains(permission);
                }
            })
            .inputMapper((user, message, runtime, mapper) -> new CommandInput(
                mapper.map(user),
                message.raw(),
                message.raw().startsWith("!") ? message.raw().substring(1) : message.raw(),
                message.raw().length(),
                message.raw().startsWith("!") ? "!" : "",
                runtime.platform()
            ))
            .renderer(result -> new ChatReply(result.status() == CommandResult.Status.SUCCESS, result.reply().orElse("")))
            .build();
    }

    public record ChatUser(String name, java.util.Set<String> permissions) {
    }

    public record ChatMessage(String raw) {
    }

    public record ChatReply(boolean ok, String text) {
    }
}
