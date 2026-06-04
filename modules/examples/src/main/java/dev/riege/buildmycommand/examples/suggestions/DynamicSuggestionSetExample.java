/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.suggestions;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionSet;
import dev.riege.buildmycommand.api.Suggestions;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Optional;

public final class DynamicSuggestionSetExample {
    private static final String PLAYERS_METADATA = "players";

    private DynamicSuggestionSetExample() {
    }

    public static CommandFramework annotationFramework() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new PartyCommands());
        return framework;
    }

    public static CommandFramework builderFramework() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("party invite <target:String>")
            .description("Invite an online player")
            .argumentSuggestions("target", Suggestions.dynamic(DynamicSuggestionSetExample::onlinePlayers))
            .executes(ctx -> Results.success("invite " + ctx.arg("target", String.class)));
        return framework;
    }

    public static List<String> suggestAnnotation(List<String> onlinePlayers, String input, int cursor) {
        return annotationFramework().suggest(source(onlinePlayers), input, cursor);
    }

    public static List<String> suggestBuilder(List<String> onlinePlayers, String input, int cursor) {
        return builderFramework().suggest(source(onlinePlayers), input, cursor);
    }

    public static List<String> suggestWithoutPlayerMetadata(String input, int cursor) {
        return annotationFramework().suggest(new CommandSource() {
        }, input, cursor);
    }

    public static List<String> richTooltipValues(List<String> onlinePlayers, String input) {
        return annotationFramework()
            .suggestRich(CommandInput.raw(source(onlinePlayers), input))
            .stream()
            .filter(suggestion -> suggestion.tooltip().isPresent())
            .map(suggestion -> suggestion.value() + ":" + suggestion.tooltip().orElseThrow())
            .toList();
    }

    public static CommandResult dispatchAnnotation(String input) {
        return annotationFramework().dispatch(source(List.of()), input);
    }

    public static CommandResult dispatchBuilder(String input) {
        return builderFramework().dispatch(source(List.of()), input);
    }

    public static Optional<Object> metadata(List<String> onlinePlayers, String key) {
        return PLAYERS_METADATA.equals(key) ? Optional.of(onlinePlayers) : Optional.empty();
    }

    private static SuggestionSet onlinePlayers(SuggestionContext context) {
        return SuggestionSet.of(players(context))
            .filteringCurrentToken()
            .tooltip("online player")
            .priority(10)
            .limit(20);
    }

    private static List<String> players(SuggestionContext context) {
        Optional<Object> metadata = context.sourceMetadata(PLAYERS_METADATA);
        if (metadata.isEmpty() || !(metadata.orElseThrow() instanceof List<?> players)) {
            return List.of();
        }
        return players.stream()
            .map(String::valueOf)
            .toList();
    }

    private static CommandSource source(List<String> onlinePlayers) {
        return new CommandSource() {
            @Override
            public Optional<Object> metadata(String key) {
                return DynamicSuggestionSetExample.metadata(onlinePlayers, key);
            }
        };
    }

    static final class PartyCommands {
        @Route("party invite <target:String>")
        @Description("Invite an online player")
        CommandResult invite(@RouteCtx CommandContext route) {
            return Results.success("invite " + route.arg("target", String.class));
        }

        @Suggest("target")
        SuggestionSet onlinePlayerTargets(SuggestionContext context) {
            return onlinePlayers(context);
        }
    }
}
