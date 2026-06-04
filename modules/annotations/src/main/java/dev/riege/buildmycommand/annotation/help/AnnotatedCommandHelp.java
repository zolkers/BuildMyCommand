/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation.help;

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
import dev.riege.buildmycommand.core.help.CommandHelp;
import dev.riege.buildmycommand.core.help.CommandHelpOptions;

import java.util.Objects;

@CommandGroup("System")
@CaseInsensitive(literals = true, options = true)
public final class AnnotatedCommandHelp {
    private final CommandHelp help;

    public AnnotatedCommandHelp(CommandHelp help) {
        this.help = Objects.requireNonNull(help, "help");
    }

    @Route("help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]")
    @Description("Show visible commands or inspect one command")
    @Usage("/help [command] [--page <page>] [--size <size>] [--alphabetic] [--group <group>]")
    @Example({"/help", "/help profile message", "/help --alphabetic --page 2", "/help --group Administration"})
    public CommandResult help(@RouteCtx CommandContext route) {
        return Results.success(help.render(
            route.source(),
            route.optionalArg("query", String.class).map(String::trim).orElse(""),
            CommandHelpOptions.from(route)
        ));
    }

    @Suggest("query")
    public SuggestionSet commands(SuggestionContext context) {
        return SuggestionSet.of(help.suggest(context.source(), context.currentToken())).filteringCurrentToken();
    }

    @Suggest("group")
    public SuggestionSet groups(SuggestionContext context) {
        return SuggestionSet.of(help.suggestGroups(context.source(), context.currentToken())).filteringCurrentToken();
    }
}
