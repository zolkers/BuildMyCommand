/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Commands;
import dev.riege.buildmycommand.api.Results;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpProviderAPITest {
    @Test
    void rendersDetailsListsPaginationFiltersAndSuggestionsFromCommandGraph() {
        HelpProviderAPI help = help();
        CommandSource guest = source(Set.of());
        CommandSource admin = source(Set.of("admin.reload", "admin.audit.read", "staff.notes"));

        String publicHelp = help.render(guest, "", HelpOptions.defaults());
        assertTrue(publicHelp.contains("== Command Help == page 1/1"));
        assertTrue(publicHelp.contains("/profile view - Open profile"));
        assertTrue(publicHelp.contains("/help - Show visible commands or inspect one command (aliases: h)"));
        assertEquals(false, publicHelp.contains("admin reload"));
        assertEquals(false, publicHelp.contains("secret"));
        assertEquals(false, publicHelp.contains("staff notes"));

        String detail = help.render(guest, "profile view", HelpOptions.defaults());
        assertTrue(detail.contains("Usage: /profile view <target>"));
        assertTrue(detail.contains("Description: profile view"));

        String filtered = help.render(admin, "admin", HelpOptions.defaults()
            .toBuilder()
            .alphabetic(true)
            .pageSize(1)
            .page(2)
            .build());
        assertTrue(filtered.contains("page 2/2 alphabetic"));
        assertTrue(filtered.contains("/admin reload - Reload subsystem"));

        String grouped = help.render(admin, "", HelpOptions.defaults()
            .toBuilder()
            .group("Administration")
            .build());
        assertTrue(grouped.contains("group Administration"));
        assertTrue(grouped.contains("/admin audit player - Read audit"));
        assertEquals(false, grouped.contains("/profile view"));

        String empty = help.render(guest, "", HelpOptions.defaults()
            .toBuilder()
            .group("Missing")
            .build());
        assertEquals("""
            == Command Help == page 1/1 group Missing
            No commands are available.""", empty);

        assertEquals(List.of("admin", "help", "profile", "staff"), help.suggest(admin, ""));
        assertEquals(List.of("admin"), help.suggest(admin, "admin"));
        assertEquals(List.of("audit", "reload"), help.suggest(admin, "admin "));
        assertEquals(List.of("player"), help.suggest(admin, "admin audit "));
        assertEquals(List.of("admin audit player", "admin reload"), help.suggestPaths(admin, "admin"));
        assertEquals(List.of("Administration", "Players", "Staff", "System"), help.suggestGroups(admin, ""));
        assertEquals(List.of("Administration"), help.suggestGroups(admin, "Adm"));
        assertEquals("profile view", help.entries(guest).get(0).path());
        assertEquals(Optional.empty(), help.entries(guest).get(0).permission());
    }

    @Test
    void supportsCustomFormatterTitleFooterAndPageBuilding() {
        CommandSource guest = source(Set.of());
        String details = HelpProviderAPI.create(HelpProviderAPITest::graph, HelpProviderAPITest::details)
            .title("Docs")
            .footer("Done")
            .formatter(new HelpFormatter() {
                @Override
                public String formatDetails(String title, String details) {
                    return title + " -> " + details.lines().findFirst().orElseThrow();
                }

                @Override
                public String formatPage(HelpPage page) {
                    return page.title()
                        + " has "
                        + page.entries().size()
                        + " entries, footer="
                        + page.footer();
                }
            })
            .render(guest, "profile view", HelpOptions.defaults());
        assertEquals("Docs -> Usage: /profile view <target>", details);

        String page = HelpProviderAPI.of(HelpProviderAPITest::graph, HelpProviderAPITest::details)
            .title("Docs")
            .footer("Done")
            .formatter(new HelpFormatter() {
                @Override
                public String formatPage(HelpPage page) {
                    return page.title()
                        + " has "
                        + page.entries().size()
                        + " entries, footer="
                        + page.footer();
                }
            })
            .render(guest, "", HelpOptions.defaults());
        assertEquals("Docs has 2 entries, footer=Done", page);
    }

    @Test
    void optionsValidateAndNormalizeInputs() {
        CommandSource source = source(Set.of());
        HelpOptions parsed = HelpOptions.from(new CommandContext(source, "help", Map.of(
            "page", 3,
            "size", 12,
            "alphabetic", true,
            "group", "Players"
        )));
        assertEquals(3, parsed.page());
        assertEquals(12, parsed.pageSize());
        assertEquals(true, parsed.alphabetic());
        assertEquals(Optional.of("Players"), parsed.group());

        assertEquals(HelpProviderAPI.DEFAULT_ROUTE,
            "help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]");
        assertEquals(Optional.empty(), HelpOptions.defaults().toBuilder().group(" ").build().group());
        assertEquals("", HelpOptions.defaults().toBuilder().commandPrefix("").build().commandPrefix());
        assertThrows(IllegalArgumentException.class, () -> HelpOptions.defaults().toBuilder().page(0).build());
        assertThrows(IllegalArgumentException.class, () -> HelpOptions.defaults().toBuilder().pageSize(0).build());
        assertThrows(NullPointerException.class, () -> HelpOptions.from(null));
        assertThrows(NullPointerException.class, () -> HelpProviderAPI.create(null, HelpProviderAPITest::details));
        assertThrows(NullPointerException.class, () -> HelpProviderAPI.create(HelpProviderAPITest::graph, null));
        assertThrows(IllegalArgumentException.class, () -> help().title(" "));
        assertThrows(IllegalArgumentException.class, () -> help().footer(" "));
        assertThrows(NullPointerException.class, () -> help().formatter(null));
        assertThrows(NullPointerException.class, () -> help().render(null, "", HelpOptions.defaults()));
        assertThrows(NullPointerException.class, () -> help().render(source(Set.of()), null, HelpOptions.defaults()));
        assertThrows(NullPointerException.class, () -> help().render(source(Set.of()), "", null));
        assertThrows(NullPointerException.class, () -> help().entries(null));
        assertThrows(NullPointerException.class, () -> help().entries(source(Set.of()), null));
        assertThrows(NullPointerException.class, () -> help().suggest(source(Set.of()), null));
        assertThrows(NullPointerException.class, () -> help().suggestGroups(source(Set.of()), null));
        assertThrows(NullPointerException.class, () -> help().page(null, HelpOptions.defaults()));
        assertThrows(NullPointerException.class, () -> help().page(List.of(), null));
        assertThrows(NullPointerException.class,
            () -> HelpProviderAPI.create(() -> null, HelpProviderAPITest::details).entries(source(Set.of())));
        assertThrows(IllegalArgumentException.class,
            () -> new HelpEntry("", "", "", List.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class,
            () -> new HelpPage("", "", List.of(), HelpOptions.defaults(), 0, 1));
        assertThrows(IllegalArgumentException.class,
            () -> new HelpPage("", "", List.of(), HelpOptions.defaults(), 1, 0));
    }

    private static HelpProviderAPI help() {
        return HelpProviderAPI.create(HelpProviderAPITest::graph, HelpProviderAPITest::details);
    }

    private static String details(CommandSource source, String path) {
        return "Usage: /" + path + " <target>\nDescription: " + path;
    }

    private static CommandGraph graph() {
        return new CommandGraph(List.of(
            Commands.literal("profile")
                .child(leaf("view", "Open profile", "Players").build())
                .build(),
            leaf("help", "Show visible commands or inspect one command", "System", "h").build(),
            Commands.literal("admin")
                .child(leaf("reload", "Reload subsystem", "Administration")
                    .permission("admin.reload")
                    .build())
                .child(Commands.literal("audit")
                    .child(leaf("player", "Read audit", "Administration")
                        .permissionRegex("admin\\.audit\\..*")
                        .build())
                    .build())
                .build(),
            leaf("staff", "Staff notes", "Staff")
                .metadata(metadata("Staff").requirement("staff.notes").build())
                .build(),
            leaf("secret", "Secret", "Other")
                .metadata(new CommandMetadata.Builder().hidden().build())
                .build()
        ));
    }

    private static CommandNode.Builder leaf(String literal, String description, String group, String... aliases) {
        CommandNode.Builder builder = Commands.literal(literal)
            .description(description)
            .metadata(metadata(group).build())
            .handler(ctx -> Results.success(literal));
        for (String alias : aliases) {
            builder.alias(alias);
        }
        return builder;
    }

    private static CommandMetadata.Builder metadata(String group) {
        return new CommandMetadata.Builder().group(group);
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
