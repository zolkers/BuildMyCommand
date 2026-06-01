package dev.buildmycommand.testkit;

import dev.buildmycommand.api.CommandSource;
import dev.buildmycommand.api.Results;
import dev.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CommandTestKitTest {
    @Test
    void assertsSuccessfulDispatch() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("ping", command -> command.executes(ctx -> Results.success("Pong")));

        CommandTestKit kit = CommandTestKit.create(framework, new CommandSource() {
        });

        assertDoesNotThrow(() -> kit.dispatch("ping")
            .assertSuccess()
            .assertMessageContains("Pong"));
    }
}
