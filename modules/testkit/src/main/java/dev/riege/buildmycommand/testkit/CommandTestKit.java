package dev.riege.buildmycommand.testkit;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Objects;

public final class CommandTestKit {
    private final CommandFramework framework;
    private final CommandSource source;

    private CommandTestKit(CommandFramework framework, CommandSource source) {
        this.framework = Objects.requireNonNull(framework, "framework");
        this.source = Objects.requireNonNull(source, "source");
    }

    public static CommandTestKit create(CommandFramework framework, CommandSource source) {
        return new CommandTestKit(framework, source);
    }

    public DispatchAssert dispatch(String input) {
        return new DispatchAssert(framework.dispatch(source, input));
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

        public DispatchAssert assertMessageContains(String text) {
            Objects.requireNonNull(text, "text");
            String reply = result.reply()
                .orElseThrow(() -> new AssertionError("Expected reply to contain '" + text + "' but reply was empty"));
            if (!reply.contains(text)) {
                throw new AssertionError("Expected reply to contain '" + text + "' but was '" + reply + "'");
            }
            return this;
        }
    }
}
