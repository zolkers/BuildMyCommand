package dev.riege.buildmycommand.core;


import dev.riege.buildmycommand.core.dispatch.CommandDispatcher;
import dev.riege.buildmycommand.core.help.HelpGenerator;
import dev.riege.buildmycommand.core.help.SchemaExporter;
import dev.riege.buildmycommand.core.help.SuggestionEngine;
import dev.riege.buildmycommand.core.parse.ArgumentParserRegistry;
import dev.riege.buildmycommand.core.parse.ArgumentResolver;
import dev.riege.buildmycommand.core.parse.CommandTokenizer;
import dev.riege.buildmycommand.core.parse.OptionParser;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.List;
import java.util.Objects;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;
    private final CommandDispatcher dispatcher;
    private final SuggestionEngine suggestions;
    private final HelpGenerator help;
    private final SchemaExporter schema;

    private CommandFramework(SimpleCommandRegistry registry) {
        CommandTokenizer tokenizer = new CommandTokenizer();
        ArgumentParserRegistry parsers = new ArgumentParserRegistry();
        ArgumentResolver argumentResolver = new ArgumentResolver(parsers);
        OptionParser optionParser = new OptionParser(parsers);

        this.registry = registry;
        this.dispatcher = new CommandDispatcher(registry, tokenizer, optionParser, argumentResolver);
        this.suggestions = new SuggestionEngine(registry, tokenizer);
        this.help = new HelpGenerator(registry, tokenizer);
        this.schema = new SchemaExporter();
    }

    public static CommandFramework create() {
        return new CommandFramework(new SimpleCommandRegistry());
    }

    public CommandRegistry registry() {
        return registry;
    }

    public List<String> rootLiterals() {
        return registry.roots().stream()
            .map(command -> command.literal())
            .toList();
    }

    public String help(String path) {
        return help(new CommandSource() {
        }, path);
    }

    public String help(CommandSource source, String path) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(path, "path");
        return help.help(source, path);
    }

    public String schema() {
        return schema.schema(registry);
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return dispatcher.dispatch(source, input);
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return suggestions.suggest(source, input, cursor);
    }
}
