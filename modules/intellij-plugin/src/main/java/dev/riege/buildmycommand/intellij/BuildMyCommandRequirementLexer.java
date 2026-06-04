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

final class BuildMyCommandRequirementLexer extends LexerBase {
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
        if (startsWith("&&") || startsWith("||")) {
            tokenEnd = tokenStart + 2;
            tokenType = BuildMyCommandRequirementTokenType.OPERATOR;
            return;
        }
        if (current == '!') {
            tokenEnd = tokenStart + 1;
            tokenType = BuildMyCommandRequirementTokenType.NEGATION;
            return;
        }
        if (current == '(' || current == ')') {
            tokenEnd = tokenStart + 1;
            tokenType = BuildMyCommandRequirementTokenType.GROUPING;
            return;
        }
        consumeWhile(BuildMyCommandRequirementLexer::isPermissionChar);
        if (tokenEnd == tokenStart) {
            tokenEnd++;
        }
        tokenType = BuildMyCommandRequirementTokenType.PERMISSION;
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private void consumeWhile(CharacterPredicate predicate) {
        tokenEnd = tokenStart;
        while (tokenEnd < endOffset && predicate.test(buffer.charAt(tokenEnd))) {
            tokenEnd++;
        }
    }

    private boolean startsWith(String value) {
        if (tokenStart + value.length() > endOffset) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (buffer.charAt(tokenStart + index) != value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPermissionChar(char value) {
        return Character.isLetterOrDigit(value)
            || value == '_'
            || value == '-'
            || value == '.'
            || value == ':'
            || value == '*';
    }

    @FunctionalInterface
    private interface CharacterPredicate {
        boolean test(char value);
    }
}
