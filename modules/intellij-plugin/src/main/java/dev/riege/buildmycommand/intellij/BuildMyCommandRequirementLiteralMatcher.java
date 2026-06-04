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

final class BuildMyCommandRequirementLiteralMatcher {
    private static final Set<String> REQUIRE_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Require",
        "Require"
    );

    private BuildMyCommandRequirementLiteralMatcher() {
    }

    static boolean isRequirementLiteral(PsiLiteralExpression literal) {
        return literal.getValue() instanceof String
            && (isRequireAnnotationValue(literal) || isRequirementMethodArgument(literal));
    }

    static TextRange contentRange(PsiLiteralExpression literal) {
        return BuildMyCommandRouteLiteralMatcher.contentRange(literal);
    }

    private static boolean isRequireAnnotationValue(PsiLiteralExpression literal) {
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
        return REQUIRE_ANNOTATIONS.contains(psiAnnotation.getQualifiedName());
    }

    private static boolean isRequirementMethodArgument(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList expressionList)) {
            return false;
        }
        PsiElement call = expressionList.getParent();
        if (!(call instanceof PsiMethodCallExpression methodCall)) {
            return false;
        }
        return "requirement".equals(methodCall.getMethodExpression().getReferenceName())
            && expressionList.getExpressions().length > 0
            && expressionList.getExpressions()[0] == literal;
    }
}
