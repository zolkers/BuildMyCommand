/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.HashSet;
import java.util.Set;

final class BuildMyCommandRouteTypeResolver {
    private BuildMyCommandRouteTypeResolver() {
    }

    static Set<String> declaredCommandTypes(PsiFile file) {
        if (file == null) {
            return Set.of();
        }
        Set<String> aliases = new HashSet<>();
        for (PsiMethodCallExpression call : PsiTreeUtil.findChildrenOfType(file, PsiMethodCallExpression.class)) {
            PsiReferenceExpression methodExpression = call.getMethodExpression();
            String methodName = methodExpression.getReferenceName();
            if (!"type".equals(methodName) && !"register".equals(methodName)) {
                continue;
            }
            PsiExpression[] arguments = call.getArgumentList().getExpressions();
            if (arguments.length < 3 || !(arguments[1] instanceof PsiClassObjectAccessExpression)) {
                continue;
            }
            if (arguments[0] instanceof PsiLiteralExpression literal && literal.getValue() instanceof String alias
                && alias.matches("[A-Za-z][A-Za-z0-9_]*")) {
                aliases.add(alias);
            }
        }
        return Set.copyOf(aliases);
    }
}
