package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.CaseInsensitive;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.CommandGroup;
import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Default;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Flag;
import dev.riege.buildmycommand.annotation.Greedy;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Option;
import dev.riege.buildmycommand.annotation.OptionalArg;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandRegistry;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionProvider;
import dev.riege.buildmycommand.api.SuggestionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnnotationBindingCoverageTest {
    @Test
    void validatesRouteBindingsForEveryRuntimeRouteShape() throws Exception {
        Method method = method(ValidatorCommands.class, "all", String.class, int.class, long.class, double.class,
            boolean.class, UUID.class, String.class, String.class, String.class, Integer.class, Optional.class,
            Boolean.class);
        MethodCommandBinder.BoundMethod bound = MethodCommandBinder.bind(new ValidatorCommands(), method);

        assertDoesNotThrow(() -> AnnotationRouteValidator.validate("""
            root <target:String> <count:Integer> <total:Long> <ratio:Double> <enabled:Boolean> <id:UUID> \
            [optional:String] <greedy:String...> [tail:String...] [--amount:Integer] [--maybe:Long] [--silent]\
            """, method, bound.bindings()));
    }

    @Test
    void rejectsRouteBindingsThatDoNotMatchTheRouteDsl() throws Exception {
        ValidatorCommands target = new ValidatorCommands();

        assertRouteError("root", target, "argumentOnly",
            "@Arg(\"target\") does not exist in route DSL for root", String.class);
        assertRouteError("root", target, "inferred",
            "@Arg(\"target\") does not exist in route DSL for root", String.class);
        assertRouteError("root", target, "optionOnly",
            "@Option(\"amount\") does not exist in route DSL for root", Integer.class);
        assertRouteError("root", target, "flagOnly",
            "@Flag(\"silent\") does not exist in route DSL for root", boolean.class);
        assertRouteError("root <target:Integer>", target, "argumentOnly",
            "route argument target expects Integer but method parameter is String on target", String.class);
        assertRouteError("root [--amount:Long]", target, "optionOnly",
            "route option amount expects Long but method parameter is Integer on amount", Integer.class);
        assertRouteError("root [target:String]", target, "argumentOnly",
            "route argument target is optional but method parameter is not @OptionalArg", String.class);
        assertRouteError("root <target:String>", target, "optionalArgument",
            "route argument target is required but method parameter is @OptionalArg", String.class);
        assertRouteError("root <target:String...>", target, "argumentOnly",
            "route argument target is greedy but method parameter is not @Greedy", String.class);
        assertRouteError("root <target:String>", target, "greedyArgument",
            "route argument target is not greedy but method parameter is @Greedy", String.class);
        assertRouteError("root <target:String>", target, "noParameters",
            "route argument target has no matching method parameter on noParameters");
        assertRouteError("root [--amount:Integer]", target, "noParameters",
            "route option amount has no matching method parameter on noParameters");
        assertRouteError("root [--silent]", target, "noParameters",
            "route flag silent has no matching method parameter on noParameters");
    }

    @Test
    void validatesRouteContextUsageRejectsEveryNonRouteContextBindingKind() throws Exception {
        ValidatorCommands target = new ValidatorCommands();
        Method method = method(ValidatorCommands.class, "route", CommandContext.class);
        MethodCommandBinder.ParameterBinding routeContext = MethodCommandBinder.bind(target, method).bindings().get(0);

        assertRouteContextError(method, routeContext, binding(target, "argumentOnly", String.class),
            "@Route method cannot use @Arg parameters; read route values from @RouteCtx CommandContext: route");
        assertRouteContextError(method, routeContext, binding(target, "optionOnly", Integer.class),
            "@Route method cannot use @Option parameters; read route values from @RouteCtx CommandContext: route");
        assertRouteContextError(method, routeContext, binding(target, "flagOnly", boolean.class),
            "@Route method cannot use @Flag parameters; read route values from @RouteCtx CommandContext: route");
        assertRouteContextError(method, routeContext, MethodCommandBinder.ParameterBinding.context(),
            "@Route method cannot use @null parameters; read route values from @RouteCtx CommandContext: route");
    }

    @Test
    void routeCoverageIgnoresNonMatchingBindingsBeforeFindingMatches() throws Exception {
        ValidatorCommands target = new ValidatorCommands();
        Method method = method(ValidatorCommands.class, "route", CommandContext.class);
        MethodCommandBinder.ParameterBinding context = MethodCommandBinder.ParameterBinding.context();

        assertDoesNotThrow(() -> AnnotationRouteValidator.validate("root <other:String> <target:String>", method, List.of(
            context,
            MethodCommandBinder.ParameterBinding.argument("other", String.class, false, false, null,
                MethodCommandBinder.SuggestionBinding.empty(), "Arg"),
            MethodCommandBinder.ParameterBinding.argument("target", String.class, false, false, null,
                MethodCommandBinder.SuggestionBinding.empty(), "Arg")
        )));
        assertDoesNotThrow(() -> AnnotationRouteValidator.validate("root [--other:Integer] [--amount:Integer]", method, List.of(
            context,
            MethodCommandBinder.ParameterBinding.option("other", Integer.class, null,
                MethodCommandBinder.SuggestionBinding.empty(), "Option"),
            MethodCommandBinder.ParameterBinding.option("amount", Integer.class, null,
                MethodCommandBinder.SuggestionBinding.empty(), "Option")
        )));
        assertDoesNotThrow(() -> AnnotationRouteValidator.validate("root [--other] [--silent]", method, List.of(
            context,
            MethodCommandBinder.ParameterBinding.flag("other", null, "Flag"),
            MethodCommandBinder.ParameterBinding.flag("silent", null, "Flag")
        )));
        assertEquals(target.getClass(), method.getDeclaringClass());
    }

    @Test
    void rejectsInvalidCommandMethodsAndParameters() throws Exception {
        assertBindError(InvalidMethodCommands.class, "privateCommand",
            "annotated command method must be public or package-private: privateCommand");
        assertBindError(InvalidMethodCommands.class, "protectedCommand",
            "annotated command method must be public or package-private: protectedCommand");
        assertBindError(InvalidMethodCommands.class, "voidCommand",
            "annotated command method must return CommandResult: voidCommand");
        assertBindError(InvalidMethodCommands.class, "multipleAnnotations", "unsupported annotated command parameter: x",
            boolean.class);
        assertBindError(InvalidMethodCommands.class, "wrongRouteContext",
            "@RouteCtx parameter must be CommandContext: routeContext", String.class);
        assertBindError(InvalidMethodCommands.class, "stringFlag", "unsupported annotated command parameter: silent",
            String.class);
        assertBindError(InvalidMethodCommands.class, "contextArgument", "unsupported annotated command parameter: context",
            CommandContext.class);
        assertBindError(InvalidMethodCommands.class, "contextOption", "unsupported annotated command parameter: context",
            CommandContext.class);
        assertBindError(InvalidMethodCommands.class, "contextFlag", "unsupported annotated command parameter: context",
            CommandContext.class);
        assertBindError(InvalidMethodCommands.class, "optionalPlain", "unsupported annotated command parameter: value",
            Optional.class);
        assertBindError(InvalidMethodCommands.class, "unsupportedPlain", "unsupported annotated command parameter: value",
            List.class);
        assertBindError(InvalidMethodCommands.class, "rawOptional", "unsupported annotated command parameter: amount",
            Optional.class);
        assertBindError(InvalidMethodCommands.class, "nestedOptional", "unsupported annotated command parameter: amount",
            Optional.class);
        assertBindError(InvalidMethodCommands.class, "unsupportedOptional",
            "unsupported annotated command parameter: amount", Optional.class);
        assertBindError(InvalidMethodCommands.class, "missingSuggestion",
            "suggestion provider not found: missing", String.class);
        assertBindError(InvalidMethodCommands.class, "blankSuggestion",
            "suggestion provider must not be blank", String.class);
        assertBindError(InvalidMethodCommands.class, "multiAlias",
            "parameter alias must contain exactly one value: amount", Integer.class);
    }

    @Test
    void bindsSupportedArgumentOptionAndFlagShapes() throws Exception {
        Method method = method(SupportedBindingCommands.class, "all", String.class, Integer.class, int.class,
            Long.class, long.class, Double.class, double.class, Boolean.class, boolean.class, UUID.class, Mode.class,
            boolean.class, Boolean.class, String.class, String.class, Optional.class);

        List<MethodCommandBinder.ParameterBinding> bindings =
            MethodCommandBinder.bind(new SupportedBindingCommands(), method).bindings();

        assertEquals(16, bindings.size());
        assertEquals(MethodCommandBinder.Kind.FLAG, bindings.get(11).kind());
        assertEquals("f", bindings.get(12).alias());
        assertEquals(MethodCommandBinder.Kind.OPTION, bindings.get(13).kind());
        assertNull(bindings.get(14).alias());
        assertEquals(MethodCommandBinder.Kind.OPTIONAL_OPTION, bindings.get(15).kind());
        assertEquals(Long.class, bindings.get(15).type());
    }

    @Test
    void rejectsInferredParametersWhenClassWasCompiledWithoutParameterNames() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);

        Path output = Files.createTempDirectory("annotation-binding-test");
        Path source = output.resolve("NoParameterNames.java");
        Files.writeString(source, """
            package dynamic;

            import dev.riege.buildmycommand.api.CommandResult;
            import dev.riege.buildmycommand.api.Results;

            public class NoParameterNames {
                public CommandResult inferred(String value) {
                    return Results.success(value);
                }
            }
            """);

        int result = compiler.run(null, null, null,
            "-classpath", System.getProperty("java.class.path"),
            "-d", output.toString(),
            source.toString());
        assertEquals(0, result);

        try (URLClassLoader loader = new URLClassLoader(new URL[] {output.toUri().toURL()},
            AnnotationBindingCoverageTest.class.getClassLoader())) {
            Class<?> owner = Class.forName("dynamic.NoParameterNames", true, loader);
            Object target = owner.getDeclaredConstructor().newInstance();
            Method method = owner.getDeclaredMethod("inferred", String.class);

            assertFalse(method.getParameters()[0].isNamePresent());
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> MethodCommandBinder.bind(target, method));

            assertEquals("cannot infer annotated command parameter name: arg0", exception.getMessage());
        }
    }

    @Test
    void appliesBindingsAndReadsValuesFromCommandContext() {
        RecordingBuilder builder = new RecordingBuilder();
        MethodCommandBinder.ParameterBinding.argument("required", String.class, false, false, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").apply(builder);
        MethodCommandBinder.ParameterBinding.argument("optional", String.class, true, false, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").apply(builder);
        MethodCommandBinder.ParameterBinding.argument("greedy", String.class, false, true, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").apply(builder);
        MethodCommandBinder.ParameterBinding.argument("tail", String.class, true, true, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").apply(builder);
        MethodCommandBinder.ParameterBinding.flag("silent", "s", "Flag").apply(builder);
        MethodCommandBinder.ParameterBinding.option("amount", Integer.class, "a",
            MethodCommandBinder.SuggestionBinding.empty(), "Option").apply(builder);
        MethodCommandBinder.ParameterBinding.optionalOption("maybe", Long.class, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Option").apply(builder);

        assertIterableEquals(List.of("arg:required:String", "optional:optional:String", "greedy:greedy:String",
            "optionalGreedy:tail:String", "flag:silent:s", "option:amount:Integer:a", "option:maybe:Long:null"),
            builder.events);

        CommandContext context = context(Map.of(
            "required", "value",
            "silent", true,
            "amount", 3,
            "maybe", 7L
        ));
        assertSame(context, MethodCommandBinder.ParameterBinding.context().value(context));
        assertSame(context, MethodCommandBinder.ParameterBinding.routeContext().value(context));
        assertEquals("value", MethodCommandBinder.ParameterBinding.argument("required", String.class, false, false,
            null, MethodCommandBinder.SuggestionBinding.empty(), "Arg").value(context));
        assertEquals(true, MethodCommandBinder.ParameterBinding.flag("silent", null, "Flag").value(context));
        assertEquals(3, MethodCommandBinder.ParameterBinding.option("amount", Integer.class, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Option").value(context));
        assertEquals(Optional.of(7L), MethodCommandBinder.ParameterBinding.optionalOption("maybe", Long.class, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Option").value(context));
        assertNull(MethodCommandBinder.ParameterBinding.argument("missing", String.class, true, false, null,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").value(context));
    }

    @Test
    void parsesDefaultArgumentValuesForSupportedDefaultTypes() {
        CommandContext context = context(Map.of());

        assertEquals("text", defaulted(String.class, "text").value(context));
        assertEquals(1, defaulted(int.class, "1").value(context));
        assertEquals(1, defaulted(Integer.class, "1").value(context));
        assertEquals(2L, defaulted(long.class, "2").value(context));
        assertEquals(2L, defaulted(Long.class, "2").value(context));
        assertEquals(3.5, defaulted(double.class, "3.5").value(context));
        assertEquals(3.5, defaulted(Double.class, "3.5").value(context));
        assertEquals(true, defaulted(boolean.class, "true").value(context));
        assertEquals(true, defaulted(Boolean.class, "true").value(context));
        assertEquals("fallback", defaulted(UUID.class, "fallback").value(context));
        assertEquals(4, MethodCommandBinder.ParameterBinding.argument("missing", int.class, false, false, "4",
            MethodCommandBinder.SuggestionBinding.empty(), "Arg").value(context));
    }

    @Test
    void invokesBoundMethodsAndPropagatesFailureCauses() throws Exception {
        InvocationCommands target = new InvocationCommands();

        assertEquals(Optional.of("ok"), MethodCommandBinder.bind(target, method(InvocationCommands.class, "ok"))
            .invoke(context(Map.of())).reply());
        IllegalStateException runtime = assertThrows(IllegalStateException.class,
            () -> MethodCommandBinder.bind(target, method(InvocationCommands.class, "runtime")).invoke(context(Map.of())));
        assertEquals("runtime", runtime.getMessage());
        AssertionError error = assertThrows(AssertionError.class,
            () -> MethodCommandBinder.bind(target, method(InvocationCommands.class, "error")).invoke(context(Map.of())));
        assertEquals("error", error.getMessage());
        IllegalStateException checked = assertThrows(IllegalStateException.class,
            () -> MethodCommandBinder.bind(target, method(InvocationCommands.class, "checked")).invoke(context(Map.of())));
        assertEquals("annotated command method failed: checked", checked.getMessage());

        Method privateMethod = InvocationCommands.class.getDeclaredMethod("privateCommand");
        MethodCommandBinder.BoundMethod inaccessible = new MethodCommandBinder.BoundMethod(target, privateMethod,
            List.of(), new MethodCommandBinder.CommandMetadata(false, Optional.empty(), List.of(), Optional.empty(),
            Optional.empty()));
        IllegalStateException illegalAccess = assertThrows(IllegalStateException.class,
            () -> inaccessible.invoke(context(Map.of())));
        assertEquals("cannot invoke annotated command method: privateCommand", illegalAccess.getMessage());
    }

    @Test
    void invokesSuggestionProvidersForStringSuggestionAndFailureShapes() throws Exception {
        SuggestionCommands target = new SuggestionCommands();
        ArgumentParseContext context = parseContext("target", String.class);

        SuggestionProvider strings = binding(target, "strings", String.class).suggestionProviderFunction().orElseThrow();
        assertIterableEquals(List.of("Ada", "Bob"), strings.suggestions(context));
        assertEquals(List.of(
            new Suggestion("Ada", Optional.empty(), 4, 6, SuggestionType.ARGUMENT, 0),
            new Suggestion("Bob", Optional.empty(), 4, 6, SuggestionType.ARGUMENT, 0)
        ), strings.richSuggestions(context));
        assertEquals(List.of(), binding(target, "empty", String.class).suggestionProviderFunction().orElseThrow()
            .richSuggestions(context));
        assertEquals(List.of("Ada"), binding(target, "filteredOverloads", String.class)
            .suggestionProviderFunction().orElseThrow().suggestions(context));

        IllegalStateException nullList = assertThrows(IllegalStateException.class,
            () -> binding(target, "nullList", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("suggestion provider did not return a List: nullList", nullList.getMessage());
        IllegalStateException unsupported = assertThrows(IllegalStateException.class,
            () -> binding(target, "numbers", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("suggestion provider must return List<String> or List<Suggestion>: numbers",
            unsupported.getMessage());
        IllegalStateException firstNull = assertThrows(IllegalStateException.class,
            () -> binding(target, "firstNull", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("suggestion provider firstNull returned null at index 0", firstNull.getMessage());
    }

    @Test
    void invokesSuggestionProvidersForRichSuggestionAndExceptionShapes() throws Exception {
        SuggestionCommands target = new SuggestionCommands();
        ArgumentParseContext context = parseContext("target", String.class);

        SuggestionProvider rich = binding(target, "rich", String.class).suggestionProviderFunction().orElseThrow();
        assertEquals(List.of(new Suggestion("Ada", Optional.of("online"), 4, 6, SuggestionType.ARGUMENT, 5)),
            rich.richSuggestions(context));

        IllegalStateException mixed = assertThrows(IllegalStateException.class,
            () -> binding(target, "mixedRich", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("suggestion provider mixedRich returned mixed element at index 1", mixed.getMessage());
        IllegalArgumentException runtime = assertThrows(IllegalArgumentException.class,
            () -> binding(target, "runtime", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("runtime", runtime.getMessage());
        AssertionError error = assertThrows(AssertionError.class,
            () -> binding(target, "error", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("error", error.getMessage());
        IllegalStateException checked = assertThrows(IllegalStateException.class,
            () -> binding(target, "checked", String.class).suggestionProviderFunction().orElseThrow()
                .richSuggestions(context));
        assertEquals("suggestion provider failed: checked", checked.getMessage());

        SuggestionProvider inaccessible = binding(target, "privateStrings", String.class)
            .suggestionProviderFunction().orElseThrow();
        providerMethod(inaccessible).setAccessible(false);
        IllegalStateException illegalAccess = assertThrows(IllegalStateException.class,
            () -> inaccessible.richSuggestions(context));
        assertEquals("cannot invoke suggestion provider: privateStrings", illegalAccess.getMessage());
    }

    @Test
    void compilerExposesRootAliasesMethodAliasesCasePolicyAndOrdering() {
        AnnotationCommandCompiler.CompiledCommands compiled =
            AnnotationCommandCompiler.compile(new CompilerHappyPathCommands());

        assertEquals(new AnnotationCommandCompiler.CasePolicy(true, false), compiled.classCasePolicy());
        assertEquals(Optional.of("tools"), compiled.rootCommand().orElseThrow().group());
        assertIterableEquals(List.of("t", "tool"), compiled.rootCommand().orElseThrow().aliases());
        assertIterableEquals(List.of("toolkit|kit [--amount:Integer]", "tools|t|tool admin", "tools", "z"),
            compiled.commands().stream().map(AnnotationCommandCompiler.CompiledCommand::route).toList());
        assertEquals(new AnnotationCommandCompiler.CasePolicy(false, true), compiled.commands().get(0).casePolicy());
        assertIterableEquals(List.of("c"), compiled.commands().get(2).subcommandAliases());

        assertIterableEquals(List.of("same", "same"), AnnotationCommandCompiler.compile(new OverloadedCommands())
            .commands().stream().map(AnnotationCommandCompiler.CompiledCommand::route).toList());
    }

    @Test
    void compilerRecognizesEveryRootMetadataTrigger() {
        assertRootCommand(new RootPermissionCommands());
        assertRootCommand(new RootHiddenCommands());
        assertRootCommand(new RootUsageCommands());
        assertRootCommand(new RootExampleCommands());
        assertRootCommand(new RootCooldownCommands());
        assertRootCommand(new RootRequireCommands());
        assertRootCommand(new RootGroupOnlyCommands());
        assertFalse(AnnotationCommandCompiler.compile(new RootWithoutMetadataCommands()).rootCommand().isPresent());
    }

    @Test
    void compilerRegistersCommandSubcommandAndRouteBranches() {
        RecordingRegistry registry = new RecordingRegistry();

        AnnotationCommandCompiler.compile(new CompilerHappyPathCommands()).register(registry);

        assertIterableEquals(List.of(
            "case:literals",
            "case:literals",
            "command:tools",
            "aliases:t,tool",
            "description:Tools root",
            "hidden",
            "usage:tools",
            "example:tools help",
            "cooldown:PT2S",
            "require:tools.use",
            "group:tools",
            "case:options",
            "route:toolkit|kit [--amount:Integer]",
            "hidden",
            "require:tools.use",
            "group:tools",
            "executesRoute",
            "route:tools|t|tool admin",
            "description:Admin route",
            "hidden",
            "usage:tools admin",
            "example:tools admin",
            "cooldown:PT3S",
            "require:tools.admin",
            "group:tools",
            "executesRoute",
            "command:tools",
            "aliases:t,tool",
            "subcommand:config",
            "description:Config command",
            "hidden",
            "require:tools.use",
            "group:tools",
            "aliases:c",
            "arg:name:String",
            "argSuggest:name:players",
            "option:amount:Integer:a",
            "optionSuggest:amount:amounts",
            "option:limit:Long:null",
            "optionSuggest:limit:amounts",
            "executes",
            "command:z",
            "hidden",
            "require:tools.use",
            "group:tools",
            "arg:name:String",
            "executes"
        ), registry.events);
    }

    @Test
    void compilerRejectsInvalidMetadataAliasesAndLiterals() {
        assertCompileError(new BlankGroupCommands(), "command group must not be blank");
        assertCompileError(new BlankClassUsageCommands(), "usage must not be blank");
        assertCompileError(new BlankClassExampleCommands(), "example must not be blank");
        assertCompileError(new BlankClassRequirementCommands(), "requirement must not be blank");
        assertCompileError(new ZeroClassCooldownCommands(), "cooldown must be positive");
        assertCompileError(new ZeroMethodCooldownCommands(), "cooldown must be positive");
        assertCompileError(new BlankMethodUsageCommands(), "usage must not be blank");
        assertCompileError(new BlankMethodExampleCommands(), "example must not be blank");
        assertCompileError(new BlankMethodRequirementCommands(), "requirement must not be blank");
        assertCompileError(new BlankRouteAliasCommands(), "route alias must not be blank");
        assertCompileError(new LongRouteAliasCommands(), "route alias is longer than route: a b c");
        assertCompileError(new ArgumentRouteAliasCommands(), "route alias can only target literal tokens: alt value");
        assertCompileError(new OptionRouteAliasCommands(), "route alias can only target literal tokens: alt value");
        assertCompileError(new BlankLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL:  ");
        assertCompileError(new LeadingSpaceLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL:  bad");
        assertCompileError(new LessThanLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL: bad<");
        assertCompileError(new GreaterThanLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL: bad>");
        assertCompileError(new BracketLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL: bad[");
        assertCompileError(new ClosingBracketLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL: bad]");
        assertCompileError(new BarLiteralCommands(),
            "@Command only accepts one literal; use @Route for route DSL: bad|alias");
    }

    private static void assertRouteError(
        String route,
        Object target,
        String methodName,
        String message,
        Class<?>... parameterTypes
    ) throws Exception {
        Method method = method(target.getClass(), methodName, parameterTypes);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validate(route, method, MethodCommandBinder.bind(target, method).bindings()));

        assertEquals(message, exception.getMessage());
    }

    private static void assertRouteContextError(
        Method method,
        MethodCommandBinder.ParameterBinding routeContext,
        MethodCommandBinder.ParameterBinding invalidBinding,
        String message
    ) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root", method, List.of(routeContext, invalidBinding)));

        assertEquals(message, exception.getMessage());
    }

    private static void assertBindError(
        Class<?> owner,
        String methodName,
        String message,
        Class<?>... parameterTypes
    ) throws Exception {
        Object target = owner.getDeclaredConstructor().newInstance();
        Method method = method(owner, methodName, parameterTypes);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> MethodCommandBinder.bind(target, method));

        assertEquals(message, exception.getMessage());
    }

    private static void assertCompileError(Object commands, String message) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> AnnotationCommandCompiler.compile(commands));

        assertEquals(message, exception.getMessage());
    }

    private static void assertRootCommand(Object commands) {
        assertEquals(true, AnnotationCommandCompiler.compile(commands).rootCommand().isPresent());
    }

    private static MethodCommandBinder.ParameterBinding binding(
        Object target,
        String methodName,
        Class<?>... parameterTypes
    ) throws Exception {
        return MethodCommandBinder.bind(target, method(target.getClass(), methodName, parameterTypes)).bindings().get(0);
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameterTypes) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static CommandContext context(Map<String, Object> arguments) {
        return new CommandContext(source(), "input", arguments);
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }

    private static ArgumentParseContext parseContext(String name, Class<?> type) {
        CommandInput input = CommandInput.raw(source(), "cmd ");
        return new ArgumentParseContext(source(), input, name, type, "", 4, 6, SuggestionType.ARGUMENT);
    }

    private static MethodCommandBinder.ParameterBinding defaulted(Class<?> type, String value) {
        return MethodCommandBinder.ParameterBinding.argument("missing", type, true, false, value,
            MethodCommandBinder.SuggestionBinding.empty(), "Arg");
    }

    private static Method providerMethod(SuggestionProvider provider) throws Exception {
        for (Field field : provider.getClass().getDeclaredFields()) {
            if (field.getType() == Method.class) {
                field.setAccessible(true);
                return (Method) field.get(provider);
            }
        }
        throw new AssertionError("provider method field not found");
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }

    enum Mode {
        FAST
    }

    static final class ValidatorCommands {
        CommandResult all(
            @Arg("target") String target,
            @Arg("count") int count,
            @Arg("total") long total,
            @Arg("ratio") double ratio,
            @Arg("enabled") boolean enabled,
            @Arg("id") UUID id,
            @Arg("optional") @OptionalArg String optional,
            @Arg("greedy") @Greedy String greedy,
            @Arg("tail") @OptionalArg @Greedy String tail,
            @Option("amount") Integer amount,
            @Option("maybe") Optional<Long> maybe,
            @Flag("silent") Boolean silent
        ) {
            return Results.silent();
        }

        CommandResult noParameters() {
            return Results.silent();
        }

        CommandResult route(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }

        CommandResult argumentOnly(@Arg("target") String target) {
            return Results.success(target);
        }

        CommandResult inferred(String target) {
            return Results.success(target);
        }

        CommandResult optionalArgument(@Arg("target") @OptionalArg String target) {
            return Results.success(target);
        }

        CommandResult greedyArgument(@Arg("target") @Greedy String target) {
            return Results.success(target);
        }

        CommandResult optionOnly(@Option("amount") Integer amount) {
            return Results.success(String.valueOf(amount));
        }

        CommandResult flagOnly(@Flag("silent") boolean silent) {
            return Results.success(String.valueOf(silent));
        }
    }

    static final class InvalidMethodCommands {
        @Command("private")
        private CommandResult privateCommand() {
            return Results.silent();
        }

        @Command("protected")
        protected CommandResult protectedCommand() {
            return Results.silent();
        }

        @Command("void")
        void voidCommand() {
        }

        CommandResult multipleAnnotations(@Arg("x") @Flag("x") boolean value) {
            return Results.success(String.valueOf(value));
        }

        CommandResult wrongRouteContext(@RouteCtx String context) {
            return Results.success(context);
        }

        CommandResult stringFlag(@Flag("silent") String silent) {
            return Results.success(silent);
        }

        CommandResult contextArgument(@Arg("context") CommandContext context) {
            return Results.success(context.input());
        }

        CommandResult contextOption(@Option("context") CommandContext context) {
            return Results.success(context.input());
        }

        CommandResult contextFlag(@Flag("context") CommandContext context) {
            return Results.success(context.input());
        }

        CommandResult optionalPlain(Optional<String> value) {
            return Results.success(String.valueOf(value));
        }

        CommandResult unsupportedPlain(List<String> value) {
            return Results.success(String.valueOf(value));
        }

        CommandResult rawOptional(@Option("amount") @SuppressWarnings("rawtypes") Optional amount) {
            return Results.success(String.valueOf(amount));
        }

        CommandResult nestedOptional(@Option("amount") Optional<List<String>> amount) {
            return Results.success(String.valueOf(amount));
        }

        CommandResult unsupportedOptional(@Option("amount") Optional<Thread> amount) {
            return Results.success(String.valueOf(amount));
        }

        CommandResult missingSuggestion(@Arg("target") @Suggest("missing") String target) {
            return Results.success(target);
        }

        CommandResult blankSuggestion(@Arg("target") @Suggest(" ") String target) {
            return Results.success(target);
        }

        CommandResult multiAlias(@Option("amount") @Alias({"a", "b"}) Integer amount) {
            return Results.success(String.valueOf(amount));
        }
    }

    static final class SupportedBindingCommands {
        CommandResult all(
            @Arg("text") String text,
            @Arg("boxedInt") Integer boxedInt,
            @Arg("primitiveInt") int primitiveInt,
            @Arg("boxedLong") Long boxedLong,
            @Arg("primitiveLong") long primitiveLong,
            @Arg("boxedDouble") Double boxedDouble,
            @Arg("primitiveDouble") double primitiveDouble,
            @Arg("boxedBoolean") Boolean boxedBoolean,
            @Arg("primitiveBoolean") boolean primitiveBoolean,
            @Arg("id") UUID id,
            @Arg("mode") Mode mode,
            @Flag("silent") boolean silent,
            @Flag("force") @Alias("f") Boolean force,
            @Option("name") String name,
            @Option("emptyAlias") @Alias({}) String emptyAlias,
            @Option("amount") Optional<Long> amount
        ) {
            return Results.silent();
        }
    }

    static final class InvocationCommands {
        CommandResult ok() {
            return Results.success("ok");
        }

        CommandResult runtime() {
            throw new IllegalStateException("runtime");
        }

        CommandResult error() {
            throw new AssertionError("error");
        }

        CommandResult checked() {
            sneakyThrow(new IOException("checked"));
            return Results.silent();
        }

        private CommandResult privateCommand() {
            return Results.silent();
        }
    }

    static final class SuggestionCommands {
        CommandResult strings(@Arg("target") @Suggest("strings") String target) {
            return Results.success(target);
        }

        List<String> strings() {
            return List.of("Ada", "Bob");
        }

        CommandResult empty(@Arg("target") @Suggest("empty") String target) {
            return Results.success(target);
        }

        List<String> empty() {
            return List.of();
        }

        CommandResult nullList(@Arg("target") @Suggest("nullList") String target) {
            return Results.success(target);
        }

        List<String> nullList() {
            return null;
        }

        CommandResult numbers(@Arg("target") @Suggest("numbers") String target) {
            return Results.success(target);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        List numbers() {
            return List.of(1);
        }

        CommandResult firstNull(@Arg("target") @Suggest("firstNull") String target) {
            return Results.success(target);
        }

        List<String> firstNull() {
            List<String> values = new ArrayList<>();
            values.add(null);
            return values;
        }

        CommandResult filteredOverloads(@Arg("target") @Suggest("filtered") String target) {
            return Results.success(target);
        }

        String filtered() {
            return "ignored";
        }

        List<String> filtered(String ignored) {
            return List.of(ignored);
        }

        List<String> filtered(String first, String second) {
            return List.of(first, second);
        }

        List<String> filtered(ArgumentParseContext context) {
            return List.of("Ada");
        }

        CommandResult rich(@Arg("target") @Suggest("rich") String target) {
            return Results.success(target);
        }

        List<Suggestion> rich(ArgumentParseContext context) {
            return List.of(new Suggestion("Ada", Optional.of("online"), context.replacementStart(),
                context.replacementEnd(), context.suggestionType(), 5));
        }

        CommandResult mixedRich(@Arg("target") @Suggest("mixedRich") String target) {
            return Results.success(target);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        List mixedRich() {
            return List.of(new Suggestion("Ada", Optional.empty(), 0, 0, SuggestionType.ARGUMENT, 0), "Bob");
        }

        CommandResult runtime(@Arg("target") @Suggest("runtime") String target) {
            return Results.success(target);
        }

        List<String> runtime() {
            throw new IllegalArgumentException("runtime");
        }

        CommandResult error(@Arg("target") @Suggest("error") String target) {
            return Results.success(target);
        }

        List<String> error() {
            throw new AssertionError("error");
        }

        CommandResult checked(@Arg("target") @Suggest("checked") String target) {
            return Results.success(target);
        }

        List<String> checked() {
            sneakyThrow(new IOException("checked"));
            return List.of();
        }

        CommandResult privateStrings(@Arg("target") @Suggest("privateStrings") String target) {
            return Results.success(target);
        }

        private List<String> privateStrings() {
            return List.of("Ada");
        }
    }

    @Command("tools")
    @Alias({"t", "tool"})
    @CommandGroup("tools")
    @CaseInsensitive(literals = true, options = false)
    @Description("Tools root")
    @Hidden
    @Usage("tools")
    @Example("tools help")
    @Cooldown(value = 2)
    @Require("tools.use")
    static final class CompilerHappyPathCommands {
        @Route("toolkit [--amount:Integer]")
        @Alias("kit")
        @CaseInsensitive(literals = false, options = true)
        CommandResult standalone(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }

        @SubRoute("admin")
        @Description("Admin route")
        @Hidden
        @Usage("tools admin")
        @Example("tools admin")
        @Cooldown(value = 3)
        @Require("tools.admin")
        CommandResult admin(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }

        @Subcommand("config")
        @Alias("c")
        @Description("Config command")
        CommandResult config(
            @Arg("name") @Suggest("players") String name,
            @Option("amount") @Alias("a") @Suggest("amounts") Integer amount,
            @Option("limit") @Suggest("amounts") Optional<Long> limit
        ) {
            return Results.success(name + amount);
        }

        @Command("z")
        CommandResult literalCommand(@Arg("name") String name) {
            return Results.success(name);
        }

        List<String> players() {
            return List.of("Ada");
        }

        List<String> amounts() {
            return List.of("1");
        }
    }

    @Command("root")
    @CommandGroup(" ")
    static final class BlankGroupCommands {
        @Subcommand("leaf")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    @Command("root")
    @Usage(" ")
    static final class BlankClassUsageCommands {
        @Subcommand("leaf")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    @Command("root")
    @Example(" ")
    static final class BlankClassExampleCommands {
        @Subcommand("leaf")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    @Command("root")
    @Require(" ")
    static final class BlankClassRequirementCommands {
        @Subcommand("leaf")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    @Command("root")
    @Cooldown(0)
    static final class ZeroClassCooldownCommands {
        @Subcommand("leaf")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    static final class ZeroMethodCooldownCommands {
        @Command("leaf")
        @Cooldown(0)
        CommandResult leaf() {
            return Results.silent();
        }
    }

    static final class BlankMethodUsageCommands {
        @Command("leaf")
        @Usage(" ")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    static final class BlankMethodExampleCommands {
        @Command("leaf")
        @Example(" ")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    static final class BlankMethodRequirementCommands {
        @Command("leaf")
        @Require(" ")
        CommandResult leaf() {
            return Results.silent();
        }
    }

    static final class BlankRouteAliasCommands {
        @Route("root")
        @Alias(" ")
        CommandResult root(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }
    }

    static final class LongRouteAliasCommands {
        @Route("root child")
        @Alias("a b c")
        CommandResult root(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }
    }

    static final class ArgumentRouteAliasCommands {
        @Route("root <target:String>")
        @Alias("alt value")
        CommandResult root(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }
    }

    static final class OptionRouteAliasCommands {
        @Route("root [--flag]")
        @Alias("alt value")
        CommandResult root(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }
    }

    static final class LeadingSpaceLiteralCommands {
        @Command(" bad")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class BlankLiteralCommands {
        @Command(" ")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class LessThanLiteralCommands {
        @Command("bad<")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class GreaterThanLiteralCommands {
        @Command("bad>")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class BracketLiteralCommands {
        @Command("bad[")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class ClosingBracketLiteralCommands {
        @Command("bad]")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class BarLiteralCommands {
        @Command("bad|alias")
        CommandResult bad() {
            return Results.silent();
        }
    }

    static final class OverloadedCommands {
        @Command("same")
        CommandResult same(String value) {
            return Results.success(value);
        }

        @Command("same")
        CommandResult same(Integer value, String other) {
            return Results.success(value + other);
        }
    }

    @Command("root")
    @Permission("root.use")
    static final class RootPermissionCommands {
    }

    @Command("root")
    @Hidden
    static final class RootHiddenCommands {
    }

    @Command("root")
    @Usage("root")
    static final class RootUsageCommands {
    }

    @Command("root")
    @Example("root")
    static final class RootExampleCommands {
    }

    @Command("root")
    @Cooldown(1)
    static final class RootCooldownCommands {
    }

    @Command("root")
    @Require("root.use")
    static final class RootRequireCommands {
    }

    @Command("root")
    @CommandGroup("roots")
    static final class RootGroupOnlyCommands {
    }

    @Command("root")
    static final class RootWithoutMetadataCommands {
    }

    private static final class RecordingRegistry implements CommandRegistry {
        final List<String> events = new ArrayList<>();

        @Override
        public CommandRegistry caseInsensitiveLiterals() {
            events.add("case:literals");
            return this;
        }

        @Override
        public CommandRegistry caseInsensitiveOptions() {
            events.add("case:options");
            return this;
        }

        @Override
        public void command(String literal, Consumer<CommandBuilder> configure) {
            events.add("command:" + literal);
            configure.accept(new RecordingBuilder(events));
        }

        @Override
        public void register(CommandNode node) {
            events.add("node");
        }

        @Override
        public RouteBuilder route(String pattern) {
            events.add("route:" + pattern);
            return new RecordingRouteBuilder(events);
        }
    }

    private static final class RecordingRouteBuilder implements CommandRegistry.RouteBuilder {
        private final List<String> events;

        RecordingRouteBuilder(List<String> events) {
            this.events = events;
        }

        @Override
        public CommandRegistry.RouteBuilder description(String description) {
            events.add("description:" + description);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder permission(String permission) {
            events.add("permission:" + permission);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder hidden() {
            events.add("hidden");
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder usage(String usage) {
            events.add("usage:" + usage);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder example(String example) {
            events.add("example:" + example);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder cooldown(Duration cooldown) {
            events.add("cooldown:" + cooldown);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder requirement(String requirement) {
            events.add("require:" + requirement);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder group(String group) {
            events.add("group:" + group);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder argumentSuggestions(
            String name,
            String providerName,
            SuggestionProvider provider
        ) {
            events.add("argSuggest:" + name + ":" + providerName);
            return this;
        }

        @Override
        public CommandRegistry.RouteBuilder optionSuggestions(
            String name,
            String providerName,
            SuggestionProvider provider
        ) {
            events.add("optionSuggest:" + name + ":" + providerName);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
            events.add("executesRoute");
            return new RecordingBuilder(events);
        }
    }

    private static final class RecordingBuilder implements CommandRegistry.CommandBuilder {
        final List<String> events;

        RecordingBuilder() {
            this(new ArrayList<>());
        }

        RecordingBuilder(List<String> events) {
            this.events = events;
        }

        @Override
        public CommandRegistry.CommandBuilder description(String description) {
            events.add("description:" + description);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder permission(String permission) {
            events.add("permission:" + permission);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder hidden() {
            events.add("hidden");
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder usage(String usage) {
            events.add("usage:" + usage);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder example(String example) {
            events.add("example:" + example);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder cooldown(Duration cooldown) {
            events.add("cooldown:" + cooldown);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder requirement(String requirement) {
            events.add("require:" + requirement);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder group(String group) {
            events.add("group:" + group);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder alias(String alias) {
            events.add("alias:" + alias);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder aliases(String... aliases) {
            events.add("aliases:" + String.join(",", aliases));
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder subcommand(
            String literal,
            Consumer<CommandRegistry.CommandBuilder> configure
        ) {
            events.add("subcommand:" + literal);
            configure.accept(new RecordingBuilder(events));
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder argument(String name, Class<T> type) {
            events.add("arg:" + name + ":" + type.getSimpleName());
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalArgument(String name, Class<T> type) {
            events.add("optional:" + name + ":" + type.getSimpleName());
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder greedyArgument(String name, Class<T> type) {
            events.add("greedy:" + name + ":" + type.getSimpleName());
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder optionalGreedyArgument(String name, Class<T> type) {
            events.add("optionalGreedy:" + name + ":" + type.getSimpleName());
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name) {
            events.add("flag:" + name);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder flag(String name, String alias) {
            events.add("flag:" + name + ":" + alias);
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type) {
            events.add("option:" + name + ":" + type.getSimpleName());
            return this;
        }

        @Override
        public <T> CommandRegistry.CommandBuilder option(String name, Class<T> type, String alias) {
            events.add("option:" + name + ":" + type.getSimpleName() + ":" + alias);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder argumentSuggestions(
            String name,
            String providerName,
            SuggestionProvider provider
        ) {
            events.add("argSuggest:" + name + ":" + providerName);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder optionSuggestions(
            String name,
            String providerName,
            SuggestionProvider provider
        ) {
            events.add("optionSuggest:" + name + ":" + providerName);
            return this;
        }

        @Override
        public CommandRegistry.CommandBuilder executes(CommandRegistry.CommandExecutor executor) {
            events.add("executes");
            return this;
        }
    }
}
