package dev.riege.buildmycommand.examples.permissions;

import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Set;

public final class PermissionExample {
    private PermissionExample() {
    }

    public static CommandResult denied() {
        return create().dispatch(source(Set.of()), "secure reload");
    }

    public static CommandResult allowed() {
        return create().dispatch(source(Set.of("admin.reload", "staff")), "secure reload");
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .route("secure reload")
            .permission("admin.reload")
            .requirement("staff || owner")
            .executes(ctx -> Results.success("Reloaded"));
        return framework;
    }

    public static CommandSource source(Set<String> permissions) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return permissions.contains(permission);
            }
        };
    }
}
