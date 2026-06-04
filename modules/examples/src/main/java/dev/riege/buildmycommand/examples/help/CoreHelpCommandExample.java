/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.help;

import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandMetadata;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.PermissionSpec;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CoreHelpCommandExample {
    private CoreHelpCommandExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();

        registerApplicationCommands(framework);
        registerHelpCommand(framework);
        return framework;
    }

    public static CommandResult dispatch(CommandSource source, String input) {
        return create().dispatch(source, input);
    }

    public static List<String> suggest(CommandSource source, String input, int cursor) {
        return create().suggest(source, input, cursor);
    }

    private static void registerApplicationCommands(CommandFramework framework) {
        framework.registry()
            .route("profile view <target:String>")
            .description("Open a player's profile card")
            .usage("/profile view <player>")
            .example("/profile view Ada")
            .group("Players")
            .argumentSuggestions("target", "online players", ctx -> onlinePlayers(ctx.rawToken()))
            .executes(ctx -> Results.success("Profile for " + ctx.arg("target", String.class)));

        framework.registry()
            .route("profile message <target:String> <message:String...> [--silent|-s]")
            .description("Send a private profile note")
            .usage("/profile message <player> <message> [--silent]")
            .example("/profile message Ada Welcome back -s")
            .group("Players")
            .argumentSuggestions("target", "online players", ctx -> onlinePlayers(ctx.rawToken()))
            .executes(ctx -> Results.success(
                "Message to "
                    + ctx.arg("target", String.class)
                    + " silent="
                    + ctx.flag("silent")
                    + ": "
                    + ctx.arg("message", String.class)));

        framework.registry()
            .route("party invite <target:String>")
            .description("Invite a player to your party")
            .usage("/party invite <player>")
            .example("/party invite Alex")
            .group("Social")
            .argumentSuggestions("target", "online players", ctx -> onlinePlayers(ctx.rawToken()))
            .executes(ctx -> Results.success("Invite sent to " + ctx.arg("target", String.class)));

        framework.registry()
            .route("admin reload [area:String]")
            .description("Reload a server subsystem")
            .permission("admin.reload")
            .usage("/admin reload [area]")
            .example("/admin reload commands")
            .group("Administration")
            .argumentSuggestions("area", "reload areas", ctx -> List.of("commands", "permissions", "cache"))
            .executes(ctx -> Results.success("Reloaded " + ctx.optionalArg("area", String.class).orElse("all")));

        framework.registry()
            .route("admin audit player <target:String>")
            .description("Read player audit information")
            .permissionRegex("admin\\.audit\\..*")
            .usage("/admin audit player <player>")
            .example("/admin audit player Ada")
            .group("Administration")
            .argumentSuggestions("target", "online players", ctx -> onlinePlayers(ctx.rawToken()))
            .executes(ctx -> Results.success("Audit for " + ctx.arg("target", String.class)));

        framework.registry()
            .route("staff notes list")
            .description("List staff notes")
            .requirement("staff.notes")
            .usage("/staff notes list")
            .example("/staff notes list")
            .group("Staff")
            .executes(ctx -> Results.success("Staff notes"));

        framework.registry()
            .route("internal diagnostics dump")
            .description("Hidden internal support command")
            .hidden()
            .executes(ctx -> Results.success("diagnostics"));
    }

    private static void registerHelpCommand(CommandFramework framework) {
        framework.registry()
            .route("help|h [query:String...]")
            .description("Show visible commands or inspect one command")
            .usage("/help [command]")
            .example("/help")
            .example("/help profile message")
            .group("System")
            .argumentSuggestions("query", "visible commands",
                ctx -> helpSuggestions(framework, ctx.source(), ctx.rawToken()))
            .executes(ctx -> renderHelp(framework, ctx));
    }

    private static CommandResult renderHelp(CommandFramework framework, CommandContext ctx) {
        String query = ctx.optionalArg("query", String.class)
            .map(String::trim)
            .orElse("");
        if (!query.isBlank()) {
            String details = framework.help(ctx.source(), query);
            return Results.success(header("Command Help") + "\n" + details);
        }

        List<HelpEntry> entries = visibleEntries(framework.graph(), ctx.source());
        Map<String, List<HelpEntry>> byGroup = entries.stream()
            .sorted(Comparator.comparing(HelpEntry::group).thenComparing(HelpEntry::path))
            .collect(Collectors.groupingBy(
                HelpEntry::group,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        StringBuilder builder = new StringBuilder(header("Command Help"));
        byGroup.forEach((group, groupEntries) -> {
            builder.append("\n\n").append(group);
            for (HelpEntry entry : groupEntries) {
                builder.append("\n  ")
                    .append(entry.path())
                    .append(" - ")
                    .append(entry.description());
                if (!entry.aliases().isEmpty()) {
                    builder.append(" (aliases: ").append(String.join(", ", entry.aliases())).append(")");
                }
            }
        });
        builder.append("\n\nUse /help <command> for usage, examples, arguments, options, and permissions.");
        return Results.success(builder.toString());
    }

    private static List<String> helpSuggestions(CommandFramework framework, CommandSource source, String currentToken) {
        String normalizedToken = currentToken.toLowerCase();
        return visibleEntries(framework.graph(), source).stream()
            .map(HelpEntry::path)
            .filter(path -> path.toLowerCase().startsWith(normalizedToken))
            .toList();
    }

    private static List<HelpEntry> visibleEntries(CommandGraph graph, CommandSource source) {
        List<HelpEntry> entries = new ArrayList<>();
        for (CommandNode root : graph.roots()) {
            collectVisibleEntries(source, List.of(root), new ArrayList<>(), entries);
        }
        return entries;
    }

    private static void collectVisibleEntries(
        CommandSource source,
        List<CommandNode> lineage,
        List<String> path,
        List<HelpEntry> entries
    ) {
        CommandNode node = lineage.get(lineage.size() - 1);
        if (node.metadata().hidden() || denied(source, lineage).isPresent()) {
            return;
        }

        List<String> currentPath = new ArrayList<>(path);
        currentPath.add(node.literal());
        if (node.executor().isPresent()) {
            entries.add(new HelpEntry(
                String.join(" ", currentPath),
                node.description().orElse("No description"),
                node.metadata().group().orElse("Other"),
                node.aliases()
            ));
        }

        for (CommandNode child : node.children()) {
            List<CommandNode> childLineage = new ArrayList<>(lineage);
            childLineage.add(child);
            collectVisibleEntries(source, childLineage, currentPath, entries);
        }
    }

    private static Optional<String> denied(CommandSource source, List<CommandNode> lineage) {
        for (CommandNode node : lineage) {
            Optional<PermissionSpec> permission = node.permissionSpec();
            if (permission.isPresent() && !hasPermission(source, permission.get())) {
                return Optional.of(permission.get().display());
            }
            Optional<String> requirement = node.metadata().requirement();
            if (requirement.isPresent() && !source.hasPermission(requirement.get())) {
                return requirement;
            }
        }
        return Optional.empty();
    }

    private static boolean hasPermission(CommandSource source, PermissionSpec permission) {
        if (permission.regex()) {
            return source.hasPermissionMatching(Pattern.compile(permission.value()));
        }
        return source.hasPermission(permission.value());
    }

    private static List<String> onlinePlayers(String currentToken) {
        String normalizedToken = currentToken.toLowerCase();
        return List.of("Ada", "Alex", "Linus", "Grace").stream()
            .filter(player -> player.toLowerCase().startsWith(normalizedToken))
            .toList();
    }

    private static String header(String title) {
        return "== " + title + " ==";
    }

    private record HelpEntry(String path, String description, String group, List<String> aliases) {
        private HelpEntry {
            aliases = List.copyOf(aliases);
        }
    }

    public static final class ExampleSource implements CommandSource {
        private final String name;
        private final Set<String> permissions;

        public ExampleSource(String name, Set<String> permissions) {
            this.name = name;
            this.permissions = Set.copyOf(permissions);
        }

        @Override
        public Optional<String> name() {
            return Optional.of(name);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permission == null || permission.isBlank() || permissions.contains(permission);
        }

        @Override
        public boolean hasPermissionMatching(Pattern pattern) {
            return permissions.stream().anyMatch(permission -> pattern.matcher(permission).matches());
        }
    }
}
