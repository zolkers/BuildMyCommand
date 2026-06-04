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

public final class BuildMyCommandRouteInjector implements MultiHostInjector {
    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!(context instanceof PsiLiteralExpression literal)) {
            return;
        }
        if (!BuildMyCommandRouteLiteralMatcher.isRouteLiteral(literal)) {
            return;
        }

        TextRange range = BuildMyCommandRouteLiteralMatcher.contentRange(literal);
        registrar
            .startInjecting(BuildMyCommandRouteLanguage.INSTANCE)
            .addPlace(null, null, (PsiLanguageInjectionHost) context, range)
            .doneInjecting();
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(PsiLiteralExpression.class);
    }
}
