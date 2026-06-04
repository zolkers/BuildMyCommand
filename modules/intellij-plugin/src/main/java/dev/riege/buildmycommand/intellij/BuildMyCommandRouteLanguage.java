/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lang.Language;

public final class BuildMyCommandRouteLanguage extends Language {
    public static final BuildMyCommandRouteLanguage INSTANCE = new BuildMyCommandRouteLanguage();

    private BuildMyCommandRouteLanguage() {
        super("BuildMyCommandRoute");
    }
}
