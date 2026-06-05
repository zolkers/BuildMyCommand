/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

@CoverageGenerated
public final class BuildMyCommandRouteCompletionContributor extends CompletionContributor {
    private static final InsertHandler<LookupElement> TRAILING_SPACE = (context, item) -> {
        CharSequence text = context.getDocument().getCharsSequence();
        int tailOffset = context.getTailOffset();
        if (tailOffset >= text.length() || shouldInsertSpaceBefore(text.charAt(tailOffset))) {
            context.getDocument().insertString(tailOffset, " ");
            context.getEditor().getCaretModel().moveToOffset(tailOffset + 1);
        }
    };

    public BuildMyCommandRouteCompletionContributor() {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(BuildMyCommandRouteLanguage.INSTANCE),
            new com.intellij.codeInsight.completion.CompletionProvider<>() {
                @Override
                protected void addCompletions(
                    @NotNull CompletionParameters parameters,
                    @NotNull ProcessingContext context,
                    @NotNull CompletionResultSet result
                ) {
                    int cursor = parameters.getOffset() - parameters.getOriginalFile().getTextRange().getStartOffset();
                    for (String value : BuildMyCommandRouteDsl.completionsFor(parameters.getOriginalFile().getText(), cursor)) {
                        result.addElement(LookupElementBuilder.create(value).withInsertHandler(TRAILING_SPACE));
                    }
                }
            }
        );
    }

    private static boolean shouldInsertSpaceBefore(char value) {
        return !Character.isWhitespace(value) && value != '>' && value != ']' && value != '|' && value != ':';
    }
}
