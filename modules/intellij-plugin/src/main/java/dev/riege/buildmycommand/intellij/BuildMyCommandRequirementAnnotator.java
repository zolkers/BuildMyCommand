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

public final class BuildMyCommandRequirementAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiLiteralExpression literal)) {
            return;
        }
        if (!BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(literal)) {
            return;
        }

        TextRange contentRange = BuildMyCommandRequirementLiteralMatcher.contentRange(literal);
        String expression = literal.getText().substring(contentRange.getStartOffset(), contentRange.getEndOffset());
        Lexer lexer = new BuildMyCommandRequirementLexer();
        lexer.start(expression);

        int literalStart = literal.getTextRange().getStartOffset();
        int contentStart = literalStart + contentRange.getStartOffset();
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

        for (BuildMyCommandRequirementDsl.Issue issue : BuildMyCommandRequirementDsl.validate(expression)) {
            holder
                .newAnnotation(HighlightSeverity.WARNING, issue.message())
                .range(TextRange.create(contentStart + issue.start(), contentStart + issue.end()))
                .create();
        }
    }

    private static TextAttributesKey keyForToken(Object tokenType) {
        if (tokenType == BuildMyCommandRequirementTokenType.PERMISSION) {
            return BuildMyCommandRequirementSyntaxHighlighter.PERMISSION;
        }
        if (tokenType == BuildMyCommandRequirementTokenType.OPERATOR
            || tokenType == BuildMyCommandRequirementTokenType.NEGATION) {
            return BuildMyCommandRequirementSyntaxHighlighter.OPERATOR;
        }
        if (tokenType == BuildMyCommandRequirementTokenType.GROUPING) {
            return BuildMyCommandRequirementSyntaxHighlighter.GROUPING;
        }
        return null;
    }
}
