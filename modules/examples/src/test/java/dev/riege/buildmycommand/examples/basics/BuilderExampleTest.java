/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.basics;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuilderExampleTest {
    @Test
    void deepSubcommandNestingExampleDispatchesNestedCommands() {
        CommandResult punish = DeepSubcommandNestingExample.dispatch(
            "admin moderation punish temporary add Alex griefing --duration 30 --silent"
        );
        CommandResult appeal = DeepSubcommandNestingExample.dispatch("admin moderation appeal approve Alex");

        assertEquals(CommandResult.Status.SUCCESS, punish.status());
        assertEquals(Optional.of("Temporary punishment added for Alex: griefing duration=30 silent=true"), punish.reply());
        assertEquals(CommandResult.Status.SUCCESS, appeal.status());
        assertEquals(Optional.of("Appeal approved for Alex"), appeal.reply());
    }

    @Test
    void playerManagementBuilderExampleDispatchesBuilderAndRouteCommands() {
        CommandResult profile = PlayerManagementBuilderExample.dispatch("player profile view Alex");
        CommandResult give = PlayerManagementBuilderExample.dispatch("player inventory give Alex diamond 64 --silent");
        CommandResult economy = PlayerManagementBuilderExample.dispatch("player economy balance add Alex 250");

        assertEquals(CommandResult.Status.SUCCESS, profile.status());
        assertEquals(Optional.of("Profile for Alex"), profile.reply());
        assertEquals(CommandResult.Status.SUCCESS, give.status());
        assertEquals(Optional.of("Gave 64 diamond to Alex silent=true"), give.reply());
        assertEquals(CommandResult.Status.SUCCESS, economy.status());
        assertEquals(Optional.of("Added 250 coins to Alex"), economy.reply());
    }

    @Test
    void playerManagementBuilderExampleSuggestsNestedActions() {
        List<String> suggestions = PlayerManagementBuilderExample.create().suggest(new CommandSource() {
        }, "player moderation ", 18);

        assertEquals(List.of("warn", "mute", "ban", "history"), suggestions);
    }
}
