package dev.riege.buildmycommand.annotation.binding;

import dev.riege.buildmycommand.annotation.Cooldown;
import dev.riege.buildmycommand.annotation.Example;
import dev.riege.buildmycommand.annotation.Hidden;
import dev.riege.buildmycommand.annotation.Middleware;
import dev.riege.buildmycommand.annotation.Require;
import dev.riege.buildmycommand.annotation.Route;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.annotation.SuggestAliases;
import dev.riege.buildmycommand.annotation.Usage;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandMiddleware;
import dev.riege.buildmycommand.api.CommandNode;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationBindingCoverageTest {
    @Test
    void binderInvokesNoArgContextAndRouteContextMethods() throws Exception {
        InvocationCommands target = new InvocationCommands();
        CommandContext context = context(Map.of("target", "Ada"));

        assertEquals(Optional.of("plain"), MethodCommandBinder.bind(target, method(InvocationCommands.class, "plain"))
            .invoke(context).reply());
        assertEquals(Optional.of("ctx:raw"), MethodCommandBinder.bind(target, method(InvocationCommands.class, "context", CommandContext.class))
            .invoke(context).reply());
        assertEquals(Optional.of("route:Ada"), MethodCommandBinder.bind(target, method(InvocationCommands.class, "route", CommandContext.class))
            .invoke(context).reply());
        assertSame(context, MethodCommandBinder.bind(target, method(InvocationCommands.class, "route", CommandContext.class))
            .bindings().get(0).value(context));
    }

    @Test
    void binderValidatesMethodShapeAndParameters() throws Exception {
        InvalidCommands target = new InvalidCommands();

        assertBindError(target, "privateMethod", "annotated command method must be public or package-private: privateMethod");
        assertBindError(target, "protectedMethod", "annotated command method must be public or package-private: protectedMethod");
        assertBindError(target, "wrongReturn", "annotated command method must return CommandResult: wrongReturn");
        assertBindError(target, "wrongRouteContext", "@RouteCtx parameter must be CommandContext: routeContext", String.class);
        assertBindError(target, "unsupported",
            "annotated command methods only support CommandContext or @RouteCtx CommandContext parameters: value",
            String.class);
        assertThrows(NullPointerException.class, () -> MethodCommandBinder.bind(null, method(InvocationCommands.class, "plain")));
        assertThrows(NullPointerException.class, () -> MethodCommandBinder.bind(target, null));
    }

    @Test
    void binderSnapshotsMetadataMiddlewaresAndSuggestionProviders() throws Exception {
        MetadataCommands target = new MetadataCommands();

        MethodCommandBinder.BoundMethod bound = MethodCommandBinder.bind(target,
            method(MetadataCommands.class, "secret", CommandContext.class));
        MethodCommandBinder.CommandMetadata metadata = bound.metadata();

        assertTrue(metadata.hidden());
        assertEquals(Optional.of("secret <target>"), metadata.usage());
        assertEquals(List.of("secret Ada", "secret Bob"), metadata.examples());
        assertEquals(Optional.of(Duration.ofSeconds(2)), metadata.cooldown());
        assertEquals(Optional.of("staff"), metadata.requirement());
        assertFalse(metadata.suggestAliases());
        assertEquals(1, metadata.middlewares().size());
        assertEquals(List.of("target"), metadata.suggestions().stream().map(MethodCommandBinder.SuggestionBinding::name).toList());

        ArgumentParseContext parseContext = new ArgumentParseContext(
            source(),
            CommandInput.raw(source(), "secret "),
            "target",
            String.class,
            "",
            7,
            7,
            SuggestionType.ARGUMENT
        );
        assertEquals(List.of("Ada", "Bob"), metadata.suggestions().get(0).provider().suggestions(parseContext));
    }

    @Test
    void binderRejectsInvalidMetadataMiddlewareAndSuggestionProviders() throws Exception {
        assertBindError(new BadUsageCommands(), "bad", "usage must not be blank", CommandContext.class);
        assertBindError(new BadExampleCommands(), "bad", "example must not be blank", CommandContext.class);
        assertBindError(new BadCooldownCommands(), "bad", "cooldown must be positive", CommandContext.class);
        assertBindError(new BadRequireCommands(), "bad", "requirement must not be blank", CommandContext.class);
        assertBindError(new BadMiddlewareCommands(), "bad",
            "command middleware must declare a no-arg constructor: "
                + BadConstructorMiddleware.class.getName(), CommandContext.class);
        assertBindError(new BadSuggestionSignatureCommands(), "bad",
            "@Suggest provider must return List and accept zero args or ArgumentParseContext: broken",
            CommandContext.class);
    }

    @Test
    void suggestionProvidersValidateRuntimeValues() throws Exception {
        assertSuggestionError(new NullSuggestionCommands(), "suggestion provider provider returned null at index 1");
        assertSuggestionError(new MixedSuggestionCommands(), "suggestion provider provider returned mixed element at index 1");
        assertSuggestionError(new WrongSuggestionTypeCommands(),
            "suggestion provider must return List<String> or List<Suggestion>: provider");
        assertSuggestionError(new ThrowingSuggestionCommands(), "boom");
        assertSuggestionError(new CheckedSuggestionCommands(), "suggestion provider failed: provider");
    }

    @Test
    void binderCoversInvocationAndProviderFailureEdges() throws Exception {
        InvocationFailureCommands target = new InvocationFailureCommands();
        CommandContext context = context(Map.of());

        assertEquals(List.of(), provider(null, "strings").provider().suggestions(parseContext()));
        assertEquals(List.of("ctx:target"), providerWithContext(target, "withContext").provider().suggestions(parseContext()));
        assertEquals(List.of(new Suggestion("Rich", Optional.empty(), 4, 4, SuggestionType.ARGUMENT, 1)),
            provider(target, "rich").provider().richSuggestions(parseContext()));
        assertEquals("suggestion provider did not return a List: notAList",
            assertThrows(IllegalStateException.class, () -> invokePrivateProvider(target, "notAList"))
                .getMessage());
        assertEquals("cannot invoke suggestion provider: privateStrings",
            assertThrows(IllegalStateException.class, () -> providerNoAccess(target, "privateStrings")
                .provider().suggestions(parseContext())).getMessage());
        assertSame(InvocationFailureCommands.ERROR,
            assertThrows(Error.class, () -> invokePrivateProvider(target, "error")));

        Method checked = method(InvocationFailureCommands.class, "checked");
        Method runtime = method(InvocationFailureCommands.class, "runtime");
        Method error = method(InvocationFailureCommands.class, "errorCommand");
        Method hidden = InvocationFailureCommands.class.getDeclaredMethod("hidden");
        MethodCommandBinder.CommandMetadata metadata = MethodCommandBinder.bind(target,
            method(InvocationFailureCommands.class, "ok")).metadata();
        MethodCommandBinder.BoundMethod inaccessible = new MethodCommandBinder.BoundMethod(
            target,
            hidden,
            List.of(),
            metadata
        );

        assertEquals("annotated command method failed: checked",
            assertThrows(IllegalStateException.class,
                () -> MethodCommandBinder.bind(target, checked).invoke(context)).getMessage());
        assertEquals("runtime",
            assertThrows(IllegalStateException.class,
                () -> MethodCommandBinder.bind(target, runtime).invoke(context)).getMessage());
        assertSame(InvocationFailureCommands.ERROR,
            assertThrows(Error.class, () -> MethodCommandBinder.bind(target, error).invoke(context)));
        assertEquals("cannot invoke annotated command method: hidden",
            assertThrows(IllegalStateException.class, () -> inaccessible.invoke(context)).getMessage());
        assertBindError(new ThrowingMiddlewareCommands(), "bad",
            "cannot instantiate command middleware: " + ThrowingConstructorMiddleware.class.getName(),
            CommandContext.class);
    }

    @Test
    void routeValidatorRequiresSingleRouteContextAndRejectsAnalysisOnlyTypes() throws Exception {
        Method ok = method(InvocationCommands.class, "route", CommandContext.class);
        Method missing = method(InvocationCommands.class, "plain");
        Method plain = method(InvocationCommands.class, "context", CommandContext.class);
        Method multiple = method(InvalidCommands.class, "multipleRouteContexts", CommandContext.class, CommandContext.class);

        AnnotationRouteValidator.validateRouteContextUsage(
            "root <target:String> [--amount:Integer]",
            ok,
            MethodCommandBinder.bind(new InvocationCommands(), ok).bindings()
        );
        IllegalArgumentException missingError = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root", missing,
                MethodCommandBinder.bind(new InvocationCommands(), missing).bindings()));
        IllegalArgumentException plainError = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root", plain,
                MethodCommandBinder.bind(new InvocationCommands(), plain).bindings()));
        IllegalArgumentException multipleError = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root", multiple,
                MethodCommandBinder.bind(new InvalidCommands(), multiple).bindings()));
        IllegalArgumentException enumError = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root <mode:enum(a,b)>", ok,
                MethodCommandBinder.bind(new InvocationCommands(), ok).bindings()));
        IllegalArgumentException constrainedError = assertThrows(IllegalArgumentException.class,
            () -> AnnotationRouteValidator.validateRouteContextUsage("root [--amount:Integer{1..5}]", ok,
                MethodCommandBinder.bind(new InvocationCommands(), ok).bindings()));

        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: plain",
            missingError.getMessage());
        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: context",
            plainError.getMessage());
        assertEquals("@Route method must declare exactly one @RouteCtx CommandContext parameter: multipleRouteContexts",
            multipleError.getMessage());
        assertEquals("route argument mode uses analysis-only type enum(a,b)", enumError.getMessage());
        assertEquals("route option amount uses analysis-only type Integer{1..5}", constrainedError.getMessage());
    }

    @Test
    void utilityConstructorsArePrivateButCovered() throws Exception {
        Constructor<MethodCommandBinder> binder = MethodCommandBinder.class.getDeclaredConstructor();
        Constructor<AnnotationRouteValidator> validator = AnnotationRouteValidator.class.getDeclaredConstructor();
        Constructor<AnnotationRouteAliases> aliases = AnnotationRouteAliases.class.getDeclaredConstructor();
        binder.setAccessible(true);
        validator.setAccessible(true);
        aliases.setAccessible(true);

        assertNotNull(binder.newInstance());
        assertNotNull(validator.newInstance());
        assertNotNull(aliases.newInstance());
        assertThrows(NullPointerException.class, () -> new MethodCommandBinder.SuggestionBinding(null, ctx -> List.of()));
        assertThrows(NullPointerException.class, () -> new MethodCommandBinder.SuggestionBinding("x", null));
        assertEquals(MethodCommandBinder.Kind.CONTEXT, MethodCommandBinder.ParameterBinding.context().kind());
    }

    private static void assertSuggestionError(Object target, String message) throws Exception {
        Method method = method(target.getClass(), "bad", CommandContext.class);
        MethodCommandBinder.CommandMetadata metadata = MethodCommandBinder.bind(target, method).metadata();
        ArgumentParseContext context = new ArgumentParseContext(
            source(),
            CommandInput.raw(source(), "bad "),
            "target",
            String.class,
            "",
            4,
            4,
            SuggestionType.ARGUMENT
        );
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> metadata.suggestions().get(0).provider().richSuggestions(context));
        assertEquals(message, exception.getMessage());
    }

    private static void assertBindError(
        Object target,
        String methodName,
        String message,
        Class<?>... parameterTypes
    ) throws Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> MethodCommandBinder.bind(target, method(target.getClass(), methodName, parameterTypes)));
        assertEquals(message, exception.getMessage());
    }

    private static MethodCommandBinder.SuggestionBinding provider(Object target, String methodName) throws Exception {
        Method method = method(InvocationFailureCommands.class, methodName);
        method.setAccessible(true);
        return provider(target, method);
    }

    private static MethodCommandBinder.SuggestionBinding providerWithContext(Object target, String methodName) throws Exception {
        Method method = method(InvocationFailureCommands.class, methodName, ArgumentParseContext.class);
        method.setAccessible(true);
        return provider(target, method);
    }

    private static MethodCommandBinder.SuggestionBinding providerNoAccess(Object target, String methodName) throws Exception {
        return provider(target, InvocationFailureCommands.class.getDeclaredMethod(methodName));
    }

    private static MethodCommandBinder.SuggestionBinding provider(Object target, Method method) throws Exception {
        Method privateProvider = MethodCommandBinder.class.getDeclaredMethod("provider", Object.class, Method.class);
        privateProvider.setAccessible(true);
        return new MethodCommandBinder.SuggestionBinding(
            "x",
            (dev.riege.buildmycommand.api.SuggestionProvider) privateProvider.invoke(null, target, method)
        );
    }

    private static Object invokePrivateProvider(Object target, String methodName) throws Exception {
        return provider(target, methodName).provider().richSuggestions(parseContext());
    }

    private static ArgumentParseContext parseContext() {
        return new ArgumentParseContext(
            source(),
            CommandInput.raw(source(), "bad "),
            "target",
            String.class,
            "",
            4,
            4,
            SuggestionType.ARGUMENT
        );
    }

    private static CommandContext context(Map<String, Object> values) {
        return new CommandContext(source(), CommandInput.raw(source(), "raw"), values);
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameterTypes) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    static final class InvocationCommands {
        CommandResult plain() {
            return Results.success("plain");
        }

        CommandResult context(CommandContext context) {
            return Results.success("ctx:" + context.input());
        }

        CommandResult route(@RouteCtx CommandContext context) {
            return Results.success("route:" + context.arg("target", String.class));
        }
    }

    static final class InvalidCommands {
        private CommandResult privateMethod() {
            return Results.silent();
        }

        protected CommandResult protectedMethod() {
            return Results.silent();
        }

        String wrongReturn() {
            return "bad";
        }

        CommandResult wrongRouteContext(@RouteCtx String context) {
            return Results.success(context);
        }

        CommandResult unsupported(String value) {
            return Results.success(value);
        }

        CommandResult multipleRouteContexts(@RouteCtx CommandContext first, @RouteCtx CommandContext second) {
            return Results.success(first.input() + second.input());
        }
    }

    @Hidden
    @Require("staff")
    @SuggestAliases(false)
    @Middleware(RecordingMiddleware.class)
    static final class MetadataCommands {
        @Route("secret <target:String>")
        @Usage("secret <target>")
        @Example({"secret Ada", "secret Bob"})
        @Cooldown(value = 2)
        CommandResult secret(@RouteCtx CommandContext context) {
            return Results.success(context.input());
        }

        @Suggest("target")
        List<String> players() {
            return List.of("Ada", "Bob");
        }
    }

    static final class BadUsageCommands {
        @Usage(" ")
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class BadExampleCommands {
        @Example("")
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class BadCooldownCommands {
        @Cooldown(0)
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class BadRequireCommands {
        @Require(" ")
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class BadMiddlewareCommands {
        @Middleware(BadConstructorMiddleware.class)
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class BadSuggestionSignatureCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        String broken() {
            return "Ada";
        }
    }

    static final class NullSuggestionCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        List<String> provider() {
            return Arrays.asList("Ada", null);
        }
    }

    static final class MixedSuggestionCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        List<Object> provider() {
            return List.of("Ada", new Suggestion("Bob", Optional.empty(), 0, 0, SuggestionType.ARGUMENT, 0));
        }
    }

    static final class WrongSuggestionTypeCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        List<Integer> provider() {
            return List.of(1);
        }
    }

    static final class ThrowingSuggestionCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        List<String> provider() {
            throw new IllegalStateException("boom");
        }
    }

    static final class CheckedSuggestionCommands {
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }

        @Suggest("target")
        List<String> provider() throws Exception {
            throw new Exception("checked");
        }
    }

    static final class RecordingMiddleware implements CommandMiddleware {
        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }

    static final class InvocationFailureCommands {
        static final Error ERROR = new AssertionError("boom");

        CommandResult ok() {
            return Results.silent();
        }

        CommandResult checked() throws Exception {
            throw new Exception("checked");
        }

        CommandResult runtime() {
            throw new IllegalStateException("runtime");
        }

        CommandResult errorCommand() {
            throw ERROR;
        }

        private CommandResult hidden() {
            return Results.silent();
        }

        List<String> strings() {
            return List.of("Ada");
        }

        List<String> withContext(ArgumentParseContext context) {
            return List.of("ctx:" + context.name());
        }

        List<Suggestion> rich() {
            return List.of(new Suggestion("Rich", Optional.empty(), 4, 4, SuggestionType.ARGUMENT, 1));
        }

        private List<String> privateStrings() {
            return List.of("Ada");
        }

        String notAList() {
            return "Ada";
        }

        List<String> error() {
            throw ERROR;
        }
    }

    static final class ThrowingMiddlewareCommands {
        @Middleware(ThrowingConstructorMiddleware.class)
        CommandResult bad(@RouteCtx CommandContext context) {
            return Results.silent();
        }
    }

    static final class ThrowingConstructorMiddleware implements CommandMiddleware {
        ThrowingConstructorMiddleware() {
            throw new IllegalStateException("boom");
        }

        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }

    static final class BadConstructorMiddleware implements CommandMiddleware {
        BadConstructorMiddleware(String ignored) {
        }

        @Override
        public CommandResult execute(CommandContext context, CommandNode command, List<String> commandPath, Chain next) {
            return next.proceed(context);
        }
    }
}
