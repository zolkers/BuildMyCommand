/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.Icon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BuildMyCommandInjectedFileType extends LanguageFileType {
    private final String name;
    private final String description;
    private final String extension;

    BuildMyCommandInjectedFileType(
        @NotNull Language language,
        @NotNull String name,
        @NotNull String description,
        @NotNull String extension
    ) {
        super(language);
        this.name = name;
        this.description = description;
        this.extension = extension;
    }

    @Override
    public @NonNls @NotNull String getName() {
        return name;
    }

    @Override
    public @Nls @NotNull String getDescription() {
        return description;
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return extension;
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }
}
