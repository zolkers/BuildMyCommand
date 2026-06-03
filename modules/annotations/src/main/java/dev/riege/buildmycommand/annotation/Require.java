package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires a boolean permission expression before a command can execute.
 *
 * <p>Use this when a single {@link Permission} node is not expressive enough. The
 * requirement DSL supports permission atoms, {@code &&}, {@code ||}, unary
 * {@code !}, and parentheses. Operator precedence is the usual boolean precedence:
 * negation first, then {@code &&}, then {@code ||}.</p>
 *
 * <pre>{@code
 * @Require("staff && (moderator || owner) && !banned")
 * @SubRoute("moderation audit <target:String>")
 * CommandResult audit(@RouteCtx CommandContext route) {
 *     return Results.success("ok");
 * }
 * }</pre>
 *
 * <p>Keep {@link Permission} for a single node and {@code @Require} for boolean
 * policy. This separation keeps help output, IDE validation, and runtime checks easy
 * to reason about.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Require {
    /**
     * Boolean permission expression evaluated against the command source.
     */
    String value();
}
