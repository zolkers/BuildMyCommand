package dev.riege.buildmycommand.api;

@FunctionalInterface
public interface CommandExceptionHandler {
    CommandResult handle(CommandExceptionContext context, Throwable error);
}
