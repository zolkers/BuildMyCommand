package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a complete executable route DSL on a method.
 *
 * <p>{@code @Route} is the recommended annotation style when the method should own
 * the whole command path, including the root literal. The route DSL supports literal
 * aliases with {@code |}, required arguments with {@code <name:Type>}, optional
 * arguments with {@code [name:Type]}, greedy strings with {@code String...}, flags
 * with {@code [--flag|-f]}, and value options with {@code [--option:Type|-o]}.</p>
 *
 * <p>A route method must declare exactly one {@link RouteCtx} parameter of type
 * {@code dev.riege.buildmycommand.api.CommandContext}. Values are read from that
 * context instead of duplicating argument annotations on each parameter.</p>
 *
 * <pre>{@code
 * @Route("moderation|mod punish <target:String> <reason:String...> [--silent|-s]")
 * CommandResult punish(@RouteCtx CommandContext route) {
 *     return Results.success(route.arg("target", String.class));
 * }
 * }</pre>
 *
 * <p>Use {@link SubRoute} instead when the method lives inside a {@link Command}
 * class and the class already provides the root literal.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {
    /**
     * A complete route DSL string, starting with the top-level command literal.
     */
    String value();
}
