/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

public final class BuildMyCommandRouteParserDefinition extends BuildMyCommandFlatParserDefinition {
    private static final BuildMyCommandInjectedFileType FILE_TYPE = new BuildMyCommandInjectedFileType(
        BuildMyCommandRouteLanguage.INSTANCE,
        "BuildMyCommandRoute",
        "BuildMyCommand route DSL",
        "bmcroute"
    );

    public BuildMyCommandRouteParserDefinition() {
        super(BuildMyCommandRouteLanguage.INSTANCE, FILE_TYPE, BuildMyCommandRouteLexer::new);
    }
}
