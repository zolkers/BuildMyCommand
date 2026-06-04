/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.annotation.help;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import dev.riege.buildmycommand.core.help.CommandHelp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotatedCommandHelpTest {
    @Test
    void registersAnnotatedHelpCommand() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("profile view")
            .description("Open profile")
            .group("Players")
            .executes(ctx -> Results.success("profile"));

        AnnotationCommandScanner.register(framework.registry(), new AnnotatedCommandHelp(CommandHelp.forFramework(framework)));

        CommandResult result = framework.dispatch(source(), "help --group Players");
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertTrue(result.reply().orElseThrow().contains("/profile view - Open profile"));
        assertEquals(List.of("profile view"), framework.suggest(source(), "help profile", 12));
        assertEquals(List.of("Players"), framework.suggest(source(), "help --group P", 14));
    }

    @Test
    void rejectsNullHelp() {
        assertThrows(NullPointerException.class, () -> new AnnotatedCommandHelp(null));
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
