package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides or supplements the displayed usage line for a command or group.
 *
 * <p>Route DSL already contains enough structure to generate usage automatically.
 * Use {@code @Usage} when the generated form needs platform-specific prefixes,
 * user-facing wording, or compatibility text that should appear exactly as written.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @SubRoute("punish <target:String> <reason:String...>")
 * @Usage("/mod punish <target> <reason>")
 * CommandResult punish(@RouteCtx CommandContext route) {
 *     return Results.success("punished");
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Usage {
    /**
     * Usage text shown by help output.
     */
    String value();
}
