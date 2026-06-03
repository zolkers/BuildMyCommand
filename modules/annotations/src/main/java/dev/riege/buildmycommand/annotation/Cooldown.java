package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Adds a cooldown to a command group or executable route.
 *
 * <p>The cooldown middleware uses this metadata to reject repeated execution until
 * the duration has elapsed for the relevant source/scope. Apply it to a group to
 * protect a subtree, or to a route method to protect only that command leaf.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Cooldown {
    /**
     * Cooldown amount in the selected {@link #unit()}.
     */
    long value();

    /**
     * Time unit used by {@link #value()}.
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
