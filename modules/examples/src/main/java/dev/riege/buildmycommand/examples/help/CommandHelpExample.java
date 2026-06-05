/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.help;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.SuggestionContext;
import dev.riege.buildmycommand.api.SuggestionSet;
import dev.riege.buildmycommand.api.help.HelpOptions;
import dev.riege.buildmycommand.api.help.HelpProviderAPI;
import dev.riege.buildmycommand.core.CommandFramework;

public final class CommandHelpExample {
    private CommandHelpExample() {
    }

    public static void register(CommandFramework framework) {
        AnnotationCommandScanner.register(framework.registry(), new HelpCommands(framework.helpProvider()));
    }

    @CommandGroup("System")
    @CaseInsensitive(literals = true, options = true)
    public static final class HelpCommands {
        private final HelpProviderAPI help;

        public HelpCommands(HelpProviderAPI help) {
            this.help = help;
        }

        @Route(HelpProviderAPI.DEFAULT_ROUTE)
        @Description("Show visible commands or inspect one command")
        @Usage("/help [command] [--page <page>] [--size <size>] [--alphabetic] [--group <group>]")
        @Example({"/help", "/help profile message", "/help --alphabetic --page 2"})
        CommandResult help(@RouteCtx CommandContext route) {
            String query = route.optionalArg("query", String.class).orElse("");
            return Results.success(help.render(route.source(), query, HelpOptions.from(route)));
        }

        @Suggest("query")
        SuggestionSet commands(SuggestionContext context) {
            return SuggestionSet.of(help.suggest(context));
        }

        @Suggest("group")
        SuggestionSet groups(SuggestionContext context) {
            return SuggestionSet.of(help.suggestGroups(context.source(), context.currentToken()))
                .filteringCurrentToken();
        }
    }
}
