/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.core.help;

public interface CommandHelpFormatter {
    CommandHelpFormatter DEFAULT = new CommandHelpFormatter() {
    };

    default String formatDetails(String title, String details) {
        return header(title) + "\n" + details;
    }

    default String formatPage(CommandHelpPage page) {
        StringBuilder builder = new StringBuilder(header(page.title()))
            .append(" page ")
            .append(page.page())
            .append("/")
            .append(page.pageCount());
        page.options().group().ifPresent(group -> builder.append(" group ").append(group));
        if (page.options().alphabetic()) {
            builder.append(" alphabetic");
        }
        if (page.entries().isEmpty()) {
            builder.append("\nNo commands are available.");
            return builder.toString();
        }

        String currentGroup = null;
        for (CommandHelpEntry entry : page.entries()) {
            if (!entry.group().equals(currentGroup)) {
                currentGroup = entry.group();
                builder.append("\n\n").append(currentGroup);
            }
            builder.append("\n  ")
                .append(page.options().commandPrefix())
                .append(entry.path())
                .append(" - ")
                .append(entry.description());
            if (page.options().showAliases() && !entry.aliases().isEmpty()) {
                builder.append(" (aliases: ").append(String.join(", ", entry.aliases())).append(")");
            }
        }
        builder.append("\n\n").append(page.footer());
        return builder.toString();
    }

    default String header(String title) {
        return "== " + title + " ==";
    }
}
