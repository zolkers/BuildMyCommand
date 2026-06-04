/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.adapters;

import com.mojang.brigadier.CommandDispatcher;
import dev.riege.buildmycommand.adapters.brigadier.BrigadierCommandAdapter;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Set;

public final class BrigadierAdapterExample {
    private BrigadierAdapterExample() {
    }

    public static CommandDispatcher<NativeSource> dispatcher() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        framework.registry()
            .route("admin|adm ban <target:String> [--silent|-s]")
            .permission("admin.ban")
            .executes(ctx -> Results.success("Banned " + ctx.arg("target", String.class)));
        BrigadierCommandAdapter<NativeSource> adapter = BrigadierCommandAdapter.create(framework, NativeSource::source);
        CommandDispatcher<NativeSource> dispatcher = new CommandDispatcher<>();
        adapter.registration().register(dispatcher);
        return dispatcher;
    }

    public record NativeSource(Set<String> permissions) {
        CommandSource source() {
            return new CommandSource() {
                @Override
                public boolean hasPermission(String permission) {
                    return permissions.contains(permission);
                }
            };
        }
    }
}
