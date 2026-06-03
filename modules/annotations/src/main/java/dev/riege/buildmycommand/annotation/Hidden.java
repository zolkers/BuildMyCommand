package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hides a command or group from normal help and suggestions.
 *
 * <p>Hidden commands still parse and execute when the caller knows the input. Use
 * this for internal commands, compatibility aliases, diagnostics, or commands that
 * are intentionally discoverable through another UI instead of command help.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Hidden {
}
