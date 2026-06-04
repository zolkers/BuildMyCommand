/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import java.util.List;
import java.util.Objects;

public record HelpPage(
    String title,
    String footer,
    List<HelpEntry> entries,
    HelpOptions options,
    int page,
    int pageCount
) {
    public HelpPage {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(footer, "footer");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        Objects.requireNonNull(options, "options");
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (pageCount < 1) {
            throw new IllegalArgumentException("pageCount must be greater than zero");
        }
    }
}
