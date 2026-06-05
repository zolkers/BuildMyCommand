/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

public final class BuildMyCommandPermissionRegexParserDefinition extends BuildMyCommandFlatParserDefinition {
    private static final BuildMyCommandInjectedFileType FILE_TYPE = new BuildMyCommandInjectedFileType(
        BuildMyCommandPermissionRegexLanguage.INSTANCE,
        "BuildMyCommandPermissionRegex",
        "BuildMyCommand permission regex",
        "bmcregex"
    );

    public BuildMyCommandPermissionRegexParserDefinition() {
        super(BuildMyCommandPermissionRegexLanguage.INSTANCE, FILE_TYPE, BuildMyCommandPermissionRegexLexer::new);
    }
}
