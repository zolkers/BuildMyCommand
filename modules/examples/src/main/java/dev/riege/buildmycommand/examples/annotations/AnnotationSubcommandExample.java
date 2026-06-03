package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationSubcommandExample {
    private AnnotationSubcommandExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new ServerCommands());
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    @Command("server")
    @Alias("srv")
    static final class ServerCommands {
        @Subcommand("reload")
        @Permission("server.reload")
        CommandResult reload() {
            return Results.success("Server reloaded");
        }

        @Subcommand("status")
        CommandResult status() {
            return Results.success("Server online");
        }

        @Subcommand("maintenance enable")
        @Permission("server.maintenance")
        CommandResult enableMaintenance() {
            return Results.success("Maintenance enabled");
        }

        @Subcommand("maintenance disable")
        @Permission("server.maintenance")
        CommandResult disableMaintenance() {
            return Results.success("Maintenance disabled");
        }
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }
}
