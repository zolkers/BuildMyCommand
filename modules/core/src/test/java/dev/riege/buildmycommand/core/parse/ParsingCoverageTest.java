/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.parse;

import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandMatchingPolicy;
import dev.riege.buildmycommand.core.route.ElementRouteStep;
import dev.riege.buildmycommand.core.route.RoutePattern;
import dev.riege.buildmycommand.core.route.RoutePatternParser;
import dev.riege.buildmycommand.core.registry.RegistryArgumentKind;
import dev.riege.buildmycommand.core.registry.RegistryArgumentSpec;
import dev.riege.buildmycommand.core.registry.RegistryOptionKind;
import dev.riege.buildmycommand.core.registry.RegistryOptionSpec;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingCoverageTest {
    @Test
    void argumentParserRegistryCoversDefaultSuccessAndFailureParsers() {
        ArgumentParserRegistry registry = new ArgumentParserRegistry();
        ArgumentParseContext context = context("value", String.class, "raw");

        assertEquals("raw", registry.parse(String.class, "raw", context).value());
        assertEquals(1, registry.parse(int.class, "1", context).value());
        assertEquals("Invalid integer", registry.parse(Integer.class, "x", context).failure().orElseThrow());
        assertEquals(2L, registry.parse(long.class, "2", context).value());
        assertEquals("Invalid long", registry.parse(Long.class, "x", context).failure().orElseThrow());
        assertEquals(1.5f, registry.parse(float.class, "1.5", context).value());
        assertEquals("Invalid float", registry.parse(Float.class, "NaN", context).failure().orElseThrow());
        assertEquals("Invalid float", registry.parse(Float.class, "x", context).failure().orElseThrow());
        assertEquals(2.5d, registry.parse(double.class, "2.5", context).value());
        assertEquals("Invalid double", registry.parse(Double.class, "Infinity", context).failure().orElseThrow());
        assertEquals("Invalid double", registry.parse(Double.class, "x", context).failure().orElseThrow());
        assertEquals(true, registry.parse(boolean.class, "true", context).value());
        assertEquals(false, registry.parse(Boolean.class, "false", context).value());
        assertEquals("Invalid boolean", registry.parse(Boolean.class, "yes", context).failure().orElseThrow());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"),
            registry.parse(UUID.class, "00000000-0000-0000-0000-000000000001", context).value());
        assertEquals("Invalid UUID", registry.parse(UUID.class, "nope", context).failure().orElseThrow());
        assertEquals(Duration.ofSeconds(5), registry.parse(Duration.class, "PT5S", context).value());
        assertEquals("Invalid duration", registry.parse(Duration.class, "5s", context).failure().orElseThrow());
        assertEquals(LocalDate.parse("2026-06-03"), registry.parse(LocalDate.class, "2026-06-03", context).value());
        assertEquals("Invalid LocalDate", registry.parse(LocalDate.class, "03/06/2026", context).failure().orElseThrow());
        assertEquals(LocalDateTime.parse("2026-06-03T10:15:30"),
            registry.parse(LocalDateTime.class, "2026-06-03T10:15:30", context).value());
        assertEquals("Invalid LocalDateTime", registry.parse(LocalDateTime.class, "bad", context).failure().orElseThrow());
        assertEquals(Path.of("abc"), registry.parse(Path.class, "abc", context).value());
        assertEquals("Invalid path", registry.parse(Path.class, "\0", context).failure().orElseThrow());
        assertEquals(URI.create("https://example.com"), registry.parse(URI.class, "https://example.com", context).value());
        assertEquals("Invalid URI", registry.parse(URI.class, "http://[", context).failure().orElseThrow());
        assertEquals(URL.class, registry.parse(URL.class, "https://example.com", context).value().getClass());
        assertEquals("Invalid URL", registry.parse(URL.class, "http://[", context).failure().orElseThrow());
        assertEquals(Mode.SURVIVAL, registry.parse(Mode.class, "SURVIVAL", context).value());
        assertEquals("Invalid enum", registry.parse(Mode.class, "creative", context).failure().orElseThrow());
        assertEquals("No parser registered", registry.parse(Object.class, "x", context).failure().orElseThrow());
    }

    @Test
    void parserAndSuggestionOverridesTakePrecedence() {
        ArgumentParserRegistry registry = new ArgumentParserRegistry(Map.of(), Map.of());
        ArgumentParseContext context = context("mode", Mode.class, "S");

        assertEquals("No parser registered", registry.parse(String.class, "x", context).failure().orElseThrow());
        assertEquals(List.of("SURVIVAL", "CREATIVE"), registry.suggestions(Mode.class, context).stream()
            .map(Suggestion::value)
            .toList());
        assertEquals(List.of(), registry.suggestions(Object.class, context));

        registry.register(String.class, (raw, ignored) -> ArgumentParseResult.success(raw.toUpperCase()));
        registry.registerSuggestions(String.class, ignored -> List.of("provider"));
        assertEquals("ADA", registry.parse(String.class, "ada", context).value());
        assertEquals(List.of("provider"), registry.suggestions(String.class, context).stream().map(Suggestion::value).toList());

        registry.register(Integer.class, (raw, ignored) -> ArgumentParseResult.failure("bad custom"));
        assertEquals("bad custom", registry.parse(Integer.class, "1", context).failure().orElseThrow());
        assertFalse(registry.parsers().isEmpty());
        assertFalse(registry.suggestionProviders().isEmpty());
    }

    @Test
    void optionParserHandlesFlagsValuesUnknownsAndCasePolicies() {
        ArgumentParserRegistry parsers = new ArgumentParserRegistry();
        CommandMatchingPolicy policy = new CommandMatchingPolicy(false, true);
        OptionParser parser = new OptionParser(parsers, policy);
        OptionParser strictParser = new OptionParser(parsers);
        CommandInput input = CommandInput.raw(new CommandSource() {
        }, "ban Alex --Duration 5 -S");
        List<RegistryOptionSpec> specs = List.of(
            new RegistryOptionSpec("duration", Integer.class, "d", RegistryOptionKind.VALUE),
            new RegistryOptionSpec("silent", boolean.class, "s", RegistryOptionKind.FLAG)
        );

        ParseOptionsResult result = parser.parseOptions(specs, List.of("Alex", "--Duration", "5", "-S"), input);

        assertTrue(result.failure().isEmpty());
        assertEquals(List.of("Alex"), result.positionals());
        assertEquals(5, result.values().get("duration"));
        assertEquals(true, result.values().get("silent"));
        assertEquals(List.of("--"), parser.parseOptions(specs, List.of("--"), input).positionals());
        assertEquals("Unknown flag or option: --missing", parser.parseOptions(specs, List.of("--missing"), input)
            .failure().orElseThrow());
        assertEquals(List.of("-1"), parser.parseOptions(specs, List.of("-1"), input).positionals());
        assertEquals("Unknown flag or option: -x", parser.parseOptions(specs, List.of("-x"), input).failure().orElseThrow());
        assertEquals("Missing value for option: duration", parser.parseOptions(specs, List.of("--duration"), input)
            .failure().orElseThrow());
        assertEquals("Invalid integer for option duration: bad", parser.parseOptions(specs, List.of("--duration", "bad"), input)
            .failure().orElseThrow());
        assertEquals("Unknown flag or option: --Duration", strictParser.parseOptions(specs, List.of("--Duration", "5"), input)
            .failure().orElseThrow());
    }

    @Test
    void tokenizerCoversQuotesEscapesWhitespaceAndFailures() {
        CommandTokenizer tokenizer = new CommandTokenizer();

        assertEquals(List.of(), tokenizer.tokenize("   ").tokens());
        assertEquals(List.of("say", "hello world", "it's", "\\q", "\\"),
            tokenizer.tokenize("say \"hello world\" it\\'s \\q \\").tokens());
        assertEquals(List.of("a b", "c\\d"), tokenizer.tokenize("'a b' c\\\\d").tokens());
        assertEquals("Unclosed quote", tokenizer.tokenize("\"unterminated").failure().orElseThrow());
    }

    @Test
    void argumentResolverCoversGreedyOptionalPrefixAndFailureBranches() {
        ArgumentParserRegistry parsers = new ArgumentParserRegistry();
        parsers.register(BrokenValue.class, (raw, ignored) -> ArgumentParseResult.failure("broken"));
        ArgumentResolver resolver = new ArgumentResolver(parsers);
        CommandInput input = CommandInput.raw(new CommandSource() {
        }, "cmd");
        List<RegistryArgumentSpec> greedy = List.of(new RegistryArgumentSpec("reason", String.class, RegistryArgumentKind.GREEDY));
        List<RegistryArgumentSpec> brokenGreedy = List.of(new RegistryArgumentSpec("reason", BrokenValue.class, RegistryArgumentKind.GREEDY));
        List<RegistryArgumentSpec> optionalGreedy = List.of(new RegistryArgumentSpec("reason", String.class, RegistryArgumentKind.OPTIONAL_GREEDY));
        List<RegistryArgumentSpec> requiredInt = List.of(new RegistryArgumentSpec("amount", Integer.class, RegistryArgumentKind.REQUIRED));
        List<RegistryArgumentSpec> optionalInt = List.of(new RegistryArgumentSpec("amount", Integer.class, RegistryArgumentKind.OPTIONAL));

        assertEquals("Missing required argument: reason", resolver.parseArguments(greedy, List.of(), input).failure().orElseThrow());
        assertTrue(resolver.parseArguments(optionalGreedy, List.of(), input).failure().isEmpty());
        assertEquals("hello world", resolver.parseArguments(greedy, List.of("hello", "world"), input).values().get("reason"));
        assertEquals("broken for argument reason: hello world", resolver.parseArguments(brokenGreedy, List.of("hello", "world"), input)
            .failure().orElseThrow());
        assertEquals("Invalid integer for argument amount: bad", resolver.parseArguments(requiredInt, List.of("bad"), input)
            .failure().orElseThrow());
        assertEquals("Unexpected argument: extra", resolver.parseArguments(optionalInt, List.of("1", "extra"), input)
            .failure().orElseThrow());
        assertEquals("greedy arguments cannot appear before subcommands: reason",
            resolver.parseArgumentPrefix(greedy, List.of("hello"), input).failure().orElseThrow());
        assertEquals("greedy arguments cannot appear before subcommands: reason",
            resolver.parseArgumentPrefix(optionalGreedy, List.of("hello"), input).failure().orElseThrow());
        assertEquals("Missing required argument: amount", resolver.parseArgumentPrefix(requiredInt, List.of(), input)
            .failure().orElseThrow());
        assertEquals(0, resolver.parseArgumentPrefix(optionalInt, List.of(), input).consumed());
        assertEquals("Invalid integer for argument amount: bad", resolver.parseArgumentPrefix(requiredInt, List.of("bad"), input)
            .failure().orElseThrow());
        assertEquals(1, resolver.parseArgumentPrefix(requiredInt, List.of("5"), input).consumed());
    }

    @Test
    void routePatternParserCoversEveryRuntimeElementConversion() {
        RoutePattern pattern = RoutePatternParser.parse("root <required:String> [optional:String] <tail:String...> [rest:String...] [--silent|-s] [--amount:Integer|-a]");

        assertEquals("root", pattern.rootLiteral());
        assertEquals(6, pattern.steps().stream().filter(ElementRouteStep.class::isInstance).count());

        SimpleCommandRegistry registry = new SimpleCommandRegistry();
        registry.route("required <target:String>").executes(ctx -> dev.riege.buildmycommand.api.Results.silent());
        registry.route("optional [target:String]").executes(ctx -> dev.riege.buildmycommand.api.Results.silent());
        registry.route("greedy <tail:String...>").executes(ctx -> dev.riege.buildmycommand.api.Results.silent());
        registry.route("optionalGreedy [tail:String...]").executes(ctx -> dev.riege.buildmycommand.api.Results.silent());
        registry.route("options [--silent|-s] [--amount:Integer|-a]").executes(ctx -> dev.riege.buildmycommand.api.Results.silent());

        assertEquals(5, registry.roots().size());
    }

    private static ArgumentParseContext context(String name, Class<?> type, String raw) {
        CommandInput input = CommandInput.raw(new CommandSource() {
        }, raw);
        return new ArgumentParseContext(
            input.source(),
            input,
            name,
            type,
            raw,
            0,
            raw.length(),
            SuggestionType.ARGUMENT
        );
    }

    private enum Mode {
        SURVIVAL,
        CREATIVE
    }

    private static final class BrokenValue {
    }
}
