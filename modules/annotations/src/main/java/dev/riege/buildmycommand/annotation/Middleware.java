package dev.riege.buildmycommand.annotation;

import dev.riege.buildmycommand.api.CommandMiddleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Middleware {
    Class<? extends CommandMiddleware>[] value();
}
