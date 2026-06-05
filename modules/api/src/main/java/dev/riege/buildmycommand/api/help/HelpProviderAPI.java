/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import dev.riege.buildmycommand.api.CommandGraph;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.PermissionSpec;
import dev.riege.buildmycommand.api.SuggestionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class HelpProviderAPI {
    public static final String DEFAULT_ROUTE =
        "help|h [query:String...] [--page:Integer|-p] [--size:Integer|-s] [--alphabetic|-a] [--group:String|-g]";

    private final Supplier<CommandGraph> graphSupplier;
    private final BiFunction<CommandSource, String, String> detailsProvider;
    private String title = "Command Help";
    private String footer = "Use /help <command> for usage, examples, arguments, options, and permissions.";
    private HelpFormatter formatter = HelpFormatter.DEFAULT;

    private HelpProviderAPI(
        Supplier<CommandGraph> graphSupplier,
        BiFunction<CommandSource, String, String> detailsProvider
    ) {
        this.graphSupplier = Objects.requireNonNull(graphSupplier, "graphSupplier");
        this.detailsProvider = Objects.requireNonNull(detailsProvider, "detailsProvider");
    }

    public static HelpProviderAPI create(
        Supplier<CommandGraph> graphSupplier,
        BiFunction<CommandSource, String, String> detailsProvider
    ) {
        return new HelpProviderAPI(graphSupplier, detailsProvider);
    }

    public static HelpProviderAPI of(
        Supplier<CommandGraph> graphSupplier,
        BiFunction<CommandSource, String, String> detailsProvider
    ) {
        return create(graphSupplier, detailsProvider);
    }

    public HelpProviderAPI title(String title) {
        this.title = metadata(title, "title");
        return this;
    }

    public HelpProviderAPI footer(String footer) {
        this.footer = metadata(footer, "footer");
        return this;
    }

    public HelpProviderAPI formatter(HelpFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        return this;
    }

    public String render(CommandSource source, String query, HelpOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(options, "options");
        HelpResolution resolution = resolve(source, query, options);
        return resolution.details()
            .map(details -> formatter.formatDetails(title, details))
            .orElseGet(() -> formatter.formatPage(resolution.page().orElseThrow()));
    }

    public HelpResolution resolve(CommandSource source, String query, HelpOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(options, "options");
        String trimmedQuery = query.trim();
        if (!trimmedQuery.isBlank() && hasVisibleEntry(source, trimmedQuery)) {
            return HelpResolution.details(detailsProvider.apply(source, trimmedQuery));
        }
        return HelpResolution.page(page(source, query, options));
    }

    public List<HelpEntry> entries(CommandSource source) {
        return entries(source, HelpOptions.defaults());
    }

    public List<HelpEntry> entries(CommandSource source, HelpOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(options, "options");
        List<HelpEntry> entries = visibleEntries(graphSupplier.get(), source).stream()
            .filter(entry -> options.group()
                .map(group -> entry.group().equalsIgnoreCase(group))
                .orElse(true))
            .toList();
        if (options.alphabetic()) {
            return entries.stream()
                .sorted(Comparator.comparing(HelpEntry::path, String.CASE_INSENSITIVE_ORDER))
                .toList();
        }
        return entries;
    }

    public List<String> suggest(CommandSource source, String currentToken) {
        Objects.requireNonNull(currentToken, "currentToken");
        return suggest(source, HelpQuery.parse(currentToken), HelpSuggestionMode.SEGMENT);
    }

    public List<String> suggest(SuggestionContext context) {
        return suggest(context, HelpSuggestionMode.SEGMENT);
    }

    public List<String> suggest(SuggestionContext context, HelpSuggestionMode mode) {
        Objects.requireNonNull(context, "context");
        return suggest(context.source(), context.helpQuery(), mode);
    }

    public List<String> suggest(CommandSource source, HelpQuery query, HelpSuggestionMode mode) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case SEGMENT -> suggestSegments(source, query);
            case PATH -> suggestPaths(source, query.prefix());
            case SMART -> {
                List<String> segments = suggestSegments(source, query);
                yield segments.isEmpty() ? suggestPaths(source, query.prefix()) : segments;
            }
        };
    }

    public List<String> suggestPaths(CommandSource source, String currentToken) {
        Objects.requireNonNull(currentToken, "currentToken");
        String normalizedToken = currentToken.toLowerCase();
        return entries(source, HelpOptions.defaults().toBuilder().alphabetic(true).build()).stream()
            .map(HelpEntry::path)
            .filter(path -> path.toLowerCase().startsWith(normalizedToken))
            .toList();
    }

    public List<String> suggestGroups(CommandSource source, String currentToken) {
        Objects.requireNonNull(currentToken, "currentToken");
        String normalizedToken = currentToken.toLowerCase();
        return entries(source).stream()
            .map(HelpEntry::group)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .filter(group -> group.toLowerCase().startsWith(normalizedToken))
            .toList();
    }

    public HelpPage page(List<HelpEntry> entries, HelpOptions options) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(options, "options");
        int pageCount = Math.max(1, (int) Math.ceil(entries.size() / (double) options.pageSize()));
        int page = Math.min(options.page(), pageCount);
        int from = Math.min((page - 1) * options.pageSize(), entries.size());
        int to = Math.min(from + options.pageSize(), entries.size());
        List<HelpEntry> pageEntries = entries.subList(from, to);
        return new HelpPage(title, footer, pageEntries, options, page, pageCount);
    }

    public HelpPage page(CommandSource source, String query, HelpOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(options, "options");
        String trimmedQuery = query.trim();
        List<HelpEntry> entries = entries(source, options).stream()
            .filter(entry -> trimmedQuery.isBlank() || entry.path().toLowerCase().contains(trimmedQuery.toLowerCase()))
            .toList();
        return page(entries, options);
    }

    public String details(CommandSource source, String path) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(path, "path");
        String trimmedPath = path.trim();
        if (trimmedPath.isBlank() || !hasVisibleEntry(source, trimmedPath)) {
            throw new IllegalArgumentException("unknown visible help path: " + path);
        }
        return detailsProvider.apply(source, trimmedPath);
    }

    private boolean hasVisibleEntry(CommandSource source, String path) {
        return entries(source).stream().anyMatch(entry -> entry.path().equalsIgnoreCase(path));
    }

    private List<String> suggestSegments(CommandSource source, HelpQuery query) {
        return entries(source, HelpOptions.defaults().toBuilder().alphabetic(true).build()).stream()
            .map(HelpEntry::path)
            .flatMap(path -> nextSegments(path, query.prefixTokens(), query.currentToken()).stream())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private static List<String> nextSegments(String path, List<String> prefixTokens, String currentSegment) {
        List<String> pathTokens = tokens(path);
        String normalizedSegment = currentSegment.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        int maxOffset = prefixTokens.isEmpty() && currentSegment.isEmpty() ? 0 : pathTokens.size() - 1;
        for (int offset = 0; offset <= maxOffset && offset + prefixTokens.size() < pathTokens.size(); offset++) {
            if (matchesAt(pathTokens, prefixTokens, offset)) {
                String candidate = pathTokens.get(offset + prefixTokens.size());
                if (candidate.toLowerCase().startsWith(normalizedSegment)) {
                    suggestions.add(candidate);
                }
            }
        }
        return suggestions;
    }

    private static boolean matchesAt(List<String> pathTokens, List<String> prefixTokens, int offset) {
        for (int index = 0; index < prefixTokens.size(); index++) {
            if (!pathTokens.get(offset + index).equalsIgnoreCase(prefixTokens.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> tokens(String input) {
        return List.of(input.trim().split("\\s+"));
    }

    private List<HelpEntry> visibleEntries(CommandGraph graph, CommandSource source) {
        Objects.requireNonNull(graph, "graph");
        List<HelpEntry> entries = new ArrayList<>();
        for (CommandNode root : graph.roots()) {
            collectVisibleEntries(source, List.of(root), new ArrayList<>(), entries);
        }
        return entries;
    }

    private void collectVisibleEntries(
        CommandSource source,
        List<CommandNode> lineage,
        List<String> path,
        List<HelpEntry> entries
    ) {
        CommandNode node = lineage.getLast();
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
                node.aliases(),
                node.permissionSpec().map(PermissionSpec::display)
            ));
        }

        for (CommandNode child : node.children()) {
            List<CommandNode> childLineage = new ArrayList<>(lineage);
            childLineage.add(child);
            collectVisibleEntries(source, childLineage, currentPath, entries);
        }
    }

    private Optional<String> denied(CommandSource source, List<CommandNode> lineage) {
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

    private static String metadata(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
