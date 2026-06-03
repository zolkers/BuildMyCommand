package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds alternative literals for a {@link Command} or {@link Subcommand}.
 *
 * <p>For route DSL methods, prefer inline literal aliases such as
 * {@code @Route("ban|block <target:String>")} or
 * {@code @SubRoute("punish temporary|temp add <target:String>")}. Use this
 * annotation when the alias belongs to a class-level command root or a legacy
 * {@link Subcommand} node.</p>
 *
 * <p>Aliases are command literals, not permission aliases and not option aliases.
 * Option/flag aliases belong in route DSL with {@code |-s} or in builder calls such
 * as {@code flag("silent", "s")}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
public @interface Alias {
    /**
     * Alternative literals that should resolve to the same command node.
     */
    String[] value();
}
