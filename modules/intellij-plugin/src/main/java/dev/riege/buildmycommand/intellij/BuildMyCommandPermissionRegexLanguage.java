/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lang.Language;

public final class BuildMyCommandPermissionRegexLanguage extends Language {
    public static final BuildMyCommandPermissionRegexLanguage INSTANCE = new BuildMyCommandPermissionRegexLanguage();

    private BuildMyCommandPermissionRegexLanguage() {
        super("BuildMyCommandPermissionRegex");
    }
}
