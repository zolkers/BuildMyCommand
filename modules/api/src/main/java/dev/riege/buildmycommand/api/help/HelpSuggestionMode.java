/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

/**
 * Controls how {@link HelpProviderAPI} should complete command names.
 */
public enum HelpSuggestionMode {
    /**
     * Suggest only the next path segment, matching shell and Minecraft-style completion menus.
     */
    SEGMENT,

    /**
     * Suggest complete visible command paths.
     */
    PATH,

    /**
     * Prefer segment suggestions, then fall back to full paths when no segment can be inferred.
     */
    SMART
}
