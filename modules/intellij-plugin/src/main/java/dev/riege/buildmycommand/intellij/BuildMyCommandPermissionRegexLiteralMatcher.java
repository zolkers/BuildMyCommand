/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReferenceExpression;

import java.util.Set;

final class BuildMyCommandPermissionRegexLiteralMatcher {
    private static final Set<String> PERMISSION_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Permission",
        "Permission"
    );

    private BuildMyCommandPermissionRegexLiteralMatcher() {
    }

    static boolean isPermissionRegexLiteral(PsiLiteralExpression literal) {
        return literal.getValue() instanceof String
            && (isRegexPermissionAnnotationValue(literal) || isPermissionRegexMethodArgument(literal));
    }

    static TextRange contentRange(PsiLiteralExpression literal) {
        return BuildMyCommandRouteLiteralMatcher.contentRange(literal);
    }

    private static boolean isRegexPermissionAnnotationValue(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiNameValuePair pair)) {
            return false;
        }
        PsiAnnotation annotation = parentAnnotation(pair);
        if (annotation == null || !isPermissionAnnotation(annotation) || !regexEnabled(annotation)) {
            return false;
        }
        String name = pair.getName();
        return name == null || "value".equals(name);
    }

    private static boolean isPermissionRegexMethodArgument(PsiLiteralExpression literal) {
        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList expressionList)) {
            return false;
        }
        PsiElement call = expressionList.getParent();
        if (!(call instanceof PsiMethodCallExpression methodCall)) {
            return false;
        }
        PsiExpression[] expressions = expressionList.getExpressions();
        PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
        return "permissionRegex".equals(methodExpression.getReferenceName())
            && expressions.length > 0
            && expressions[0] == literal;
    }

    private static PsiAnnotation parentAnnotation(PsiElement element) {
        PsiElement current = element.getParent();
        while (current != null && !(current instanceof PsiAnnotation)) {
            current = current.getParent();
        }
        return current instanceof PsiAnnotation annotation ? annotation : null;
    }

    private static boolean isPermissionAnnotation(PsiAnnotation annotation) {
        return PERMISSION_ANNOTATIONS.contains(annotation.getQualifiedName());
    }

    private static boolean regexEnabled(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("regex");
        return value instanceof PsiLiteralExpression literal && Boolean.TRUE.equals(literal.getValue());
    }
}
