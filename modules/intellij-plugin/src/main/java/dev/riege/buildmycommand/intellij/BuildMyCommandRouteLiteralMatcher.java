/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameValuePair;

import java.util.Set;

final class BuildMyCommandRouteLiteralMatcher {
    private static final Set<String> ROUTE_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Route",
        "dev.riege.buildmycommand.annotation.SubRoute",
        "Route",
        "SubRoute"
    );

    private BuildMyCommandRouteLiteralMatcher() {
    }

    static boolean isRouteLiteral(PsiLiteralExpression literal) {
        return literal.getValue() instanceof String
            && (isRouteAnnotationValue(literal) || isRouteMethodArgument(literal) || isLiteralPathMethodArgument(literal));
    }

    static boolean isLiteralPathLiteral(PsiLiteralExpression literal) {
        return literal.getValue() instanceof String && isLiteralPathMethodArgument(literal);
    }

    static TextRange contentRange(PsiLiteralExpression literal) {
        String text = literal.getText();
        if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length() >= 6) {
            return TextRange.create(3, text.length() - 3);
        }
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            return TextRange.create(1, text.length() - 1);
        }
        return TextRange.EMPTY_RANGE;
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
        String methodName = methodCall.getMethodExpression().getReferenceName();
        if (!"route".equals(methodName) && !"subRoute".equals(methodName)) {
            return false;
        }
        return expressionList.getExpressions().length > 0 && expressionList.getExpressions()[0] == literal;
    }

    private static boolean isLiteralPathMethodArgument(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList expressionList)) {
            return false;
        }
        PsiElement call = expressionList.getParent();
        if (!(call instanceof PsiMethodCallExpression methodCall)) {
            return false;
        }
        String methodName = methodCall.getMethodExpression().getReferenceName();
        if (!"path".equals(methodName)) {
            return false;
        }
        return expressionList.getExpressions().length > 0 && expressionList.getExpressions()[0] == literal;
    }
}
