package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assigns a documentation/help grouping key to a command class.
 *
 * <p>Groups are metadata only: they do not change parsing, permissions, middleware,
 * aliases, or execution. Use them to organize generated help pages, examples, or
 * external documentation.</p>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 * @Command("admin")
 * @CommandGroup("moderation")
 * final class AdminModerationCommands {
 *     @SubRoute("punish <target:String>")
 *     CommandResult punish(@RouteCtx CommandContext route) {
 *         return Results.success("punished");
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandGroup {
    /**
     * Stable group key, for example {@code "moderation"} or {@code "admin"}.
     */
    String value();
}
