package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a method as a named suggestion provider.
 *
 * <p>The scanner uses this annotation to connect route DSL argument or option names
 * to Java suggestion logic. Suggestion methods should be small, deterministic, and
 * side-effect free whenever possible. They can return plain strings or richer
 * framework suggestion types depending on the supported method signature.</p>
 *
 * <p>The name should match an argument or option name declared in {@link Route} or
 * {@link SubRoute}, or a provider name referenced through builder metadata.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Suggest {
    /**
     * Argument, option, or provider name served by the annotated method.
     */
    String value();
}
