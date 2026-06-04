/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.intellij;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

public final class BuildMyCommandRouteFeatureTest extends BasePlatformTestCase {
    public void testLiteralMatcherRecognizesRoutesAndContentRanges() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @dev.riege.buildmycommand.annotation.Route("root <target:String>")
                void annotated() {
                }

                @SubRoute("branch <id:String>")
                void shortSubRoute() {
                }

                void registry(dev.riege.buildmycommand.api.CommandRegistry registry) {
                    registry.route("call [--force|-f]", null);
                    registry.command("root", root -> root
                        .path("rank set promote", promote -> promote.executes(ctx -> null))
                        .subRoute("relative|rel <target:String>").executes(ctx -> null));
                    registry.route("first", "second");
                    registry.other("not route method");
                    new String("new expression literal");
                    String[] array = {"array literal"};
                    String plain = "not a route";
                    int number = 12;
                }
            }
            """);

        PsiLiteralExpression annotation = literal(file, "root <target:String>");
        PsiLiteralExpression shortSubRoute = literal(file, "branch <id:String>");
        PsiLiteralExpression registry = literal(file, "call [--force|-f]");
        PsiLiteralExpression literalPath = literal(file, "rank set promote");
        PsiLiteralExpression relativeSubRoute = literal(file, "relative|rel <target:String>");
        PsiLiteralExpression secondArgument = literal(file, "second");
        PsiLiteralExpression otherMethod = literal(file, "not route method");
        PsiLiteralExpression newExpression = literal(file, "new expression literal");
        PsiLiteralExpression arrayLiteral = literal(file, "array literal");
        PsiLiteralExpression plain = literal(file, "not a route");
        PsiLiteralExpression number = PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression.class)
            .stream()
            .filter(literal -> Integer.valueOf(12).equals(literal.getValue()))
            .findFirst()
            .orElseThrow();
        PsiLiteralExpression block = literalWithText("\"\"\"block <value:String>\"\"\"");
        PsiLiteralExpression openBlock = literalWithText("\"\"\"open");
        PsiLiteralExpression openString = literalWithText("\"open");
        PsiLiteralExpression shortBlock = literalWithText("\"\"\"");
        PsiLiteralExpression shortString = literalWithText("\"");
        PsiLiteralExpression raw = literalWithText("raw");
        PsiLiteralExpression orphanAnnotationValue = literalWithParent(nameValuePairWithParent(null));
        PsiLiteralExpression emptyRouteCallArgument = literalWithParent(expressionListForRouteCall(new PsiExpression[0]));
        PsiLiteralExpression emptyPathCallArgument = literalWithParent(expressionListForMethodCall("path",
            new PsiExpression[0], null));
        PsiLiteralExpression pathWithoutMethodCall = literalWithParent(expressionListForMethodCall("path",
            new PsiExpression[] {literalWithText("\"route\"")}, file));

        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(annotation));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(shortSubRoute));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(registry));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(literalPath));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(relativeSubRoute));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(secondArgument));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(otherMethod));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(newExpression));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(arrayLiteral));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(plain));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(number));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(orphanAnnotationValue));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(emptyRouteCallArgument));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(emptyPathCallArgument));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(pathWithoutMethodCall));
        assertEquals(TextRange.create(1, annotation.getText().length() - 1),
            BuildMyCommandRouteLiteralMatcher.contentRange(annotation));
        assertEquals(TextRange.create(3, block.getText().length() - 3),
            BuildMyCommandRouteLiteralMatcher.contentRange(block));
        assertEquals(TextRange.EMPTY_RANGE, BuildMyCommandRouteLiteralMatcher.contentRange(openBlock));
        assertEquals(TextRange.EMPTY_RANGE, BuildMyCommandRouteLiteralMatcher.contentRange(openString));
        assertEquals(TextRange.create(1, shortBlock.getText().length() - 1),
            BuildMyCommandRouteLiteralMatcher.contentRange(shortBlock));
        assertEquals(TextRange.EMPTY_RANGE, BuildMyCommandRouteLiteralMatcher.contentRange(shortString));
        assertEquals(TextRange.EMPTY_RANGE, BuildMyCommandRouteLiteralMatcher.contentRange(raw));
    }

    public void testInjectorRegistersRouteLanguageForRouteLiteralsOnly() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @dev.riege.buildmycommand.annotation.Route("root <target:String>")
                void annotated() {
                    String plain = "not a route";
                    int number = 1;
                }
            }
            """);
        PsiLiteralExpression route = literal(file, "root <target:String>");
        PsiLiteralExpression plain = literal(file, "not a route");
        RecordingRegistrar registrar = new RecordingRegistrar();
        BuildMyCommandRouteInjector injector = new BuildMyCommandRouteInjector();

        injector.getLanguagesToInject(registrar, route);
        injector.getLanguagesToInject(new RecordingRegistrar(), plain);
        injector.getLanguagesToInject(new RecordingRegistrar(), file);

        assertSame(BuildMyCommandRouteLanguage.INSTANCE, registrar.language);
        assertSame(route, registrar.host);
        assertEquals(TextRange.create(1, route.getText().length() - 1), registrar.range);
        assertTrue(registrar.done);
        assertEquals(List.of(PsiLiteralExpression.class), injector.elementsToInjectIn());
    }

    public void testRequirementLiteralMatcherAndInjectorUseRequirementLanguageOnly() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @dev.riege.buildmycommand.annotation.Require("staff && !banned")
                @Require("")
                @Permission("plain.node")
                void annotated() {
                    registry.route("admin reload")
                        .requirement("owner || staff")
                        .permission("admin.reload");
                    String plain = "staff && owner";
                }
            }
            """);
        PsiLiteralExpression annotation = literal(file, "staff && !banned");
        PsiLiteralExpression builder = literal(file, "owner || staff");
        PsiLiteralExpression empty = literal(file, "");
        PsiLiteralExpression permission = literal(file, "plain.node");
        PsiLiteralExpression plain = literal(file, "staff && owner");
        PsiLiteralExpression orphanAnnotationValue = literalWithParent(nameValuePairWithParent(null));
        PsiLiteralExpression orphanRequirementCall = literalWithParent(expressionListForMethodCall("requirement",
            new PsiExpression[] {literalWithText("\"route\"")}, file));
        PsiLiteralExpression emptyRequirementCall = literalWithParent(expressionListForMethodCall("requirement",
            new PsiExpression[0], null));
        RecordingRegistrar registrar = new RecordingRegistrar();
        BuildMyCommandRequirementInjector injector = new BuildMyCommandRequirementInjector();

        assertTrue(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(annotation));
        assertTrue(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(builder));
        assertFalse(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(permission));
        assertFalse(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(plain));
        assertFalse(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(orphanAnnotationValue));
        assertFalse(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(orphanRequirementCall));
        assertFalse(BuildMyCommandRequirementLiteralMatcher.isRequirementLiteral(emptyRequirementCall));

        injector.getLanguagesToInject(registrar, annotation);
        injector.getLanguagesToInject(new RecordingRegistrar(), permission);
        injector.getLanguagesToInject(new RecordingRegistrar(), empty);

        assertSame(BuildMyCommandRequirementLanguage.INSTANCE, registrar.language);
        assertSame(annotation, registrar.host);
        assertEquals(TextRange.create(1, annotation.getText().length() - 1), registrar.range);
        assertTrue(registrar.done);
        assertEquals(List.of(PsiLiteralExpression.class), injector.elementsToInjectIn());
    }

    public void testAnnotatorHighlightsAndWarnsForRouteDsl() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @dev.riege.buildmycommand.annotation.Route("give <target:Player> [--force|-f]")
                void annotated() {
                    String plain = "not a route";
                }
            }
            """);
        PsiLiteralExpression route = literal(file, "give <target:Player> [--force|-f]");
        PsiLiteralExpression plain = literal(file, "not a route");
        RecordingAnnotationHolder holder = new RecordingAnnotationHolder();
        BuildMyCommandRouteAnnotator annotator = new BuildMyCommandRouteAnnotator();

        annotator.annotate(file, holder.proxy());
        annotator.annotate(plain, holder.proxy());
        annotator.annotate(route, holder.proxy());

        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.INFORMATION
                && BuildMyCommandRouteSyntaxHighlighter.LITERAL.equals(record.attributes)));
        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.WARNING
                && "Unknown argument type: Player".equals(record.message)));
    }

    public void testRequirementAnnotatorHighlightsBooleanDslAndWarnsMalformedExpressions() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @dev.riege.buildmycommand.annotation.Require("staff && (!banned || owner)")
                void valid() {
                }

                @Require("staff && (owner || )")
                void invalid() {
                    String plain = "staff && owner";
                }
            }
            """);
        PsiLiteralExpression valid = literal(file, "staff && (!banned || owner)");
        PsiLiteralExpression invalid = literal(file, "staff && (owner || )");
        PsiLiteralExpression plain = literal(file, "staff && owner");
        RecordingAnnotationHolder holder = new RecordingAnnotationHolder();
        BuildMyCommandRequirementAnnotator annotator = new BuildMyCommandRequirementAnnotator();

        annotator.annotate(file, holder.proxy());
        annotator.annotate(plain, holder.proxy());
        annotator.annotate(valid, holder.proxy());
        annotator.annotate(invalid, holder.proxy());

        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.INFORMATION
                && BuildMyCommandRequirementSyntaxHighlighter.PERMISSION.equals(record.attributes)));
        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.INFORMATION
                && BuildMyCommandRequirementSyntaxHighlighter.OPERATOR.equals(record.attributes)));
        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.INFORMATION
                && BuildMyCommandRequirementSyntaxHighlighter.GROUPING.equals(record.attributes)));
        assertTrue(holder.records.stream().anyMatch(record ->
            record.severity == HighlightSeverity.WARNING
                && BuildMyCommandRequirementDsl.MISSING_OPERAND.equals(record.message)));
    }

    public void testInspectionReportsRouteContractAndDslProblems() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @Route("ctor")
                Demo() {
                }

                void plain() {
                }

                @Route("short <target:String>")
                void shortRoute(@RouteCtx Object context) {
                }

                @SubRoute("short child")
                void shortSubRoute(@RouteCtx Object context) {
                }

                @SubRoute("orphan child")
                void orphanSubRoute(@dev.riege.buildmycommand.annotation.RouteCtx dev.riege.buildmycommand.api.CommandContext context) {
                }

                @Command("bad root <target:String>")
                void commandDsl() {
                }

                @Command
                void commandWithoutValue() {
                }

                @Subcommand("orphan")
                void orphanSubcommand() {
                }

                @Subcommand("orphan-class")
                static final class OrphanClass {
                }

                @dev.riege.buildmycommand.annotation.Command("owner")
                static final class Owner {
                    @Subcommand("nested")
                    static final class Nested {
                        @Subcommand("deeper")
                        static final class Deeper {
                        }

                        @Subcommand("leaf")
                        void nestedLeaf() {
                        }
                    }

                    @Subcommand("bad child <target:String>")
                    void subcommandDsl() {
                    }

                    @dev.riege.buildmycommand.annotation.Subcommand("child")
                    void routeContextOnPlainSubcommand(@dev.riege.buildmycommand.annotation.RouteCtx dev.riege.buildmycommand.api.CommandContext context) {
                    }
                }

                @dev.riege.buildmycommand.annotation.Route("give <target:Player>")
                void invalid(@dev.riege.buildmycommand.annotation.RouteCtx dev.riege.buildmycommand.api.CommandContext context) {
                }

                @dev.riege.buildmycommand.annotation.Route("valid <target:String>")
                void valid(@dev.riege.buildmycommand.annotation.RouteCtx Object context) {
                    String plain = "not a route";
                }

                void registry(dev.riege.buildmycommand.api.CommandRegistry registry) {
                    registry.command("rank", rank -> rank
                        .path("set promote <target:String>", promote -> promote.executes(ctx -> null))
                        .path("set demote|down", demote -> demote.executes(ctx -> null))
                        .path("set clear [--force]", clear -> clear.executes(ctx -> null))
                        .path("set inspect", inspect -> inspect.executes(ctx -> null))
                        .subRoute("set assign <target:String>", assign -> assign.executes(ctx -> null)));
                }
            }

            @Subcommand("top")
            final class TopLevelSubcommand {
            }
            """);

        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        JavaElementVisitor visitor = (JavaElementVisitor) new BuildMyCommandRouteInspection().buildVisitor(holder, true);
        for (PsiClass psiClass : PsiTreeUtil.findChildrenOfType(file, PsiClass.class)) {
            visitor.visitClass(psiClass);
        }
        for (PsiMethod method : PsiTreeUtil.findChildrenOfType(file, PsiMethod.class)) {
            visitor.visitMethod(method);
        }
        for (PsiLiteralExpression literal : PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression.class)) {
            visitor.visitLiteralExpression(literal);
        }
        List<String> descriptions = holder.messages;

        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_TYPE_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUB_ROUTE_OWNER_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUBCOMMAND_OWNER_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.COMMAND_LITERAL_ONLY));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUBCOMMAND_LITERAL_ONLY));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.DEEP_SUBCOMMAND_TREE_DISCOURAGED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.PATH_LITERAL_ONLY));
        assertTrue(descriptions.contains("Unknown argument type: Player"));
        assertNotNull(file);
    }

    public void testInspectionReportsRequirementAndPermissionDslProblems() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @Permission("staff || owner")
                @Require("staff && (owner || )")
                void annotated() {
                }

                void registry(dev.riege.buildmycommand.api.CommandRegistry registry) {
                    registry.route("admin reload")
                        .permission(12)
                        .permission("admin.reload && staff")
                        .permission("admin.simple", "ignored")
                        .requirement("staff && (!banned || owner)")
                        .executes(ctx -> dev.riege.buildmycommand.api.Results.silent());
                    registry.command("admin", command -> command
                        .permission("admin.root || owner")
                        .requirement("staff && (owner || )"));
                }
            }
            """);

        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        JavaElementVisitor visitor = (JavaElementVisitor) new BuildMyCommandRouteInspection().buildVisitor(holder, true);
        for (PsiMethod method : PsiTreeUtil.findChildrenOfType(file, PsiMethod.class)) {
            visitor.visitMethod(method);
        }
        for (PsiLiteralExpression literal : PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression.class)) {
            visitor.visitLiteralExpression(literal);
        }

        assertTrue(holder.messages.contains("@Permission accepts one permission node; use @Require for boolean expressions"));
        assertTrue(holder.messages.contains("Requirement expression is missing an operand"));
        assertFalse(holder.messages.contains("staff && (!banned || owner)"));
    }

    public void testInspectionReportsSuggestNamesThatDoNotMatchRouteBindings() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @Route("global <target:String> [--mode:String|-m]")
                void global(@RouteCtx dev.riege.buildmycommand.api.CommandContext route) {
                }

                @Suggest("target")
                java.util.List<String> validRouteSuggestion() {
                    return java.util.List.of();
                }

                @Suggest("mode")
                dev.riege.buildmycommand.api.SuggestionSet validOptionSuggestion() {
                    return dev.riege.buildmycommand.api.SuggestionSet.empty();
                }

                @Suggest("missing")
                java.util.List<String> invalidSuggestion() {
                    return java.util.List.of();
                }

                @Suggest("")
                java.util.List<String> blankSuggestion() {
                    return java.util.List.of();
                }

                static final class UtilitySuggestions {
                    @Suggest("externalProvider")
                    java.util.List<String> providerOnlySuggestion() {
                        return java.util.List.of();
                    }
                }

                @dev.riege.buildmycommand.annotation.Command("wecc")
                static final class WeccCommands {
                    @SubRoute("bang|b <target:String>")
                    dev.riege.buildmycommand.api.CommandResult bang(@RouteCtx dev.riege.buildmycommand.api.CommandContext route) {
                        return dev.riege.buildmycommand.api.Results.silent();
                    }

                    @Suggest("target")
                    java.util.List<String> validSubRouteSuggestion() {
                        return java.util.List.of();
                    }

                    @Suggest("wat")
                    java.util.List<String> invalidSubRouteSuggestion() {
                        return java.util.List.of();
                    }
                }
            }
            """);

        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        JavaElementVisitor visitor = (JavaElementVisitor) new BuildMyCommandRouteInspection().buildVisitor(holder, true);
        for (PsiMethod method : PsiTreeUtil.findChildrenOfType(file, PsiMethod.class)) {
            visitor.visitMethod(method);
        }

        assertTrue(holder.messages.contains("@Suggest name does not match any @Route/@SubRoute argument or option in this class"));
        assertEquals(2, holder.messages.stream()
            .filter(BuildMyCommandRouteInspection.SUGGEST_TARGET_NOT_FOUND::equals)
            .count());
    }

    public void testInspectionReportsConcreteAnnotationContractProblems() {
        addBuildMyCommandApiStubs();
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @Alias({"contracts", ""})
                @Middleware({GoodImplicitMiddleware.class, GoodExplicitMiddleware.class, NotMiddleware.class, MissingMiddleware.class})
                static final class ClassLevelMiddlewareContracts {
                }

                @Middleware
                static final class MissingMiddlewareValue {
                }

                @Route("private")
                private dev.riege.buildmycommand.api.CommandResult privateRoute(
                    @RouteCtx dev.riege.buildmycommand.api.CommandContext route
                ) {
                    return dev.riege.buildmycommand.api.Results.silent();
                }

                @Route("protected")
                protected dev.riege.buildmycommand.api.CommandResult protectedRoute(
                    @RouteCtx dev.riege.buildmycommand.api.CommandContext route
                ) {
                    return dev.riege.buildmycommand.api.Results.silent();
                }

                @Route("bad-return")
                String badReturn(@RouteCtx dev.riege.buildmycommand.api.CommandContext route) {
                    return "";
                }

                @Route("bad-param <target:String>")
                dev.riege.buildmycommand.api.CommandResult badParam(String raw) {
                    return dev.riege.buildmycommand.api.Results.silent();
                }

                @Suggest("")
                String badSuggestReturn(String raw) {
                    return "";
                }

                @Suggest("target")
                java.util.List<String> goodSuggest(dev.riege.buildmycommand.api.SuggestionContext context) {
                    return java.util.List.of();
                }

                @Cooldown(0)
                @Usage("")
                @Example("")
                @Middleware(BadConstructorMiddleware.class)
                @Route("metadata")
                dev.riege.buildmycommand.api.CommandResult badMetadata(
                    @RouteCtx dev.riege.buildmycommand.api.CommandContext route
                ) {
                    return dev.riege.buildmycommand.api.Results.silent();
                }

                static final class GoodImplicitMiddleware implements dev.riege.buildmycommand.api.CommandMiddleware {
                    public dev.riege.buildmycommand.api.CommandResult execute(
                        dev.riege.buildmycommand.api.CommandContext context,
                        dev.riege.buildmycommand.api.CommandNode command,
                        java.util.List<String> commandPath,
                        dev.riege.buildmycommand.api.CommandMiddleware.Chain next
                    ) {
                        return next.proceed(context);
                    }
                }

                static final class GoodExplicitMiddleware implements dev.riege.buildmycommand.api.CommandMiddleware {
                    GoodExplicitMiddleware() {
                    }

                    public dev.riege.buildmycommand.api.CommandResult execute(
                        dev.riege.buildmycommand.api.CommandContext context,
                        dev.riege.buildmycommand.api.CommandNode command,
                        java.util.List<String> commandPath,
                        dev.riege.buildmycommand.api.CommandMiddleware.Chain next
                    ) {
                        return next.proceed(context);
                    }
                }

                static final class BadConstructorMiddleware implements dev.riege.buildmycommand.api.CommandMiddleware {
                    BadConstructorMiddleware(String value) {
                    }

                    public dev.riege.buildmycommand.api.CommandResult execute(
                        dev.riege.buildmycommand.api.CommandContext context,
                        dev.riege.buildmycommand.api.CommandNode command,
                        java.util.List<String> commandPath,
                        dev.riege.buildmycommand.api.CommandMiddleware.Chain next
                    ) {
                        return next.proceed(context);
                    }
                }

                static final class NotMiddleware {
                }
            }
            """);

        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        JavaElementVisitor visitor = (JavaElementVisitor) new BuildMyCommandRouteInspection().buildVisitor(holder, true);
        for (PsiClass psiClass : PsiTreeUtil.findChildrenOfType(file, PsiClass.class)) {
            visitor.visitClass(psiClass);
        }
        for (PsiMethod method : PsiTreeUtil.findChildrenOfType(file, PsiMethod.class)) {
            visitor.visitMethod(method);
        }

        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.COMMAND_METHOD_VISIBILITY_REQUIRED));
        assertEquals(2, holder.messages.stream()
            .filter(BuildMyCommandRouteInspection.COMMAND_METHOD_VISIBILITY_REQUIRED::equals)
            .count());
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.COMMAND_METHOD_RETURN_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.COMMAND_METHOD_PARAMETER_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.SUGGEST_PROVIDER_SIGNATURE_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.SUGGEST_VALUE_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.COOLDOWN_POSITIVE_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.METADATA_NOT_BLANK));
        assertEquals(3, holder.messages.stream()
            .filter(BuildMyCommandRouteInspection.METADATA_NOT_BLANK::equals)
            .count());
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.MIDDLEWARE_TYPE_REQUIRED));
        assertTrue(holder.messages.contains(BuildMyCommandRouteInspection.MIDDLEWARE_NO_ARG_CONSTRUCTOR_REQUIRED));
    }

    public void testInspectionPrivateFallbackHelpersCoverSyntheticEdges() throws Exception {
        addBuildMyCommandApiStubs();
        PsiElement file = myFixture.configureByText("Demo.java", "final class Demo {}");
        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        Method problemElement = BuildMyCommandRouteInspection.class.getDeclaredMethod(
            "problemElement",
            PsiElement.class,
            PsiElement.class
        );
        Method isMiddlewareClass = BuildMyCommandRouteInspection.class.getDeclaredMethod(
            "isMiddlewareClass",
            PsiClass.class,
            PsiElement.class
        );
        problemElement.setAccessible(true);
        isMiddlewareClass.setAccessible(true);

        assertSame(file, problemElement.invoke(null, null, file));
        assertNotNull(isMiddlewareClass.invoke(null, findClass(file, "Demo"), file));
        assertTrue(holder.messages.isEmpty());
    }

    private void addBuildMyCommandApiStubs() {
        myFixture.addFileToProject("dev/riege/buildmycommand/api/CommandMiddleware.java", """
            package dev.riege.buildmycommand.api;
            public interface CommandMiddleware {
                CommandResult execute(CommandContext context, CommandNode command, java.util.List<String> commandPath, Chain next);
                interface Chain {
                    CommandResult proceed(CommandContext context);
                }
            }
            """);
        myFixture.addFileToProject("dev/riege/buildmycommand/api/CommandResult.java",
            "package dev.riege.buildmycommand.api; public final class CommandResult {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/CommandContext.java",
            "package dev.riege.buildmycommand.api; public final class CommandContext {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/CommandNode.java",
            "package dev.riege.buildmycommand.api; public final class CommandNode {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/ArgumentParseContext.java",
            "package dev.riege.buildmycommand.api; public final class ArgumentParseContext {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/SuggestionContext.java",
            "package dev.riege.buildmycommand.api; public final class SuggestionContext {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/SuggestionSet.java",
            "package dev.riege.buildmycommand.api; public final class SuggestionSet {}");
        myFixture.addFileToProject("dev/riege/buildmycommand/api/Results.java", """
            package dev.riege.buildmycommand.api;
            public final class Results {
                public static CommandResult silent() {
                    return new CommandResult();
                }
            }
            """);
    }

    public void testSuggestInspectionIgnoresProviderMethodsWithoutContainingClass() throws Exception {
        PsiElement file = myFixture.configureByText("Demo.java", "final class Demo {}");
        RecordingProblemsHolder holder = new RecordingProblemsHolder(file.getContainingFile());
        Method inspect = BuildMyCommandRouteInspection.class.getDeclaredMethod(
            "inspectSuggestBinding",
            PsiMethod.class,
            ProblemsHolder.class
        );
        inspect.setAccessible(true);

        inspect.invoke(null, providerWithSuggestValue(literalWithParent(null)), holder);
        inspect.invoke(null, providerWithSuggestValue(null), holder);

        assertTrue(holder.messages.isEmpty());
    }

    public void testImplicitUsageProviderMarksCommandAnnotationsAsUsed() {
        PsiElement file = myFixture.configureByText("Demo.java", """
            final class Demo {
                @CaseInsensitive(literals = true, options = true)
                static final class ModerationCommands {
                    @Route("moderation|mod punish <target:String> <reason:String...> [--duration:Integer|-d] [--silent|-s]")
                    @Description("Punish a user")
                    @Permission("mod.punish")
                    void punish(@RouteCtx dev.riege.buildmycommand.api.CommandContext route) {
                    }
                }

                static final class PlainCommands {
                    void helper(String value) {
                    }
                }

                @dev.riege.buildmycommand.annotation.Command("qualified")
                static final class QualifiedCommands {
                    @dev.riege.buildmycommand.annotation.Description("metadata class")
                    static final class MetadataOnlyClass {
                    }

                    @dev.riege.buildmycommand.annotation.Description("metadata method")
                    @dev.riege.buildmycommand.annotation.Suggest("target")
                    java.util.List<String> metadataOnly() {
                        return java.util.List.of("Ada");
                    }
                }
            }
            """);

        BuildMyCommandImplicitUsageProvider provider = new BuildMyCommandImplicitUsageProvider();
        PsiClass moderation = findClass(file, "ModerationCommands");
        PsiClass plain = findClass(file, "PlainCommands");
        PsiClass qualified = findClass(file, "QualifiedCommands");
        PsiClass metadataOnlyClass = findClass(file, "MetadataOnlyClass");
        PsiMethod punish = findMethod(file, "punish");
        PsiMethod helper = findMethod(file, "helper");
        PsiMethod metadataOnly = findMethod(file, "metadataOnly");
        PsiParameter route = punish.getParameterList().getParameters()[0];
        PsiParameter value = helper.getParameterList().getParameters()[0];

        assertTrue(provider.isImplicitUsage(moderation));
        assertTrue(provider.isImplicitUsage(qualified));
        assertTrue(provider.isImplicitUsage(metadataOnlyClass));
        assertTrue(provider.isImplicitUsage(punish));
        assertTrue(provider.isImplicitUsage(metadataOnly));
        assertTrue(provider.isImplicitUsage(route));
        assertTrue(provider.isImplicitRead(route));
        assertFalse(provider.isImplicitUsage(plain));
        assertFalse(provider.isImplicitUsage(helper));
        assertFalse(provider.isImplicitUsage(value));
        assertFalse(provider.isImplicitUsage(file));
        assertFalse(provider.isImplicitWrite(route));
    }

    public void testSyntaxHighlighterMapsEveryTokenAndFallback() {
        BuildMyCommandRouteSyntaxHighlighter highlighter = new BuildMyCommandRouteSyntaxHighlighter();
        BuildMyCommandRequirementSyntaxHighlighter requirementHighlighter =
            new BuildMyCommandRequirementSyntaxHighlighter();

        assertHighlight(highlighter, BuildMyCommandRouteTokenType.LITERAL, BuildMyCommandRouteSyntaxHighlighter.LITERAL);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.ARGUMENT, BuildMyCommandRouteSyntaxHighlighter.ARGUMENT);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.TYPE, BuildMyCommandRouteSyntaxHighlighter.TYPE);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.OPTION_LONG, BuildMyCommandRouteSyntaxHighlighter.OPTION_LONG);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.OPTION_ALIAS, BuildMyCommandRouteSyntaxHighlighter.OPTION_ALIAS);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.GREEDY, BuildMyCommandRouteSyntaxHighlighter.GREEDY);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.MARKUP, BuildMyCommandRouteSyntaxHighlighter.MARKUP);
        assertEquals(0, highlighter.getTokenHighlights(TokenType.WHITE_SPACE).length);
        assertNotNull(new BuildMyCommandRouteSyntaxHighlighterFactory().getSyntaxHighlighter(null, null));
        assertRequirementHighlight(requirementHighlighter, BuildMyCommandRequirementTokenType.PERMISSION,
            BuildMyCommandRequirementSyntaxHighlighter.PERMISSION);
        assertRequirementHighlight(requirementHighlighter, BuildMyCommandRequirementTokenType.OPERATOR,
            BuildMyCommandRequirementSyntaxHighlighter.OPERATOR);
        assertRequirementHighlight(requirementHighlighter, BuildMyCommandRequirementTokenType.NEGATION,
            BuildMyCommandRequirementSyntaxHighlighter.OPERATOR);
        assertRequirementHighlight(requirementHighlighter, BuildMyCommandRequirementTokenType.GROUPING,
            BuildMyCommandRequirementSyntaxHighlighter.GROUPING);
        assertEquals(0, requirementHighlighter.getTokenHighlights(TokenType.WHITE_SPACE).length);
        assertNotNull(new BuildMyCommandRequirementSyntaxHighlighterFactory().getSyntaxHighlighter(null, null));
    }

    public void testLexerCoversWhitespaceOffsetsAndTokenBoundaries() {
        Lexer lexer = new BuildMyCommandRouteLexer();
        Lexer requirementLexer = new BuildMyCommandRequirementLexer();

        lexer.start("  give <target:String> . ! - [--flag|-f]", 2, 39, 0);
        assertEquals(0, lexer.getState());
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        assertEquals("give", tokenText(lexer));
        lexer.advance();
        assertEquals(TokenType.WHITE_SPACE, lexer.getTokenType());
        lexer.advance();
        assertEquals(BuildMyCommandRouteTokenType.MARKUP, lexer.getTokenType());
        lexer.advance();
        assertEquals(BuildMyCommandRouteTokenType.ARGUMENT, lexer.getTokenType());

        lexer.start("!", 0, 1, 0);
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        assertEquals("!", tokenText(lexer));
        lexer.advance();
        assertNull(lexer.getTokenType());
        assertEquals(1, lexer.getBufferEnd());

        lexer.start("-.", 0, 2, 0);
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        lexer.advance();
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        lexer.start("-", 0, 1, 0);
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        lexer.start("--.", 0, 3, 0);
        assertEquals(BuildMyCommandRouteTokenType.OPTION_LONG, lexer.getTokenType());
        lexer.advance();
        lexer.start("--", 0, 2, 0);
        assertEquals(BuildMyCommandRouteTokenType.OPTION_LONG, lexer.getTokenType());
        lexer.start("[optional", 0, 9, 0);
        lexer.advance();
        assertEquals(BuildMyCommandRouteTokenType.ARGUMENT, lexer.getTokenType());
        lexer.start("<  arg", 0, 6, 0);
        lexer.advance();
        lexer.advance();
        assertEquals(BuildMyCommandRouteTokenType.ARGUMENT, lexer.getTokenType());
        lexer.start("name", 0, 4, 0);
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());
        lexer.start("_", 0, 1, 0);
        assertEquals(BuildMyCommandRouteTokenType.LITERAL, lexer.getTokenType());

        requirementLexer.start(" staff && (!banned || owner)", 1, 28, 0);
        assertEquals(BuildMyCommandRequirementTokenType.PERMISSION, requirementLexer.getTokenType());
        assertEquals("staff", tokenText(requirementLexer));
        requirementLexer.advance();
        assertEquals(TokenType.WHITE_SPACE, requirementLexer.getTokenType());
        requirementLexer.advance();
        assertEquals(BuildMyCommandRequirementTokenType.OPERATOR, requirementLexer.getTokenType());
        requirementLexer.advance();
        requirementLexer.advance();
        assertEquals(BuildMyCommandRequirementTokenType.GROUPING, requirementLexer.getTokenType());
        requirementLexer.advance();
        assertEquals(BuildMyCommandRequirementTokenType.NEGATION, requirementLexer.getTokenType());
        requirementLexer.start("$", 0, 1, 0);
        assertEquals(BuildMyCommandRequirementTokenType.PERMISSION, requirementLexer.getTokenType());
        assertEquals(0, requirementLexer.getState());
        assertEquals(1, requirementLexer.getBufferEnd());
        requirementLexer.advance();
        assertNull(requirementLexer.getTokenType());
        assertEquals(1, requirementLexer.getTokenStart());
    }

    public void testDslValidationCoversMalformedInputsAndCompletionFallbacks() throws Exception {
        List<BuildMyCommandRouteDsl.Issue> issues = BuildMyCommandRouteDsl.validate(
            "  root|r|r <bad <bad...> <broken:Integer....> <name:String...> [loose [--silent] [--broken:Unknown.Name] [--bad|long] [optional:String] <late:String>"
        );

        assertIssue(issues, "Duplicate alias: r");
        assertIssue(issues, "Malformed argument");
        assertIssue(issues, "Greedy arguments must use String");
        assertIssue(issues, "Unknown option type: Unknown.Name");
        assertIssue(issues, "Malformed option");
        assertIssue(issues, "Required argument cannot follow an optional argument");
        assertEquals(List.of(), BuildMyCommandRouteDsl.validate("[--silent]"));
        assertEquals(List.of(), BuildMyCommandRouteDsl.validate("[--duration:Integer]"));
        assertEquals(List.of(), BuildMyCommandRouteDsl.completionsFor("give", 100));
        assertEquals(List.of(), BuildMyCommandRouteDsl.completionsFor("give", -4));

        var constructor = BuildMyCommandRouteDsl.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    public void testRequirementDslValidationCoversValidAndMalformedInputs() throws Exception {
        assertEquals(List.of(), BuildMyCommandRequirementDsl.validate("staff && (!banned || owner)"));
        assertTrue(BuildMyCommandRequirementDsl.looksBoolean("staff || owner"));
        assertTrue(BuildMyCommandRequirementDsl.looksBoolean("!banned"));
        assertTrue(BuildMyCommandRequirementDsl.looksBoolean("(staff)"));
        assertFalse(BuildMyCommandRequirementDsl.looksBoolean("admin.reload"));
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("staff &&"), BuildMyCommandRequirementDsl.MISSING_OPERAND);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("staff owner"), BuildMyCommandRequirementDsl.MISSING_OPERATOR);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("staff && (owner || )"),
            BuildMyCommandRequirementDsl.MISSING_OPERAND);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("staff!owner"),
            BuildMyCommandRequirementDsl.MISSING_OPERATOR);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("!"), BuildMyCommandRequirementDsl.MISSING_OPERAND);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("(staff"), BuildMyCommandRequirementDsl.UNCLOSED_GROUP);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("staff)"), BuildMyCommandRequirementDsl.UNEXPECTED_GROUP_END);
        assertRequirementIssue(BuildMyCommandRequirementDsl.validate("&& staff"), BuildMyCommandRequirementDsl.MISSING_OPERAND);

        Constructor<BuildMyCommandRequirementDsl> constructor =
            BuildMyCommandRequirementDsl.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    public void testAnnotatorPrivateTokenMappingCoversGreedyAndFallback() throws Exception {
        Method keyForToken = BuildMyCommandRouteAnnotator.class.getDeclaredMethod("keyForToken", Object.class);
        keyForToken.setAccessible(true);

        assertEquals(BuildMyCommandRouteSyntaxHighlighter.GREEDY,
            keyForToken.invoke(null, BuildMyCommandRouteTokenType.GREEDY));
        assertNull(keyForToken.invoke(null, TokenType.WHITE_SPACE));
    }

    public void testTextMateBundleProviderConvertsInvalidResourceUriToIllegalState() throws Exception {
        URL invalid = new URL(null, "broken://bundle", new URLStreamHandler() {
            @Override
            protected java.net.URLConnection openConnection(URL url) {
                throw new UnsupportedOperationException();
            }

            @Override
            protected String toExternalForm(URL url) {
                return "http://[broken";
            }
        });

        IllegalStateException exception = null;
        try {
            BuildMyCommandTextMateBundleProvider.bundle("BuildMyCommand Route DSL", invalid);
            fail("Expected invalid URI to be wrapped");
        } catch (IllegalStateException expected) {
            exception = expected;
        }
        assertTrue(exception.getMessage().contains("Invalid BuildMyCommand TextMate bundle path"));
    }

    private static PsiLiteralExpression literal(PsiElement root, String value) {
        return PsiTreeUtil.findChildrenOfType(root, PsiLiteralExpression.class)
            .stream()
            .filter(literal -> value.equals(literal.getValue()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing literal " + value + " in " + PsiTreeUtil
                .findChildrenOfType(root, PsiLiteralExpression.class)
                .stream()
                .map(PsiLiteralExpression::getValue)
                .toList()));
    }

    private static PsiClass findClass(PsiElement root, String name) {
        return PsiTreeUtil.findChildrenOfType(root, PsiClass.class)
            .stream()
            .filter(psiClass -> name.equals(psiClass.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing class " + name));
    }

    private static PsiMethod findMethod(PsiElement root, String name) {
        return PsiTreeUtil.findChildrenOfType(root, PsiMethod.class)
            .stream()
            .filter(method -> name.equals(method.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing method " + name));
    }

    private static PsiLiteralExpression literalWithText(String text) {
        return (PsiLiteralExpression) Proxy.newProxyInstance(
            PsiLiteralExpression.class.getClassLoader(),
            new Class<?>[] {PsiLiteralExpression.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getText" -> text;
                case "getValue" -> "";
                default -> null;
            }
        );
    }

    private static PsiLiteralExpression literalWithParent(PsiElement parent) {
        return (PsiLiteralExpression) Proxy.newProxyInstance(
            PsiLiteralExpression.class.getClassLoader(),
            new Class<?>[] {PsiLiteralExpression.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getText" -> "\"route\"";
                case "getValue" -> "route";
                case "getParent" -> parent;
                default -> null;
            }
        );
    }

    private static PsiMethod providerWithSuggestValue(PsiLiteralExpression value) {
        PsiAnnotation suggest = (PsiAnnotation) Proxy.newProxyInstance(
            PsiAnnotation.class.getClassLoader(),
            new Class<?>[] {PsiAnnotation.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getQualifiedName" -> "dev.riege.buildmycommand.annotation.Suggest";
                case "findDeclaredAttributeValue" -> value;
                default -> null;
            }
        );
        PsiModifierList modifierList = (PsiModifierList) Proxy.newProxyInstance(
            PsiModifierList.class.getClassLoader(),
            new Class<?>[] {PsiModifierList.class},
            (proxy, method, args) -> "getAnnotations".equals(method.getName()) ? new PsiAnnotation[] {suggest} : null
        );
        return (PsiMethod) Proxy.newProxyInstance(
            PsiMethod.class.getClassLoader(),
            new Class<?>[] {PsiMethod.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getModifierList" -> modifierList;
                case "getContainingClass" -> null;
                default -> null;
            }
        );
    }

    private static PsiNameValuePair nameValuePairWithParent(PsiElement parent) {
        return (PsiNameValuePair) Proxy.newProxyInstance(
            PsiNameValuePair.class.getClassLoader(),
            new Class<?>[] {PsiNameValuePair.class},
            (proxy, method, args) -> "getParent".equals(method.getName()) ? parent : null
        );
    }

    private static PsiExpressionList expressionListForRouteCall(PsiExpression[] expressions) {
        return expressionListForMethodCall("route", expressions, null);
    }

    private static PsiExpressionList expressionListForMethodCall(
        String methodName,
        PsiExpression[] expressions,
        PsiElement parentOverride
    ) {
        PsiReferenceExpression methodExpression = (PsiReferenceExpression) Proxy.newProxyInstance(
            PsiReferenceExpression.class.getClassLoader(),
            new Class<?>[] {PsiReferenceExpression.class},
            (proxy, method, args) -> "getReferenceName".equals(method.getName()) ? methodName : null
        );
        PsiMethodCallExpression call = (PsiMethodCallExpression) Proxy.newProxyInstance(
            PsiMethodCallExpression.class.getClassLoader(),
            new Class<?>[] {PsiMethodCallExpression.class},
            (proxy, method, args) -> "getMethodExpression".equals(method.getName()) ? methodExpression : null
        );
        return (PsiExpressionList) Proxy.newProxyInstance(
            PsiExpressionList.class.getClassLoader(),
            new Class<?>[] {PsiExpressionList.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getParent" -> parentOverride == null ? call : parentOverride;
                case "getExpressions" -> expressions;
                default -> null;
            }
        );
    }

    private static String tokenText(Lexer lexer) {
        return lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
    }

    private static void assertHighlight(
        BuildMyCommandRouteSyntaxHighlighter highlighter,
        IElementType tokenType,
        TextAttributesKey key
    ) {
        assertEquals(key, highlighter.getTokenHighlights(tokenType)[0]);
    }

    private static void assertRequirementHighlight(
        BuildMyCommandRequirementSyntaxHighlighter highlighter,
        IElementType tokenType,
        TextAttributesKey key
    ) {
        assertEquals(key, highlighter.getTokenHighlights(tokenType)[0]);
    }

    private static void assertIssue(List<BuildMyCommandRouteDsl.Issue> issues, String message) {
        assertTrue(message, issues.stream().anyMatch(issue -> message.equals(issue.message())));
    }

    private static void assertRequirementIssue(List<BuildMyCommandRequirementDsl.Issue> issues, String message) {
        assertTrue(message, issues.stream().anyMatch(issue -> message.equals(issue.message())));
    }

    private static final class RecordingRegistrar implements MultiHostRegistrar {
        private Language language;
        private PsiLanguageInjectionHost host;
        private TextRange range;
        private boolean done;

        @Override
        public MultiHostRegistrar startInjecting(Language language) {
            this.language = language;
            return this;
        }

        @Override
        public MultiHostRegistrar addPlace(
            String prefix,
            String suffix,
            PsiLanguageInjectionHost host,
            TextRange range
        ) {
            this.host = host;
            this.range = range;
            return this;
        }

        @Override
        public void doneInjecting() {
            done = true;
        }
    }

    private static final class RecordingAnnotationHolder {
        private final List<Record> records = new ArrayList<>();

        private AnnotationHolder proxy() {
            return (AnnotationHolder) Proxy.newProxyInstance(
                AnnotationHolder.class.getClassLoader(),
                new Class<?>[] {AnnotationHolder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "newAnnotation" -> builder((HighlightSeverity) args[0], (String) args[1]);
                    case "newSilentAnnotation" -> builder((HighlightSeverity) args[0], null);
                    case "isBatchMode" -> false;
                    case "getCurrentAnnotationSession" -> null;
                    default -> null;
                }
            );
        }

        private AnnotationBuilder builder(HighlightSeverity severity, String message) {
            InvocationHandler handler = new InvocationHandler() {
                private TextRange range;
                private TextAttributesKey attributes;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) {
                        case "range" -> range = (TextRange) args[0];
                        case "textAttributes" -> attributes = (TextAttributesKey) args[0];
                        case "create", "createAnnotation" -> {
                            records.add(new Record(severity, message, range, attributes));
                            return null;
                        }
                        default -> {
                        }
                    }
                    return proxy;
                }
            };
            return (AnnotationBuilder) Proxy.newProxyInstance(
                AnnotationBuilder.class.getClassLoader(),
                new Class<?>[] {AnnotationBuilder.class},
                handler
            );
        }
    }

    private record Record(
        HighlightSeverity severity,
        String message,
        TextRange range,
        TextAttributesKey attributes
    ) {
    }

    private static final class RecordingProblemsHolder extends ProblemsHolder {
        private final List<String> messages = new ArrayList<>();

        private RecordingProblemsHolder(PsiFile file) {
            super(InspectionManager.getInstance(file.getProject()), file, true);
        }

        @Override
        public void registerProblem(PsiElement psiElement, String descriptionTemplate, LocalQuickFix... fixes) {
            messages.add(descriptionTemplate);
        }

        @Override
        public void registerProblem(
            PsiElement psiElement,
            TextRange rangeInElement,
            String descriptionTemplate,
            LocalQuickFix... fixes
        ) {
            messages.add(descriptionTemplate);
        }

        @Override
        public void registerProblem(ProblemDescriptor problemDescriptor) {
            messages.add(problemDescriptor.getDescriptionTemplate());
        }
    }
}
