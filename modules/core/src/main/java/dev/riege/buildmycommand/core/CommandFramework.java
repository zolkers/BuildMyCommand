package dev.riege.buildmycommand.core;


import dev.riege.buildmycommand.core.dispatch.CommandDispatcher;
import dev.riege.buildmycommand.core.help.HelpGenerator;
import dev.riege.buildmycommand.core.help.SchemaExporter;
import dev.riege.buildmycommand.core.help.SuggestionEngine;
import dev.riege.buildmycommand.core.parse.ArgumentParserRegistry;
import dev.riege.buildmycommand.core.parse.ArgumentResolver;
import dev.riege.buildmycommand.core.parse.CommandTokenizer;
import dev.riege.buildmycommand.core.parse.OptionParser;
import dev.riege.buildmycommand.core.registry.ManualCommandImporter;
import dev.riege.buildmycommand.core.registry.SimpleCommandRegistry;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Suggestion;

import java.util.List;
import java.util.Objects;

public final class CommandFramework {
    private final SimpleCommandRegistry registry;
    private final CommandDispatcher dispatcher;
    private final SuggestionEngine suggestions;
    private final HelpGenerator help;
    private final SchemaExporter schema;
    private final CommandMatchingPolicy matchingPolicy;

    private CommandFramework(SimpleCommandRegistry registry, CommandMatchingPolicy matchingPolicy) {
        CommandTokenizer tokenizer = new CommandTokenizer();
        ArgumentParserRegistry parsers = new ArgumentParserRegistry();
        ArgumentResolver argumentResolver = new ArgumentResolver(parsers);
        OptionParser optionParser = new OptionParser(parsers, matchingPolicy);

        this.registry = registry;
        this.matchingPolicy = matchingPolicy;
        this.dispatcher = new CommandDispatcher(registry, tokenizer, optionParser, argumentResolver, matchingPolicy);
        this.suggestions = new SuggestionEngine(registry, tokenizer, matchingPolicy);
        this.help = new HelpGenerator(registry, tokenizer);
        this.schema = new SchemaExporter();
    }

    public static CommandFramework create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public CommandRegistry registry() {
        return registry;
    }

    public List<String> rootLiterals() {
        return registry.roots().stream()
            .map(command -> command.literal())
            .toList();
    }

    public List<String> rootLabels() {
        return registry.roots().stream()
            .flatMap(command -> {
                List<String> labels = new java.util.ArrayList<>();
                labels.add(command.literal());
                labels.addAll(command.aliases());
                return labels.stream();
            })
            .distinct()
            .toList();
    }

    public boolean caseInsensitiveLiterals() {
        return matchingPolicy.caseInsensitiveLiterals();
    }

    public boolean caseInsensitiveOptions() {
        return matchingPolicy.caseInsensitiveOptions();
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

    public CommandGraph graph() {
        return new CommandGraph(registry.roots().stream()
            .map(ManualCommandImporter::exportNode)
            .toList());
    }

    public CommandResult dispatch(CommandInput input) {
        Objects.requireNonNull(input, "input");
        return dispatch(input.source(), input.normalizedInput());
    }

    public CommandResult dispatch(CommandSource source, String input) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return dispatcher.dispatch(source, input);
    }

    public List<Suggestion> suggestRich(CommandInput input) {
        Objects.requireNonNull(input, "input");
        return suggestions.suggestRich(input);
    }

    public List<String> suggest(CommandSource source, String input, int cursor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(input, "input");
        return suggestions.suggest(source, input, cursor);
    }

    public static final class Builder {
        private boolean caseInsensitiveLiterals;
        private boolean caseInsensitiveOptions;

        private Builder() {
        }

        public Builder caseInsensitiveLiterals() {
            caseInsensitiveLiterals = true;
            return this;
        }

        public Builder caseInsensitiveOptions() {
            caseInsensitiveOptions = true;
            return this;
        }

        public CommandFramework build() {
            CommandMatchingPolicy matchingPolicy =
                new CommandMatchingPolicy(caseInsensitiveLiterals, caseInsensitiveOptions);
            return new CommandFramework(new SimpleCommandRegistry(matchingPolicy), matchingPolicy);
        }
    }
}
