package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the single context parameter consumed by a {@link Route} or {@link SubRoute}
 * method.
 *
 * <p>The parameter type must be {@code dev.riege.buildmycommand.api.CommandContext}.
 * When route DSL is used, this context is the canonical way to access arguments,
 * options, flags, source, input, path metadata, and platform information. Do not
 * duplicate route arguments as separate Java parameters.</p>
 *
 * <pre>{@code
 * @SubRoute("punish <target:String> [--silent|-s]")
 * CommandResult punish(@RouteCtx CommandContext route) {
 *     String target = route.arg("target", String.class);
 *     boolean silent = route.flag("silent");
 *     return Results.success(target + " silent=" + silent);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RouteCtx {
}
