/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BuildMyCommandPermissionRegexLexer extends LexerBase {
    private CharSequence buffer = "";
    private int endOffset;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        tokenStart = startOffset;
        tokenEnd = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (tokenEnd >= endOffset) {
            tokenStart = endOffset;
            tokenType = null;
            return;
        }
        tokenStart = tokenEnd;
        char current = buffer.charAt(tokenStart);
        if (Character.isWhitespace(current)) {
            consumeWhile(Character::isWhitespace);
            tokenType = TokenType.WHITE_SPACE;
            return;
        }
        if (current == '\\') {
            tokenEnd = Math.min(tokenStart + 2, endOffset);
            tokenType = BuildMyCommandPermissionRegexTokenType.ESCAPE;
            return;
        }
        if (current == '[') {
            consumeCharClass();
            tokenType = BuildMyCommandPermissionRegexTokenType.CHAR_CLASS;
            return;
        }
        if (current == '*' || current == '+' || current == '?' || current == '{') {
            consumeQuantifier();
            tokenType = BuildMyCommandPermissionRegexTokenType.QUANTIFIER;
            return;
        }
        if (current == '(' || current == ')') {
            tokenEnd = tokenStart + 1;
            tokenType = BuildMyCommandPermissionRegexTokenType.GROUPING;
            return;
        }
        if (current == '|' || current == '^' || current == '$' || current == '.') {
            tokenEnd = tokenStart + 1;
            tokenType = BuildMyCommandPermissionRegexTokenType.OPERATOR;
            return;
        }
        consumeWhile(BuildMyCommandPermissionRegexLexer::isLiteralChar);
        if (tokenEnd == tokenStart) {
            tokenEnd++;
        }
        tokenType = BuildMyCommandPermissionRegexTokenType.LITERAL;
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private void consumeCharClass() {
        tokenEnd = tokenStart + 1;
        boolean escaped = false;
        while (tokenEnd < endOffset) {
            char current = buffer.charAt(tokenEnd++);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == ']') {
                return;
            }
        }
    }

    private void consumeQuantifier() {
        tokenEnd = tokenStart + 1;
        if (buffer.charAt(tokenStart) != '{') {
            return;
        }
        while (tokenEnd < endOffset && buffer.charAt(tokenEnd - 1) != '}') {
            tokenEnd++;
        }
    }

    private void consumeWhile(CharacterPredicate predicate) {
        tokenEnd = tokenStart;
        while (tokenEnd < endOffset && predicate.test(buffer.charAt(tokenEnd))) {
            tokenEnd++;
        }
    }

    private static boolean isLiteralChar(char value) {
        return Character.isLetterOrDigit(value)
            || value == '_'
            || value == '-'
            || value == ':';
    }

    @FunctionalInterface
    private interface CharacterPredicate {
        boolean test(char value);
    }
}
