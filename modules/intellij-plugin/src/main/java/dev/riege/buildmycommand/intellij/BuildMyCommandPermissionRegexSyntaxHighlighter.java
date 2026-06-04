/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandPermissionRegexSyntaxHighlighter extends SyntaxHighlighterBase implements SyntaxHighlighter {
    static final TextAttributesKey LITERAL =
        TextAttributesKey.createTextAttributesKey("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    static final TextAttributesKey ESCAPE =
        TextAttributesKey.createTextAttributesKey("CONSTANT_CHARACTER_ESCAPE_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
    static final TextAttributesKey CHAR_CLASS =
        TextAttributesKey.createTextAttributesKey("CONSTANT_CHARACTER_CLASS_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.PARAMETER);
    static final TextAttributesKey QUANTIFIER =
        TextAttributesKey.createTextAttributesKey("KEYWORD_OPERATOR_QUANTIFIER_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.OPERATION_SIGN);
    static final TextAttributesKey GROUPING =
        TextAttributesKey.createTextAttributesKey("PUNCTUATION_GROUP_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.PARENTHESES);
    static final TextAttributesKey OPERATOR =
        TextAttributesKey.createTextAttributesKey("KEYWORD_OPERATOR_BUILDMYCOMMAND_REGEX",
            DefaultLanguageHighlighterColors.OPERATION_SIGN);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new BuildMyCommandPermissionRegexLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == BuildMyCommandPermissionRegexTokenType.LITERAL) {
            return pack(LITERAL);
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.ESCAPE) {
            return pack(ESCAPE);
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.CHAR_CLASS) {
            return pack(CHAR_CLASS);
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.QUANTIFIER) {
            return pack(QUANTIFIER);
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.GROUPING) {
            return pack(GROUPING);
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.OPERATOR) {
            return pack(OPERATOR);
        }
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
