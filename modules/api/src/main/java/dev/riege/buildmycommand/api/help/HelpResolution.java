/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import java.util.Objects;
import java.util.Optional;

/**
 * Structured result produced by a help provider before final string formatting.
 */
public record HelpResolution(Kind kind, Optional<String> details, Optional<HelpPage> page) {
    public HelpResolution {
        Objects.requireNonNull(kind, "kind");
        details = Objects.requireNonNull(details, "details");
        page = Objects.requireNonNull(page, "page");
        if (kind == Kind.DETAILS && details.isEmpty()) {
            throw new IllegalArgumentException("details resolution requires details text");
        }
        if (kind == Kind.PAGE && page.isEmpty()) {
            throw new IllegalArgumentException("page resolution requires a page");
        }
    }

    public static HelpResolution details(String details) {
        return new HelpResolution(Kind.DETAILS, Optional.of(Objects.requireNonNull(details, "details")),
            Optional.empty());
    }

    public static HelpResolution page(HelpPage page) {
        return new HelpResolution(Kind.PAGE, Optional.empty(), Optional.of(Objects.requireNonNull(page, "page")));
    }

    public enum Kind {
        DETAILS,
        PAGE
    }
}
