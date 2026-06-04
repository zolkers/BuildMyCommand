/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.help;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.core.parse.ArgumentParserRegistry;
import dev.riege.buildmycommand.core.parse.CommandTokenizer;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuggestionEngineCoverageTest {
    @Test
    void suggestsRootsAliasesChildrenFlagsOptionsAndProviderValues() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("Ban|Block <target:String> [--duration:Integer|-d] [--silent|-s]")
            .permission("ban.use")
            .argumentSuggestions("target", "players", ctx -> List.of("Ada"))
            .optionSuggestions("duration", "durations", ctx -> List.of("60"))
            .executes(ctx -> Results.silent());
        registry.route("Admin pardon").permission("ban.use").executes(ctx -> Results.silent());
        registry.route("hidden").hidden().executes(ctx -> Results.silent());
        CommandMatchingPolicy policy = new CommandMatchingPolicy(true, true);
        policy.enableCaseInsensitiveLiterals();
        policy.enableCaseInsensitiveOptions();
        SuggestionEngine engine = new SuggestionEngine(registry, new CommandTokenizer(), policy);
        CommandSource allowed = source(Set.of("ban.use"));

        assertEquals(List.of("Ban", "Block"), values(engine.suggestRich(input(allowed, "b", 1))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "b", 1))));
        assertEquals(List.of("Ada"), values(engine.suggestRich(input(allowed, "Ban ", 4))));
        assertEquals(List.of("--duration", "-d", "--silent", "-s"), values(engine.suggestRich(input(allowed, "Ban Ada -", 9))));
        assertEquals(List.of("60"), values(engine.suggestRich(input(allowed, "Ban Ada --duration ", 19))));
        assertEquals(List.of("pardon"), values(engine.suggestRich(input(allowed, "Admin p", 7))));
        assertEquals(SuggestionType.FLAG, engine.suggestRich(input(allowed, "Ban Ada -", 9)).get(0).type());
    }

    @Test
    void suggestsParserValuesWhenCustomProvidersAreAbsentAndHandlesFailureStates() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.command("mode", command -> command
            .argument("type", Mode.class)
            .executes(ctx -> Results.silent()));
        registry.command("modeopt", command -> command
            .option("type", Mode.class, "t")
            .executes(ctx -> Results.silent()));
        registry.command("secure", command -> command
            .argument("target", Integer.class)
            .subcommand("child", child -> child.permission("secure.child").executes(ctx -> Results.silent())));
        registry.command("admin", command -> command
            .subcommand("child", child -> child.executes(ctx -> Results.silent())));
        registry.command("multi", command -> command
            .argument("first", String.class)
            .argument("second", Mode.class)
            .option("type", Mode.class)
            .flag("silent")
            .executes(ctx -> Results.silent()));
        registry.command("nested", command -> command
            .optionalArgument("target", String.class)
            .subcommand("child", child -> child.executes(ctx -> Results.silent())));
        registry.command("locked", command -> command
            .permission("locked.use")
            .argument("target", Mode.class)
            .option("type", Mode.class)
            .flag("silent")
            .executes(ctx -> Results.silent()));
        registry.command("hiddenArg", command -> command
            .hidden()
            .argument("target", Mode.class)
            .option("type", Mode.class)
            .flag("silent")
            .executes(ctx -> Results.silent()));
        SuggestionEngine engine = new SuggestionEngine(
            registry,
            new CommandTokenizer(),
            CommandMatchingPolicy.strict(),
            new ArgumentParserRegistry()
        );

        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "\"bad", 4))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "unknown ", 8))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "mode ", 5))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "modeopt --type ", 15))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "modeopt -t ", 11))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "modeopt -x ", 11))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "multi first ", 12))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "multi --type CREATIVE ", 22))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "multi -- ", 9))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "multi - ", 8))));
        assertEquals(List.of("SURVIVAL", "CREATIVE"), values(engine.suggestRich(input(source(Set.of()), "multi -1 ", 9))));
        assertEquals(List.of("--type", "--silent"), values(engine.suggestRich(input(source(Set.of()), "multi -", 7))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "multi --silent ", 15))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "multi -x ", 9))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "mode --bad ", 11))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "mode extra ", 11))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "secure notAnInt ", 16))));
        assertEquals(List.of("child"), values(engine.suggestRich(input(source(Set.of()), "admin x ", 8))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "secure -x c", 11))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "nested child ", 13))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "secure 5 c", 10))));
        assertEquals(List.of("child"), values(engine.suggestRich(input(source(Set.of("secure.child")), "secure 5 c", 10))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "locked --type ", 14))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "locked -", 8))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "locked ", 7))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "hiddenArg --type ", 17))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "hiddenArg -", 11))));
        assertEquals(List.of(), values(engine.suggestRich(input(source(Set.of()), "hiddenArg ", 10))));
    }

    @Test
    void compatibilityConstructorsAndStringSuggestionFacadeWork() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("root child").executes(ctx -> Results.silent());
        SuggestionEngine defaultEngine = new SuggestionEngine(registry, new CommandTokenizer());
        SuggestionEngine policyEngine = new SuggestionEngine(registry, new CommandTokenizer(), CommandMatchingPolicy.strict());

        assertEquals(List.of("root"), defaultEngine.suggest(source(Set.of()), "r", 1));
        assertEquals(List.of("child"), policyEngine.suggestRich(input(source(Set.of()), "root c", 6)).stream()
            .map(Suggestion::value)
            .toList());
    }

    @Test
    void canHideAliasesFromSuggestionsWithoutDisablingExecution() {
        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("wecc bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.success("ok"));
        SuggestionEngine engine = new SuggestionEngine(registry, new CommandTokenizer());

        assertEquals(List.of("bang"), values(engine.suggestRich(input(source(Set.of()), "wecc ", 5))));
        assertEquals(List.of("bang"), values(engine.suggestRich(input(source(Set.of()), "wecc b", 6))));
    }

    private static List<String> values(List<Suggestion> suggestions) {
        return suggestions.stream().map(Suggestion::value).toList();
    }

    private static CommandInput input(CommandSource source, String text, int cursor) {
        return new CommandInput(source, text, cursor, "", dev.riege.buildmycommand.api.CommandPlatform.test());
    }

    private static CommandSource source(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }

    private enum Mode {
        SURVIVAL,
        CREATIVE
    }
}
