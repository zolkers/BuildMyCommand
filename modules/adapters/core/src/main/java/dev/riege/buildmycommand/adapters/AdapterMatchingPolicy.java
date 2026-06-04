/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

public record AdapterMatchingPolicy(
    boolean caseInsensitiveLiterals,
    boolean caseInsensitiveOptions,
    boolean caseSensitiveArguments
) {
    public static AdapterMatchingPolicy strict() {
        return new AdapterMatchingPolicy(false, false, true);
    }
}
