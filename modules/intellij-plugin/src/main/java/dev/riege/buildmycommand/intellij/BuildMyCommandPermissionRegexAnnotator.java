/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class BuildMyCommandPermissionRegexAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiLiteralExpression literal)
            || !BuildMyCommandPermissionRegexLiteralMatcher.isPermissionRegexLiteral(literal)) {
            return;
        }

        TextRange contentRange = BuildMyCommandPermissionRegexLiteralMatcher.contentRange(literal);
        String expression = literal.getText().substring(contentRange.getStartOffset(), contentRange.getEndOffset());
        int contentStart = literal.getTextRange().getStartOffset() + contentRange.getStartOffset();
        highlightTokens(holder, expression, contentStart);
        validateRegex(holder, expression, contentStart, contentRange);
    }

    private static void highlightTokens(AnnotationHolder holder, String expression, int contentStart) {
        Lexer lexer = new BuildMyCommandPermissionRegexLexer();
        lexer.start(expression);
        while (lexer.getTokenType() != null) {
            TextAttributesKey key = keyForToken(lexer.getTokenType());
            if (key != null) {
                holder
                    .newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange.create(contentStart + lexer.getTokenStart(), contentStart + lexer.getTokenEnd()))
                    .textAttributes(key)
                    .create();
            }
            lexer.advance();
        }
    }

    private static void validateRegex(
        AnnotationHolder holder,
        String expression,
        int contentStart,
        TextRange contentRange
    ) {
        try {
            Pattern.compile(expression);
        } catch (PatternSyntaxException ignored) {
            holder
                .newAnnotation(HighlightSeverity.WARNING, BuildMyCommandRouteInspection.PERMISSION_REGEX_INVALID)
                .range(TextRange.create(contentStart, contentStart + contentRange.getLength()))
                .create();
        }
    }

    private static TextAttributesKey keyForToken(Object tokenType) {
        if (tokenType == BuildMyCommandPermissionRegexTokenType.LITERAL) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.LITERAL;
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.ESCAPE) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.ESCAPE;
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.CHAR_CLASS) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.CHAR_CLASS;
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.QUANTIFIER) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.QUANTIFIER;
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.GROUPING) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.GROUPING;
        }
        if (tokenType == BuildMyCommandPermissionRegexTokenType.OPERATOR) {
            return BuildMyCommandPermissionRegexSyntaxHighlighter.OPERATOR;
        }
        return null;
    }
}
