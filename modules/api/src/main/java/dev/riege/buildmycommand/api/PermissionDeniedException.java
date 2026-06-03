package dev.riege.buildmycommand.api;

import java.util.Objects;

public final class PermissionDeniedException extends CommandException {
    private final String permission;

    public PermissionDeniedException(String permission) {
        super("Missing permission: " + Objects.requireNonNull(permission, "permission"));
        this.permission = permission;
    }

    public String permission() {
        return permission;
    }
}
