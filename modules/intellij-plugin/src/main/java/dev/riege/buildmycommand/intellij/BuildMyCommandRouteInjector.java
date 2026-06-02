package dev.riege.buildmycommand.intellij;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class BuildMyCommandRouteInjector implements MultiHostInjector {
    private static final Set<String> ROUTE_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Command",
        "dev.riege.buildmycommand.annotation.Route",
        "dev.riege.buildmycommand.annotation.Subcommand"
    );

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof PsiLiteralExpression literal) || !(context instanceof PsiLanguageInjectionHost host)) {
            return;
        }
        if (!(literal.getValue() instanceof String)) {
            return;
        }
        if (!isRouteLiteral(literal)) {
            return;
        }

        TextRange range = stringContentRange(literal);
        if (range.isEmpty()) {
            return;
        }

        registrar
            .startInjecting(BuildMyCommandRouteLanguage.INSTANCE)
            .addPlace(null, null, host, range)
            .doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(PsiLiteralExpression.class);
    }

    private static boolean isRouteLiteral(PsiLiteralExpression literal) {
        return isRouteAnnotationValue(literal) || isRouteMethodArgument(literal);
    }

    private static boolean isRouteAnnotationValue(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiNameValuePair pair)) {
            return false;
        }
        PsiElement annotation = pair.getParent();
        while (annotation != null && !(annotation instanceof PsiAnnotation)) {
            annotation = annotation.getParent();
        }
        if (!(annotation instanceof PsiAnnotation psiAnnotation)) {
            return false;
        }
        return ROUTE_ANNOTATIONS.contains(psiAnnotation.getQualifiedName());
    }

    private static boolean isRouteMethodArgument(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList expressionList)) {
            return false;
        }
        PsiElement call = expressionList.getParent();
        if (!(call instanceof PsiMethodCallExpression methodCall)) {
            return false;
        }
        if (!"route".equals(methodCall.getMethodExpression().getReferenceName())) {
            return false;
        }
        return expressionList.getExpressions().length > 0 && expressionList.getExpressions()[0] == literal;
    }

    private static TextRange stringContentRange(PsiLiteralExpression literal) {
        String text = literal.getText();
        if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length() >= 6) {
            return TextRange.create(3, text.length() - 3);
        }
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            return TextRange.create(1, text.length() - 1);
        }
        return TextRange.EMPTY_RANGE;
    }
}
