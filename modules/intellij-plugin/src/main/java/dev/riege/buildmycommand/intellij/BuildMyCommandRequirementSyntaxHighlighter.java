package dev.riege.buildmycommand.intellij;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandRequirementSyntaxHighlighter extends SyntaxHighlighterBase implements SyntaxHighlighter {
    static final TextAttributesKey PERMISSION =
        TextAttributesKey.createTextAttributesKey("ENTITY_NAME_PERMISSION_BUILDMYCOMMAND_REQUIRE",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    static final TextAttributesKey OPERATOR =
        TextAttributesKey.createTextAttributesKey("KEYWORD_OPERATOR_BUILDMYCOMMAND_REQUIRE",
            DefaultLanguageHighlighterColors.OPERATION_SIGN);
    static final TextAttributesKey GROUPING =
        TextAttributesKey.createTextAttributesKey("PUNCTUATION_GROUP_BUILDMYCOMMAND_REQUIRE",
            DefaultLanguageHighlighterColors.PARENTHESES);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new BuildMyCommandRequirementLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == BuildMyCommandRequirementTokenType.PERMISSION) {
            return pack(PERMISSION);
        }
        if (tokenType == BuildMyCommandRequirementTokenType.OPERATOR
            || tokenType == BuildMyCommandRequirementTokenType.NEGATION) {
            return pack(OPERATOR);
        }
        if (tokenType == BuildMyCommandRequirementTokenType.GROUPING) {
            return pack(GROUPING);
        }
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
