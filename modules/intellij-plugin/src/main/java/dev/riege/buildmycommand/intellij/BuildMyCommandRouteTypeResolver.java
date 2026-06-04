/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

import java.util.HashSet;
import java.util.Set;

final class BuildMyCommandRouteTypeResolver {
    private BuildMyCommandRouteTypeResolver() {
    }

    static Set<String> declaredCommandTypes(PsiFile file) {
        if (file == null) {
            return Set.of();
        }
        Set<String> aliases = new HashSet<>(declaredFileCommandTypes(file));
        aliases.addAll(declaredProjectCommandTypes(file.getProject()));
        return Set.copyOf(aliases);
    }

    private static Set<String> declaredProjectCommandTypes(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
            Set<String> aliases = new HashSet<>();
            PsiManager psiManager = PsiManager.getInstance(project);
            for (VirtualFile virtualFile : FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    aliases.addAll(declaredFileCommandTypes(psiFile));
                }
            }
            return CachedValueProvider.Result.create(Set.copyOf(aliases), PsiModificationTracker.MODIFICATION_COUNT);
        });
    }

    private static Set<String> declaredFileCommandTypes(PsiFile file) {
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
