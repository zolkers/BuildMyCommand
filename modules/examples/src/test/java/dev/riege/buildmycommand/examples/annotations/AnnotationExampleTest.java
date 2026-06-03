package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnotationExampleTest {
    @Test
    void deepAnnotationSubRouteExampleDispatchesNestedRoutes() {
        CommandResult punishment = DeepAnnotationNestingExample.dispatch(
            "admin moderation punish temporary add Alex griefing --duration 30 --silent"
        );
        CommandResult appeal = DeepAnnotationNestingExample.dispatch("admin moderation appeal approve Alex");

        assertEquals(CommandResult.Status.SUCCESS, punishment.status());
        assertEquals(Optional.of("Temporary punishment added for Alex: griefing duration=30 silent=true"),
            punishment.reply());
        assertEquals(CommandResult.Status.SUCCESS, appeal.status());
        assertEquals(Optional.of("Appeal approved for Alex"), appeal.reply());
    }

    @Test
    void deepAnnotationSubRouteExampleSuggestsDeepRouteBranches() {
        List<String> suggestions = DeepAnnotationNestingExample.create().suggest(new CommandSource() {
        }, "admin moderation punish ", 24);

        assertEquals(List.of("permanent", "perm", "temporary", "temp"), suggestions);
    }
}
