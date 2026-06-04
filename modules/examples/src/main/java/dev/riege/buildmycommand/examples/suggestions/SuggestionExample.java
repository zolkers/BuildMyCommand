/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.suggestions;

import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Locale;

public final class SuggestionExample {
    private SuggestionExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .suggestionProvider(OnlinePlayer.class, context -> List.of("Ada", "Alex", "Steve").stream()
                .filter(name -> name.toLowerCase(Locale.ROOT)
                    .startsWith(context.rawToken().toLowerCase(Locale.ROOT)))
                .toList())
            .argumentParser(OnlinePlayer.class, (rawToken, context) -> rawToken.isBlank()
                ? ArgumentParseResult.failure("player is required")
                : ArgumentParseResult.success(new OnlinePlayer(rawToken)))
            .build();
        framework.registry().command("message", message -> message
            .argument("target", OnlinePlayer.class)
            .greedyArgument("text", String.class)
            .executes(ctx -> Results.success("DM " + ctx.arg("target", OnlinePlayer.class).name())));
        return framework;
    }

    public static List<String> suggestTargets(String input, int cursor) {
        return create().suggest(new CommandSource() {
        }, input, cursor);
    }

    public record OnlinePlayer(String name) {
    }
}
