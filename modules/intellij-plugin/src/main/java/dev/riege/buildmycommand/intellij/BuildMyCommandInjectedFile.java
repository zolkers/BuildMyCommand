/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

final class BuildMyCommandInjectedFile extends PsiFileBase {
    private final BuildMyCommandInjectedFileType fileType;

    BuildMyCommandInjectedFile(
        @NotNull FileViewProvider viewProvider,
        @NotNull BuildMyCommandInjectedFileType fileType
    ) {
        super(viewProvider, fileType.getLanguage());
        this.fileType = fileType;
    }

    @Override
    public @NotNull FileType getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return fileType.getDescription();
    }
}
