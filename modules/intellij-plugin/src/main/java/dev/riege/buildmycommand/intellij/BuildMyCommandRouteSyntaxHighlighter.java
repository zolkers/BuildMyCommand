package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandRouteSyntaxHighlighter extends SyntaxHighlighterBase implements SyntaxHighlighter {
    private static final TextAttributesKey LITERAL =
        TextAttributesKey.createTextAttributesKey("ENTITY_NAME_FUNCTION_LITERAL_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.KEYWORD);
    private static final TextAttributesKey ARGUMENT =
        TextAttributesKey.createTextAttributesKey("VARIABLE_PARAMETER_ARGUMENT_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.PARAMETER);
    private static final TextAttributesKey TYPE =
        TextAttributesKey.createTextAttributesKey("STORAGE_TYPE_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.CLASS_NAME);
    private static final TextAttributesKey OPTION_LONG =
        TextAttributesKey.createTextAttributesKey("ENTITY_NAME_OPTION_LONG_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    private static final TextAttributesKey OPTION_ALIAS =
        TextAttributesKey.createTextAttributesKey("ENTITY_NAME_OPTION_ALIAS_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    private static final TextAttributesKey GREEDY =
        TextAttributesKey.createTextAttributesKey("KEYWORD_OPERATOR_GREEDY_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.OPERATION_SIGN);
    private static final TextAttributesKey MARKUP =
        TextAttributesKey.createTextAttributesKey("PUNCTUATION_DEFINITION_BUILDMYCOMMAND_ROUTE",
            DefaultLanguageHighlighterColors.BRACKETS);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new BuildMyCommandRouteLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == BuildMyCommandRouteTokenType.LITERAL) {
            return pack(LITERAL);
        }
        if (tokenType == BuildMyCommandRouteTokenType.ARGUMENT) {
            return pack(ARGUMENT);
        }
        if (tokenType == BuildMyCommandRouteTokenType.TYPE) {
            return pack(TYPE);
        }
        if (tokenType == BuildMyCommandRouteTokenType.OPTION_LONG) {
            return pack(OPTION_LONG);
        }
        if (tokenType == BuildMyCommandRouteTokenType.OPTION_ALIAS) {
            return pack(OPTION_ALIAS);
        }
        if (tokenType == BuildMyCommandRouteTokenType.GREEDY) {
            return pack(GREEDY);
        }
        if (tokenType == BuildMyCommandRouteTokenType.MARKUP) {
            return pack(MARKUP);
        }
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
