/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BuildMyCommandPermissionRegexInjector implements MultiHostInjector {
    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof PsiLiteralExpression literal)
            || !(literal instanceof PsiLanguageInjectionHost host)
            || !BuildMyCommandPermissionRegexLiteralMatcher.isPermissionRegexLiteral(literal)) {
            return;
        }
        TextRange range = BuildMyCommandPermissionRegexLiteralMatcher.contentRange(literal);
        if (range.isEmpty()) {
            return;
        }
        registrar
            .startInjecting(BuildMyCommandPermissionRegexLanguage.INSTANCE)
            .addPlace(null, null, host, range)
            .doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(PsiLiteralExpression.class);
    }
}
