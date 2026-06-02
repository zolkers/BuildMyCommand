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

public final class BuildMyCommandRouteAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiLiteralExpression literal)) {
            return;
        }
        if (!BuildMyCommandRouteLiteralMatcher.isRouteLiteral(literal)) {
            return;
        }

        TextRange contentRange = BuildMyCommandRouteLiteralMatcher.contentRange(literal);
        if (contentRange.isEmpty()) {
            return;
        }

        String route = literal.getText().substring(contentRange.getStartOffset(), contentRange.getEndOffset());
        Lexer lexer = new BuildMyCommandRouteLexer();
        lexer.start(route);

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

        for (BuildMyCommandRouteDsl.Issue issue : BuildMyCommandRouteDsl.validate(route)) {
            holder
                .newAnnotation(HighlightSeverity.WARNING, issue.message())
                .range(TextRange.create(contentStart + issue.start(), contentStart + issue.end()))
                .create();
        }
    }

    private static TextAttributesKey keyForToken(Object tokenType) {
        if (tokenType == BuildMyCommandRouteTokenType.LITERAL) {
            return BuildMyCommandRouteSyntaxHighlighter.LITERAL;
        }
        if (tokenType == BuildMyCommandRouteTokenType.ARGUMENT) {
            return BuildMyCommandRouteSyntaxHighlighter.ARGUMENT;
        }
        if (tokenType == BuildMyCommandRouteTokenType.TYPE) {
            return BuildMyCommandRouteSyntaxHighlighter.TYPE;
        }
        if (tokenType == BuildMyCommandRouteTokenType.OPTION_LONG) {
            return BuildMyCommandRouteSyntaxHighlighter.OPTION_LONG;
        }
        if (tokenType == BuildMyCommandRouteTokenType.OPTION_ALIAS) {
            return BuildMyCommandRouteSyntaxHighlighter.OPTION_ALIAS;
        }
        if (tokenType == BuildMyCommandRouteTokenType.GREEDY) {
            return BuildMyCommandRouteSyntaxHighlighter.GREEDY;
        }
        if (tokenType == BuildMyCommandRouteTokenType.MARKUP) {
            return BuildMyCommandRouteSyntaxHighlighter.MARKUP;
        }
        return null;
    }
}
