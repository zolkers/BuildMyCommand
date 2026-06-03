package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Default;
import dev.riege.buildmycommand.annotation.Flag;
import dev.riege.buildmycommand.annotation.Option;
import dev.riege.buildmycommand.annotation.OptionalArg;
import dev.riege.buildmycommand.annotation.Suggest;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class AnnotationParameterExample {
    private AnnotationParameterExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new InventoryCommands());
        return framework;
    }

    static final class InventoryCommands {
        @Command("kit")
        CommandResult kit(
            @Arg("target") String target,
            @OptionalArg @Default("starter") @Suggest("kit-names") String kit,
            @Option("amount") @Default("1") Integer amount,
            @Flag("silent") boolean silent
        ) {
            return Results.success("Giving " + amount + "x " + kit + " to " + target + " silent=" + silent);
        }
    }
}
