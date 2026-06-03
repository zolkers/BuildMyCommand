package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BuildMyCommandRouteLexer extends LexerBase {
    private CharSequence buffer = "";
    private int startOffset;
    private int endOffset;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
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
        if (startsWith("...")) {
            tokenEnd += 3;
            tokenType = BuildMyCommandRouteTokenType.GREEDY;
            return;
        }
        if (startsWith("--")) {
            consumeDashedWord();
            tokenType = BuildMyCommandRouteTokenType.OPTION_LONG;
            return;
        }
        if (current == '-' && tokenStart + 1 < endOffset && isWord(buffer.charAt(tokenStart + 1))) {
            consumeDashedWord();
            tokenType = BuildMyCommandRouteTokenType.OPTION_ALIAS;
            return;
        }
        if (isMarkup(current)) {
            tokenEnd++;
            tokenType = BuildMyCommandRouteTokenType.MARKUP;
            return;
        }
        if (isWord(current)) {
            consumeWhile(BuildMyCommandRouteLexer::isWord);
        } else {
            tokenEnd++;
        }
        tokenType = wordTokenType();
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

    private void consumeDashedWord() {
        tokenEnd = tokenStart + 1;
        while (tokenEnd < endOffset && (buffer.charAt(tokenEnd) == '-' || isWord(buffer.charAt(tokenEnd)))) {
            tokenEnd++;
        }
    }

    private IElementType wordTokenType() {
        char previous = previousSignificantChar();
        if (previous == ':') {
            return BuildMyCommandRouteTokenType.TYPE;
        }
        if (previous == '<' || previous == '[') {
            return BuildMyCommandRouteTokenType.ARGUMENT;
        }
        return BuildMyCommandRouteTokenType.LITERAL;
    }

    private char previousSignificantChar() {
        for (int index = tokenStart - 1; index >= startOffset; index--) {
            char value = buffer.charAt(index);
            if (!Character.isWhitespace(value)) {
                return value;
            }
        }
        return '\0';
    }

    private boolean startsWith(String value) {
        int length = value.length();
        if (tokenStart + length > endOffset) {
            return false;
        }
        for (int index = 0; index < length; index++) {
            if (buffer.charAt(tokenStart + index) != value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMarkup(char value) {
        return value == '<' || value == '>' || value == '[' || value == ']' || value == ':' || value == '|';
    }

    private static boolean isWord(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    @FunctionalInterface
    private interface CharacterPredicate {
        boolean test(char value);
    }
}
