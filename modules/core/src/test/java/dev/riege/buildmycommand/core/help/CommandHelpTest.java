/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.help;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHelpTest {
    @Test
    void rendersDetailListPaginationFilteringAndSuggestions() {
        CommandFramework framework = framework();
        CommandHelp help = CommandHelp.forFramework(framework);
        help.register();
        CommandSource guest = source(Set.of());
        CommandSource admin = source(Set.of("admin.reload", "admin.audit.read", "staff.notes"));

        String publicHelp = help.render(guest, "", CommandHelpOptions.defaults());
        assertTrue(publicHelp.contains("== Command Help == page 1/1"));
        assertTrue(publicHelp.contains("/profile view - Open profile"));
        assertTrue(publicHelp.contains("/help - Show visible commands or inspect one command (aliases: h)"));
        assertEquals(false, publicHelp.contains("admin reload"));
        assertEquals(false, publicHelp.contains("secret"));
        assertEquals(false, publicHelp.contains("staff notes"));

        String detail = help.render(guest, "profile view", CommandHelpOptions.defaults());
        assertTrue(detail.contains("Usage: /profile view <player>"));
        assertTrue(detail.contains("Description: Open profile"));

        String filtered = help.render(admin, "admin", CommandHelpOptions.defaults()
            .toBuilder()
            .alphabetic(true)
            .pageSize(1)
            .page(2)
            .build());
        assertTrue(filtered.contains("page 2/2 alphabetic"));
        assertTrue(filtered.contains("/admin reload - Reload subsystem"));

        String grouped = help.render(admin, "", CommandHelpOptions.defaults()
            .toBuilder()
            .group("Administration")
            .build());
        assertTrue(grouped.contains("group Administration"));
        assertTrue(grouped.contains("/admin audit player - Read audit"));
        assertEquals(false, grouped.contains("/profile view"));

        String empty = help.render(guest, "", CommandHelpOptions.defaults()
            .toBuilder()
            .group("Missing")
            .build());
        assertEquals("""
            == Command Help == page 1/1 group Missing
            No commands are available.""", empty);

        assertEquals(List.of("admin audit player", "admin reload"), help.suggest(admin, "admin"));
        assertEquals(List.of("Administration", "Players", "Staff", "System"), help.suggestGroups(admin, ""));
        assertEquals(List.of("Administration"), help.suggestGroups(admin, "Adm"));
        assertEquals("profile view", help.entries(guest).get(0).path());
        assertEquals(Optional.empty(), help.entries(guest).get(0).permission());

        String custom = CommandHelp.forFramework(framework)
            .formatter(new CommandHelpFormatter() {
                @Override
                public String formatDetails(String title, String details) {
                    return title + " -> " + details.lines().findFirst().orElseThrow();
                }

                @Override
                public String formatPage(CommandHelpPage page) {
                    return page.title() + " has " + page.entries().size() + " entries";
                }
            })
            .render(guest, "profile view", CommandHelpOptions.defaults());
        assertEquals("Command Help -> Usage: /profile view <player>", custom);
        assertEquals("Command Help has 2 entries", CommandHelp.forFramework(framework)
            .formatter(new CommandHelpFormatter() {
                @Override
                public String formatPage(CommandHelpPage page) {
                    return page.title() + " has " + page.entries().size() + " entries";
                }
            })
            .render(guest, "", CommandHelpOptions.defaults()));
    }

    @Test
    void registeredHelpCommandParsesFlagsAndOptions() {
        CommandFramework framework = framework();
        CommandHelp.forFramework(framework).title("Docs").footer("Done").register();
        CommandSource admin = source(Set.of("admin.reload", "admin.audit.read", "staff.notes"));

        CommandResult result = framework.dispatch(admin, "help --group Administration --alphabetic --size 1 --page 2");
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        String reply = result.reply().orElseThrow();
        assertTrue(reply.contains("== Docs == page 2/2 group Administration alphabetic"));
        assertTrue(reply.contains("/admin reload - Reload subsystem"));
        assertTrue(reply.contains("Done"));
        assertEquals(List.of("admin audit player", "admin reload"), framework.suggest(admin, "help admin", 10));
        assertEquals(List.of("Administration"), framework.suggest(admin, "help --group Adm", 16));
    }

    @Test
    void optionsValidateAndNormalizeInputs() {
        assertEquals(Optional.empty(), CommandHelpOptions.defaults().toBuilder().group(" ").build().group());
        assertEquals("", CommandHelpOptions.defaults().toBuilder().commandPrefix("").build().commandPrefix());
        assertThrows(IllegalArgumentException.class, () -> CommandHelpOptions.defaults().toBuilder().page(0).build());
        assertThrows(IllegalArgumentException.class,
            () -> CommandHelpOptions.defaults().toBuilder().pageSize(0).build());
        assertThrows(NullPointerException.class, () -> CommandHelp.forFramework(null));
        assertThrows(IllegalArgumentException.class, () -> CommandHelp.forFramework(framework()).title(" "));
        assertThrows(IllegalArgumentException.class, () -> CommandHelp.forFramework(framework()).footer(" "));
        assertThrows(NullPointerException.class, () -> CommandHelp.forFramework(framework()).formatter(null));
        assertThrows(IllegalArgumentException.class,
            () -> new CommandHelpEntry("", "", "", List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class,
            () -> new CommandHelpPage("", "", List.of(), CommandHelpOptions.defaults(), 0, 1));
        assertThrows(IllegalArgumentException.class,
            () -> new CommandHelpPage("", "", List.of(), CommandHelpOptions.defaults(), 1, 0));
    }

    private static CommandFramework framework() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("profile view <target:String>")
            .description("Open profile")
            .usage("/profile view <player>")
            .group("Players")
            .executes(ctx -> Results.success("profile"));
        framework.registry()
            .route("admin reload")
            .description("Reload subsystem")
            .permission("admin.reload")
            .group("Administration")
            .executes(ctx -> Results.success("reload"));
        framework.registry()
            .route("admin audit player")
            .description("Read audit")
            .permissionRegex("admin\\.audit\\..*")
            .group("Administration")
            .executes(ctx -> Results.success("audit"));
        framework.registry()
            .route("staff notes")
            .description("Staff notes")
            .requirement("staff.notes")
            .group("Staff")
            .executes(ctx -> Results.success("notes"));
        framework.registry()
            .route("secret")
            .hidden()
            .executes(ctx -> Results.success("secret"));
        return framework;
    }

    private static CommandSource source(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }

            @Override
            public boolean hasPermissionMatching(Pattern pattern) {
                return permissions.stream().anyMatch(permission -> pattern.matcher(permission).matches());
            }
        };
    }
}
