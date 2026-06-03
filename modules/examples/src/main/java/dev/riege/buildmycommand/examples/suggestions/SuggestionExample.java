package dev.riege.buildmycommand.examples.suggestions;

import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;

public final class SuggestionExample {
    private SuggestionExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .suggestionProvider(OnlinePlayer.class, context -> List.of("Ada", "Alex", "Steve").stream()
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT)
                    .startsWith(context.rawToken().toLowerCase(java.util.Locale.ROOT)))
                .toList())
            .argumentParser(OnlinePlayer.class, (rawToken, context) -> rawToken.isBlank()
                ? dev.riege.buildmycommand.api.ArgumentParseResult.failure("player is required")
                : dev.riege.buildmycommand.api.ArgumentParseResult.success(new OnlinePlayer(rawToken)))
            .build();
        framework.registry()
            .route("message <target:OnlinePlayer> <text:String...>")
            .executes(ctx -> Results.success("DM " + ctx.arg("target", OnlinePlayer.class).name()));
        return framework;
    }

    public static List<String> suggestTargets(String input, int cursor) {
        return create().suggest(new CommandSource() {
        }, input, cursor);
    }

    public record OnlinePlayer(String name) {
    }
}
