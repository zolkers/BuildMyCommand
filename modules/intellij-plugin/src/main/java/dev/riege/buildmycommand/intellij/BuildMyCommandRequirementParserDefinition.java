/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

public final class BuildMyCommandRequirementParserDefinition extends BuildMyCommandFlatParserDefinition {
    private static final BuildMyCommandInjectedFileType FILE_TYPE = new BuildMyCommandInjectedFileType(
        BuildMyCommandRequirementLanguage.INSTANCE,
        "BuildMyCommandRequirement",
        "BuildMyCommand requirement expression",
        "bmcrequire"
    );

    public BuildMyCommandRequirementParserDefinition() {
        super(BuildMyCommandRequirementLanguage.INSTANCE, FILE_TYPE, BuildMyCommandRequirementLexer::new);
    }
}
