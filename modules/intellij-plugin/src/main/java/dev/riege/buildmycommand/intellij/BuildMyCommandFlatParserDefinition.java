/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

abstract class BuildMyCommandFlatParserDefinition implements ParserDefinition {
    private static final TokenSet WHITESPACE = TokenSet.create(TokenType.WHITE_SPACE);

    private final IFileElementType file;
    private final BuildMyCommandInjectedFileType fileType;
    private final Supplier<Lexer> lexerFactory;

    BuildMyCommandFlatParserDefinition(
        @NotNull Language language,
        @NotNull BuildMyCommandInjectedFileType fileType,
        @NotNull Supplier<Lexer> lexerFactory
    ) {
        this.file = new IFileElementType(language);
        this.fileType = fileType;
        this.lexerFactory = lexerFactory;
    }

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return lexerFactory.get();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new BuildMyCommandFlatParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return file;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new BuildMyCommandInjectedFile(viewProvider, fileType);
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
