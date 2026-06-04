/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.annotations;

import dev.riege.buildmycommand.annotation.Alias;
import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.Description;
import dev.riege.buildmycommand.annotation.Permission;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.Subcommand;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.CommandContext;
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
                @SubRoute("grant <target:String> <permission:String>")
                @Alias("g")
                @Permission("team.member.permission.grant")
                CommandResult grant(@RouteCtx CommandContext route) {
                    return Results.success("Granted " + route.arg("permission", String.class)
                        + " to " + route.arg("target", String.class));
                }

                @SubRoute("revoke <target:String> <permission:String>")
                @Alias("r")
                @Permission("team.member.permission.revoke")
                CommandResult revoke(@RouteCtx CommandContext route) {
                    return Results.success("Revoked " + route.arg("permission", String.class)
                        + " from " + route.arg("target", String.class));
                }
            }

            @Subcommand("role")
            @Description("Member role operations")
            static final class RoleCommands {
                @SubRoute("set <target:String> <role:String> [--priority:Integer] [--temporary]")
                @Permission("team.member.role.set")
                CommandResult set(@RouteCtx CommandContext route) {
                    int effectivePriority = route.option("priority", Integer.class).orElse(0);
                    return Results.success(
                        "Set "
                            + route.arg("target", String.class)
                            + " role="
                            + route.arg("role", String.class)
                            + " priority="
                            + effectivePriority
                            + " temporary="
                            + route.flag("temporary")
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
