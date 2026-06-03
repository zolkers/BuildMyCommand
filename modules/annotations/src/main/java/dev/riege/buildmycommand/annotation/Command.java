package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a top-level command root for annotation scanning.
 *
 * <p>The value must be a single literal such as {@code "admin"} or {@code "wecc"}.
 * It is intentionally not a route DSL string. Put aliases in {@link Alias}, and put
 * deep command paths on methods with {@link SubRoute}. This keeps the class as a
 * stable command tree owner while the method route describes the actual executable
 * path.</p>
 *
 * <p>Recommended shape:</p>
 *
 * <pre>{@code
 * @Command("admin")
 * @Alias("a")
 * static final class AdminCommands {
 *     @SubRoute("moderation|mod punish <target:String> <reason:String...>")
 *     CommandResult punish(@RouteCtx CommandContext route) {
 *         return Results.success(route.arg("target", String.class));
 *     }
 * }
 * }</pre>
 *
 * <p>{@code @Command} can also be used on a method for a tiny root command, but a
 * class-level root plus {@link Route} or {@link SubRoute} is the canonical style for
 * non-trivial projects.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Command {
    /**
     * The canonical top-level command literal. Do not include spaces, aliases,
     * arguments, options, or route DSL markers here.
     */
    String value();
}
