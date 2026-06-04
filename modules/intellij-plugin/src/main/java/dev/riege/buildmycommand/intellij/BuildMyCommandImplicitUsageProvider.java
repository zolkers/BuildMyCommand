/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class BuildMyCommandImplicitUsageProvider implements ImplicitUsageProvider {
    private static final Set<String> COMMAND_CLASS_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Command",
        "dev.riege.buildmycommand.annotation.Subcommand",
        "dev.riege.buildmycommand.annotation.CommandGroup",
        "dev.riege.buildmycommand.annotation.CaseInsensitive"
    );
    private static final Set<String> COMMAND_METHOD_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Command",
        "dev.riege.buildmycommand.annotation.Route",
        "dev.riege.buildmycommand.annotation.Subcommand",
        "dev.riege.buildmycommand.annotation.SubRoute"
    );
    private static final Set<String> COMMAND_PARAMETER_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.RouteCtx"
    );
    private static final Set<String> COMMAND_METADATA_ANNOTATIONS = Set.of(
        "dev.riege.buildmycommand.annotation.Alias",
        "dev.riege.buildmycommand.annotation.Description",
        "dev.riege.buildmycommand.annotation.Permission",
        "dev.riege.buildmycommand.annotation.Usage",
        "dev.riege.buildmycommand.annotation.Example",
        "dev.riege.buildmycommand.annotation.Cooldown",
        "dev.riege.buildmycommand.annotation.Require",
        "dev.riege.buildmycommand.annotation.Middleware",
        "dev.riege.buildmycommand.annotation.Hidden",
        "dev.riege.buildmycommand.annotation.Suggest",
        "dev.riege.buildmycommand.annotation.SuggestAliases"
    );

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof PsiClass psiClass) {
            return hasAnyAnnotation(psiClass, COMMAND_CLASS_ANNOTATIONS)
                || hasAnyAnnotation(psiClass, COMMAND_METADATA_ANNOTATIONS);
        }
        if (element instanceof PsiMethod method) {
            return hasAnyAnnotation(method, COMMAND_METHOD_ANNOTATIONS)
                || hasAnyAnnotation(method, COMMAND_METADATA_ANNOTATIONS);
        }
        if (element instanceof PsiParameter parameter) {
            return hasAnyAnnotation(parameter, COMMAND_PARAMETER_ANNOTATIONS)
                || hasAnyAnnotation(parameter, COMMAND_METADATA_ANNOTATIONS);
        }
        return false;
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return isImplicitUsage(element);
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }

    private static boolean hasAnyAnnotation(PsiModifierListOwner owner, Set<String> annotations) {
        for (PsiAnnotation annotation : owner.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && annotations.contains(qualifiedName)) {
                return true;
            }
            if (qualifiedName != null && annotations.contains("dev.riege.buildmycommand.annotation." + qualifiedName)) {
                return true;
            }
        }
        return false;
    }
}
