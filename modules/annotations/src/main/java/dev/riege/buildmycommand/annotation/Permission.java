package dev.riege.buildmycommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires one permission node before a command can execute.
 *
 * <p>{@code @Permission} is deliberately simple: it represents a single permission
 * string such as {@code "admin.reload"} or {@code "team.member.grant"}. It is
 * checked through the command source permission contract. The annotation is inherited
 * through the command path according to the scanner/compiler rules: group-level
 * permissions can protect a whole subtree, while route-level permissions protect one
 * executable leaf.</p>
 *
 * <p>Do not put boolean expressions here. For expressions such as
 * {@code "staff || owner"} or {@code "staff && !banned"}, use {@link Require}
 * instead. The IntelliJ plugin intentionally warns when {@code @Permission} looks
 * like a boolean requirement.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Permission {
    /**
     * A single permission node.
     */
    String value();
}
