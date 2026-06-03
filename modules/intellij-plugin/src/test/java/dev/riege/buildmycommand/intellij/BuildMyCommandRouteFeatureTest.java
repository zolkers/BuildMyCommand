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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
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

        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(annotation));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(shortSubRoute));
        assertTrue(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(registry));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(secondArgument));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(otherMethod));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(newExpression));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(arrayLiteral));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(plain));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(number));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(orphanAnnotationValue));
        assertFalse(BuildMyCommandRouteLiteralMatcher.isRouteLiteral(emptyRouteCallArgument));
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
                void invalid(@dev.riege.buildmycommand.annotation.Arg String target,
                             @dev.riege.buildmycommand.annotation.Option Integer amount,
                             @dev.riege.buildmycommand.annotation.Flag boolean silent) {
                }

                @dev.riege.buildmycommand.annotation.Route("valid <target:String>")
                void valid(@dev.riege.buildmycommand.annotation.RouteCtx Object context) {
                    String plain = "not a route";
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
        visitor.visitLiteralExpression(literal(file, "give <target:Player>"));
        visitor.visitLiteralExpression(literal(file, "not a route"));
        List<String> descriptions = holder.messages;

        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CONTEXT_TYPE_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_PARAMETER_ANNOTATIONS_FORBIDDEN));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUB_ROUTE_OWNER_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUBCOMMAND_OWNER_REQUIRED));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.COMMAND_LITERAL_ONLY));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.SUBCOMMAND_LITERAL_ONLY));
        assertTrue(descriptions.contains(BuildMyCommandRouteInspection.ROUTE_CTX_FORBIDDEN_OUTSIDE_ROUTE_DSL));
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
                    void metadataOnly(@dev.riege.buildmycommand.annotation.Default("fallback") String value) {
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
        PsiParameter defaulted = metadataOnly.getParameterList().getParameters()[0];

        assertTrue(provider.isImplicitUsage(moderation));
        assertTrue(provider.isImplicitUsage(qualified));
        assertTrue(provider.isImplicitUsage(metadataOnlyClass));
        assertTrue(provider.isImplicitUsage(punish));
        assertTrue(provider.isImplicitUsage(metadataOnly));
        assertTrue(provider.isImplicitUsage(route));
        assertTrue(provider.isImplicitUsage(defaulted));
        assertTrue(provider.isImplicitRead(route));
        assertFalse(provider.isImplicitUsage(plain));
        assertFalse(provider.isImplicitUsage(helper));
        assertFalse(provider.isImplicitUsage(value));
        assertFalse(provider.isImplicitUsage(file));
        assertFalse(provider.isImplicitWrite(route));
    }

    public void testSyntaxHighlighterMapsEveryTokenAndFallback() {
        BuildMyCommandRouteSyntaxHighlighter highlighter = new BuildMyCommandRouteSyntaxHighlighter();

        assertHighlight(highlighter, BuildMyCommandRouteTokenType.LITERAL, BuildMyCommandRouteSyntaxHighlighter.LITERAL);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.ARGUMENT, BuildMyCommandRouteSyntaxHighlighter.ARGUMENT);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.TYPE, BuildMyCommandRouteSyntaxHighlighter.TYPE);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.OPTION_LONG, BuildMyCommandRouteSyntaxHighlighter.OPTION_LONG);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.OPTION_ALIAS, BuildMyCommandRouteSyntaxHighlighter.OPTION_ALIAS);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.GREEDY, BuildMyCommandRouteSyntaxHighlighter.GREEDY);
        assertHighlight(highlighter, BuildMyCommandRouteTokenType.MARKUP, BuildMyCommandRouteSyntaxHighlighter.MARKUP);
        assertEquals(0, highlighter.getTokenHighlights(TokenType.WHITE_SPACE).length);
        assertNotNull(new BuildMyCommandRouteSyntaxHighlighterFactory().getSyntaxHighlighter(null, null));
    }

    public void testLexerCoversWhitespaceOffsetsAndTokenBoundaries() {
        Lexer lexer = new BuildMyCommandRouteLexer();

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

    private static PsiNameValuePair nameValuePairWithParent(PsiElement parent) {
        return (PsiNameValuePair) Proxy.newProxyInstance(
            PsiNameValuePair.class.getClassLoader(),
            new Class<?>[] {PsiNameValuePair.class},
            (proxy, method, args) -> "getParent".equals(method.getName()) ? parent : null
        );
    }

    private static PsiExpressionList expressionListForRouteCall(PsiExpression[] expressions) {
        PsiReferenceExpression methodExpression = (PsiReferenceExpression) Proxy.newProxyInstance(
            PsiReferenceExpression.class.getClassLoader(),
            new Class<?>[] {PsiReferenceExpression.class},
            (proxy, method, args) -> "getReferenceName".equals(method.getName()) ? "route" : null
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
                case "getParent" -> call;
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
