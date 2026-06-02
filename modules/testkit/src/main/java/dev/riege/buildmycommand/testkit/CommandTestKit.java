package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Objects;

public final class CommandTestKit {
    private final CommandFramework framework;
    private final CommandSource source;
    private final CommandPlatform platform;

    private CommandTestKit(CommandFramework framework, CommandSource source, CommandPlatform platform) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.source = Objects.requireNonNull(source, "source");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    public static CommandTestKit create(CommandFramework framework, CommandSource source) {
        return new CommandTestKit(framework, source, CommandPlatform.test());
    }

    public static CommandTestKit create(CommandFramework framework) {
        return create(framework, TestCommandSource.create());
    }

    public CommandTestKit source(CommandSource source) {
        return new CommandTestKit(framework, source, platform);
    }

    public CommandTestKit platform(CommandPlatform platform) {
        return new CommandTestKit(framework, source, platform);
    }

    public DispatchAssert assertDispatch(String input) {
        return dispatch(input);
    }

    public DispatchAssert dispatch(String input) {
        Objects.requireNonNull(input, "input");
        return new DispatchAssert(framework.dispatch(new CommandInput(source, input, input.length(), "", platform)));
    }

    public SuggestionAssert assertSuggestions(String input) {
        Objects.requireNonNull(input, "input");
        return new SuggestionAssert(framework.suggest(source, input, input.length()));
    }

    public SchemaAssert assertSchema() {
        return new SchemaAssert(framework.schema());
    }

    public static final class DispatchAssert {
        private final CommandResult result;

        private DispatchAssert(CommandResult result) {
            this.result = Objects.requireNonNull(result, "result");
        }

        public DispatchAssert assertSuccess() {
            if (result.status() != CommandResult.Status.SUCCESS) {
                throw new AssertionError("Expected success but was " + result.status());
            }
            return this;
        }

        public DispatchAssert assertFailure() {
            if (result.status() != CommandResult.Status.FAILURE) {
                throw new AssertionError("Expected failure but was " + result.status());
            }
            return this;
        }

        public DispatchAssert succeedsWith(String reply) {
            assertSuccess();
            return assertReply(reply);
        }

        public DispatchAssert failsWith(String reply) {
            assertFailure();
            return assertReply(reply);
        }

        public DispatchAssert assertMessageContains(String text) {
            Objects.requireNonNull(text, "text");
            String reply = result.reply()
                .orElseThrow(() -> new AssertionError("Expected reply to contain '" + text + "' but reply was empty"));
            if (!reply.contains(text)) {
                throw new AssertionError("Expected reply to contain '" + text + "' but was '" + reply + "'");
            }
            return this;
        }

        public DispatchAssert assertReply(String expected) {
            Objects.requireNonNull(expected, "expected");
            String actual = result.reply()
                .orElseThrow(() -> new AssertionError("Expected reply '" + expected + "' but reply was empty"));
            if (!actual.equals(expected)) {
                throw new AssertionError("Expected reply '" + expected + "' but was '" + actual + "'");
            }
            return this;
        }
    }

    public static final class SuggestionAssert {
        private final List<String> suggestions;

        private SuggestionAssert(List<String> suggestions) {
            this.suggestions = List.copyOf(Objects.requireNonNull(suggestions, "suggestions"));
        }

        public SuggestionAssert contains(String suggestion) {
            Objects.requireNonNull(suggestion, "suggestion");
            if (!suggestions.contains(suggestion)) {
                throw new AssertionError("Expected suggestions to contain '" + suggestion + "' but were " + suggestions);
            }
            return this;
        }

        public SuggestionAssert doesNotContain(String suggestion) {
            Objects.requireNonNull(suggestion, "suggestion");
            if (suggestions.contains(suggestion)) {
                throw new AssertionError("Expected suggestions not to contain '" + suggestion + "' but were " + suggestions);
            }
            return this;
        }

        public List<String> values() {
            return suggestions;
        }
    }

    public static final class SchemaAssert {
        private final String schema;

        private SchemaAssert(String schema) {
            this.schema = Objects.requireNonNull(schema, "schema");
        }

        public SchemaAssert containsCommand(String literal) {
            Objects.requireNonNull(literal, "literal");
            String expected = "command " + literal;
            if (!schema.contains(expected)) {
                throw new AssertionError("Expected schema to contain '" + expected + "' but was:\n" + schema);
            }
            return this;
        }

        public SchemaAssert contains(String text) {
            Objects.requireNonNull(text, "text");
            if (!schema.contains(text)) {
                throw new AssertionError("Expected schema to contain '" + text + "' but was:\n" + schema);
            }
            return this;
        }

        public String value() {
            return schema;
        }
    }
}
