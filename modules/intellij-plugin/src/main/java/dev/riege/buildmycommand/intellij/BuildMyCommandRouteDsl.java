/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BuildMyCommandRouteDsl {
    static final List<String> TYPES = List.of(
        "String",
        "Integer",
        "int",
        "Long",
        "long",
        "Double",
        "double",
        "Float",
        "float",
        "Boolean",
        "boolean",
        "UUID",
        "Duration",
        "LocalDate",
        "LocalDateTime",
        "Path",
        "URI",
        "URL"
    );

    private static final Set<String> TYPE_SET = Set.copyOf(TYPES);
    private static final Pattern ARGUMENT = Pattern.compile("^([A-Za-z_][A-Za-z0-9_-]*):([A-Za-z][A-Za-z0-9_]*)(\\.\\.\\.)?$");
    private static final Pattern OPTION = Pattern.compile("^--([A-Za-z][A-Za-z0-9_-]*)(?::([A-Za-z][A-Za-z0-9_.]*))?(?:\\|-([A-Za-z0-9]))?$");

    private BuildMyCommandRouteDsl() {
    }

    static List<Issue> validate(String route) {
        List<Issue> issues = new ArrayList<>();
        Set<String> aliases = new HashSet<>();
        boolean optionalArgumentSeen = false;
        int index = 0;
        for (String token : route.split("\\s+")) {
            if (token.isEmpty()) {
                index++;
                continue;
            }
            int start = route.indexOf(token, index);
            int end = start + token.length();
            index = end;

            if (token.startsWith("<") && token.endsWith(">")) {
                if (optionalArgumentSeen) {
                    issues.add(new Issue(start, end, "Required argument cannot follow an optional argument"));
                }
                validateArgument(token.substring(1, token.length() - 1), start + 1, issues);
                continue;
            }

            if (token.startsWith("[") && token.endsWith("]")) {
                String body = token.substring(1, token.length() - 1);
                if (body.startsWith("--")) {
                    validateOption(body, start + 1, aliases, issues);
                } else {
                    optionalArgumentSeen = true;
                    validateArgument(body, start + 1, issues);
                }
                continue;
            }

            validateLiteralAliases(token, start, aliases, issues);
        }
        return List.copyOf(issues);
    }

    static List<String> completionsFor(String route, int cursor) {
        String prefix = route.substring(0, Math.max(0, Math.min(cursor, route.length())));
        if (prefix.endsWith(":")) {
            return TYPES;
        }
        if (prefix.endsWith("|-")) {
            return List.of("a", "d", "f", "s", "v");
        }
        if (prefix.endsWith("[--")) {
            return List.of("amount", "duration", "silent", "force");
        }
        return List.of();
    }

    static Set<String> bindingNames(String route) {
        Set<String> names = new HashSet<>();
        for (String token : route.split("\\s+")) {
            if (token.startsWith("<") && token.endsWith(">")) {
                addArgumentName(token.substring(1, token.length() - 1), names);
            } else if (token.startsWith("[") && token.endsWith("]")) {
                String body = token.substring(1, token.length() - 1);
                if (body.startsWith("--")) {
                    addOptionName(body, names);
                } else {
                    addArgumentName(body, names);
                }
            }
        }
        return Set.copyOf(names);
    }

    private static void addArgumentName(String body, Set<String> names) {
        int separator = body.indexOf(':');
        String name = separator < 0 ? body : body.substring(0, separator);
        if (!name.isBlank()) {
            names.add(name);
        }
    }

    private static void addOptionName(String body, Set<String> names) {
        String withoutPrefix = body.substring(2);
        int end = withoutPrefix.length();
        int typeSeparator = withoutPrefix.indexOf(':');
        int aliasSeparator = withoutPrefix.indexOf('|');
        if (typeSeparator >= 0) {
            end = Math.min(end, typeSeparator);
        }
        if (aliasSeparator >= 0) {
            end = Math.min(end, aliasSeparator);
        }
        String name = withoutPrefix.substring(0, end);
        if (!name.isBlank()) {
            names.add(name);
        }
    }

    private static void validateArgument(String body, int offset, List<Issue> issues) {
        String greedyType = null;
        if (body.endsWith("...")) {
            int separator = body.indexOf(':');
            if (separator >= 0) {
                greedyType = body.substring(separator + 1, body.length() - 3);
            }
        }
        Matcher matcher = ARGUMENT.matcher(body);
        if (!matcher.matches()) {
            if (greedyType != null) {
                issues.add(new Issue(offset, offset + body.length(), "Greedy arguments must use String"));
            } else {
                issues.add(new Issue(offset, offset + body.length(), "Malformed argument"));
            }
            return;
        }
        String type = matcher.group(2);
        if (!TYPE_SET.contains(type)) {
            issues.add(new Issue(offset + matcher.start(2), offset + matcher.end(2), "Unknown argument type: " + type));
        }
        if (matcher.group(3) != null && !"String".equals(type)) {
            issues.add(new Issue(offset + matcher.start(2), offset + matcher.end(3), "Greedy arguments must use String"));
        }
    }

    private static void validateOption(String body, int offset, Set<String> aliases, List<Issue> issues) {
        Matcher matcher = OPTION.matcher(body);
        if (!matcher.matches()) {
            issues.add(new Issue(offset, offset + body.length(), "Malformed option"));
            return;
        }
        String type = matcher.group(2);
        if (type != null && !TYPE_SET.contains(type)) {
            issues.add(new Issue(offset + matcher.start(2), offset + matcher.end(2), "Unknown option type: " + type));
        }
        String alias = matcher.group(3);
        if (alias != null && !aliases.add("-" + alias)) {
            issues.add(new Issue(offset + matcher.start(3) - 1, offset + matcher.end(3), "Duplicate alias: -" + alias));
        }
    }

    private static void validateLiteralAliases(String token, int offset, Set<String> aliases, List<Issue> issues) {
        String[] parts = token.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            if (!aliases.add(parts[i])) {
                int start = token.indexOf(parts[i]);
                issues.add(new Issue(offset + start, offset + start + parts[i].length(), "Duplicate alias: " + parts[i]));
            }
        }
    }

    record Issue(int start, int end, String message) {
    }
}
