package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a literal subcommand group or leaf in the legacy/nested annotation model.
 *
 * <p>The value must be a single literal, not route DSL. Aliases belong in
 * {@link Alias}. Deeply nested {@code @Subcommand} classes are still supported for
 * users who prefer a class-per-node tree, but the recommended style for most command
 * paths is {@link Command} on the root class and {@link SubRoute} on executable
 * methods.</p>
 *
 * <p>Use this annotation for coarse grouping only, for example when a nested class
 * genuinely owns shared middleware or metadata. For regular paths like
 * {@code team member permission grant <target:String>}, prefer one
 * {@code @SubRoute("member permission grant <target:String>")} method.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Subcommand {
    /**
     * A single subcommand literal. Do not include spaces, aliases, arguments,
     * options, or route DSL markers.
     */
    String value();
}
