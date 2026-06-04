/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandPermissionRegexTokenType extends IElementType {
    static final BuildMyCommandPermissionRegexTokenType LITERAL =
        new BuildMyCommandPermissionRegexTokenType("LITERAL");
    static final BuildMyCommandPermissionRegexTokenType ESCAPE =
        new BuildMyCommandPermissionRegexTokenType("ESCAPE");
    static final BuildMyCommandPermissionRegexTokenType CHAR_CLASS =
        new BuildMyCommandPermissionRegexTokenType("CHAR_CLASS");
    static final BuildMyCommandPermissionRegexTokenType QUANTIFIER =
        new BuildMyCommandPermissionRegexTokenType("QUANTIFIER");
    static final BuildMyCommandPermissionRegexTokenType GROUPING =
        new BuildMyCommandPermissionRegexTokenType("GROUPING");
    static final BuildMyCommandPermissionRegexTokenType OPERATOR =
        new BuildMyCommandPermissionRegexTokenType("OPERATOR");

    private BuildMyCommandPermissionRegexTokenType(@NotNull @NonNls String debugName) {
        super(debugName, BuildMyCommandPermissionRegexLanguage.INSTANCE);
    }
}
