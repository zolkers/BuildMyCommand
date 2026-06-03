package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Arg;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Flag;
import dev.riege.buildmycommand.annotation.Option;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

public final class NestedSubcommandExample {
    private NestedSubcommandExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.create();
        AnnotationCommandScanner.register(framework.registry(), new TeamCommands());
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    @Command("team")
    @Alias("t")
    @Description("Team management tree")
    static final class TeamCommands {
        @Subcommand("member")
        @Alias("m")
        @Description("Member operations")
        static final class MemberCommands {
            @Subcommand("permission")
            @Alias("perm")
            @Description("Member permission operations")
            static final class PermissionCommands {
                @Subcommand("grant")
                @Alias("g")
                @Permission("team.member.permission.grant")
                CommandResult grant(@Arg("target") String target, @Arg("permission") String permission) {
                    return Results.success("Granted " + permission + " to " + target);
                }

                @Subcommand("revoke")
                @Alias("r")
                @Permission("team.member.permission.revoke")
                CommandResult revoke(@Arg("target") String target, @Arg("permission") String permission) {
                    return Results.success("Revoked " + permission + " from " + target);
                }
            }

            @Subcommand("role")
            @Description("Member role operations")
            static final class RoleCommands {
                @Subcommand("set")
                @Permission("team.member.role.set")
                CommandResult set(
                    @Arg("target") String target,
                    @Arg("role") String role,
                    @Option("priority") Integer priority,
                    @Flag("temporary") boolean temporary
                ) {
                    int effectivePriority = priority == null ? 0 : priority;
                    return Results.success(
                        "Set "
                            + target
                            + " role="
                            + role
                            + " priority="
                            + effectivePriority
                            + " temporary="
                            + temporary
                    );
                }
            }
        }
    }

    private static CommandSource source() {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }
}
