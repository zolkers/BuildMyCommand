/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.examples.types;

import dev.riege.buildmycommand.annotation.AnnotationCommandScanner;
import dev.riege.buildmycommand.annotation.Command;
import dev.riege.buildmycommand.annotation.RouteCtx;
import dev.riege.buildmycommand.annotation.SubRoute;
import dev.riege.buildmycommand.api.ArgumentParseContext;
import dev.riege.buildmycommand.api.ArgumentParseResult;
import dev.riege.buildmycommand.api.ArgumentParser;
import dev.riege.buildmycommand.api.CommandContext;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.api.SuggestionType;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CustomCommandTypeExample {
    private CustomCommandTypeExample() {
    }

    public static CommandFramework create() {
        CommandFramework framework = CommandFramework.builder()
            .type("Material", Material.class, new MaterialParser(List.of("diamond", "emerald", "stone")))
            .build();
        AnnotationCommandScanner.register(framework.registry(), new ShopCommands());
        return framework;
    }

    public static CommandResult dispatch(String input) {
        return create().dispatch(source(), input);
    }

    public static List<String> suggest(String input, int cursor) {
        return create().suggest(source(), input, cursor);
    }

    private static CommandSource source() {
        return new CommandSource() {
        };
    }

    public record Material(String id) {
        public Material {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
        }
    }

    public static final class MaterialParser implements ArgumentParser<Material> {
        private final List<String> materials;

        public MaterialParser(List<String> materials) {
            this.materials = List.copyOf(materials);
        }

        @Override
        public ArgumentParseResult<Material> parse(String rawToken, ArgumentParseContext context) {
            String normalized = rawToken.toLowerCase(Locale.ROOT);
            if (!materials.contains(normalized)) {
                return ArgumentParseResult.failure("Unknown material: " + rawToken);
            }
            return ArgumentParseResult.success(new Material(normalized));
        }

        @Override
        public List<Suggestion> suggestions(ArgumentParseContext context) {
            return materials.stream()
                .filter(material -> material.startsWith(context.rawToken().toLowerCase(Locale.ROOT)))
                .map(material -> new Suggestion(
                    material,
                    Optional.of("material"),
                    context.replacementStart(),
                    context.replacementEnd(),
                    SuggestionType.ARGUMENT,
                    0
                ))
                .toList();
        }
    }

    @Command("shop")
    static final class ShopCommands {
        @SubRoute("give <item:Material>")
        CommandResult give(@RouteCtx CommandContext ctx) {
            Material item = ctx.arg("item", Material.class);
            return Results.success("giving " + item.id());
        }
    }
}
