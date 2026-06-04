/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.api.Suggestion;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandAdapterContractTest {
    @Test
    void adaptsNativeSourceAndInputThroughFrameworkThenRendersResult() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("greet", command -> command
                .alias("hello")
                .executes(ctx -> Results.success(ctx.source().id().orElse("missing")
                    + ":"
                    + ctx.commandInput().platform().id())));
        ContractAdapter adapter = new ContractAdapter(framework);
        NativeSource nativeSource = new NativeSource("native-1", "Ada");
        NativeInput nativeInput = new NativeInput("!greet", 6);

        CommandSource source = adapter.mapSource(nativeSource);
        CommandInput input = adapter.mapInput(nativeSource, nativeInput);
        CommandResult result = adapter.dispatch(nativeSource, nativeInput);
        RenderedResult rendered = adapter.render(result);

        assertEquals(Optional.of("native-1"), source.id());
        assertEquals(Optional.of("Ada"), source.name());
        assertEquals("!greet", input.rawInput());
        assertEquals("greet", input.normalizedInput());
        assertEquals(5, input.normalizedCursor());
        assertEquals("contract", input.platform().id());
        assertEquals(CommandResult.Status.SUCCESS, result.status());
        assertEquals(new RenderedResult(1, Optional.of("native-1:contract")), rendered);
    }

    @Test
    void exposesValidatedCapabilitiesAndRegistrationLabels() {
        CommandFramework framework = CommandFramework.create();
        framework.registry()
            .command("ban", command -> command
                .alias("block")
                .executes(ctx -> Results.silent()));
        ContractAdapter adapter = new ContractAdapter(framework);

        assertEquals(new AdapterCapabilities(true, true, false), adapter.capabilities());
        assertEquals("contract", adapter.config().adapterId());
        assertEquals("Contract Test", adapter.config().displayName());
        assertEquals("contract", adapter.runtime().platform().id());
        assertEquals(new AdapterRegistrationLabels(
            List.of("ban"),
            List.of("ban", "block")
        ), adapter.registrationLabels());
        assertEquals(AdapterMatchingPolicy.strict(), adapter.matchingPolicy());
    }

    @Test
    void adapterInterfaceForcesSuggestionAndMatchingContracts() {
        CommandFramework framework = CommandFramework.builder()
            .caseInsensitiveLiterals()
            .caseInsensitiveOptions()
            .build();
        framework.registry()
            .command("ping", command -> command
                .alias("p")
                .executes(ctx -> Results.success("pong")));
        framework.registry()
            .route("bang|b <target:String>")
            .suggestAliases(false)
            .executes(ctx -> Results.success(ctx.arg("target", String.class)));
        IAdapter<NativeSource, NativeInput, RenderedResult> adapter = new ContractAdapter(framework);

        List<Suggestion> richSuggestions = adapter.suggestRich(
            new NativeSource("1", "Ada"),
            new NativeInput("!P", 2),
            2
        );

        assertEquals(List.of("ping", "p"), adapter.suggest(new NativeSource("1", "Ada"), new NativeInput("!P", 2), 2));
        assertEquals(List.of("ping", "p"), richSuggestions.stream().map(Suggestion::value).toList());
        assertEquals(List.of("bang"), adapter.suggest(new NativeSource("1", "Ada"), new NativeInput("!b", 2), 2));
        assertEquals(Optional.of("Ada"), adapter.dispatch(
            new NativeSource("1", "Ada"),
            new NativeInput("!b Ada", 6)
        ).reply());
        assertEquals(new AdapterMatchingPolicy(true, true, true), adapter.matchingPolicy());
    }

    @Test
    void simpleAdapterBuilderLetsUsersCreateCustomAdaptersWithLambdas() {
        CommandFramework framework = CommandFramework.create();
        framework.registry().command("echo", command -> command.executes(ctx -> Results.success(
            ctx.source().name().orElseThrow() + ":" + ctx.commandInput().normalizedInput()
        )));
        CommandPlatform platform = new CommandPlatform("chat", "Chat", true, true, true);

        SimpleCommandAdapter<NativeSource, NativeInput, RenderedResult> adapter =
            SimpleCommandAdapter.<NativeSource, NativeInput, RenderedResult>builder(framework, platform)
                .config(AdapterConfig.of("custom-chat", "Custom Chat", AdapterCapabilities.from(platform)))
                .sourceMapper(source -> new CommandSource() {
                    @Override
                    public Optional<String> name() {
                        return Optional.of(source.name());
                    }
                })
                .inputMapper((source, input, runtime, mapper) -> new CommandInput(
                    mapper.map(source),
                    input.rawInput(),
                    input.rawInput().substring(1),
                    input.cursor(),
                    "!",
                    runtime.platform()
                ))
                .renderer(result -> new RenderedResult(
                    result.status() == CommandResult.Status.SUCCESS ? 200 : 500,
                    result.reply()
                ))
                .build();

        RenderedResult rendered = adapter.execute(new NativeSource("1", "Ada"), new NativeInput("!echo", 5));

        assertEquals(new RenderedResult(200, Optional.of("Ada:echo")), rendered);
        assertEquals("custom-chat", adapter.config().adapterId());
        assertEquals(Optional.of("Ada"), adapter.mapSource(new NativeSource("1", "Ada")).name());
        assertEquals("echo", adapter.mapInput(new NativeSource("1", "Ada"), new NativeInput("!echo", 5))
            .normalizedInput());
    }

    @Test
    void simpleAdapterBuilderFailsFastWhenRequiredPiecesAreMissing() {
        CommandFramework framework = CommandFramework.create();
        CommandPlatform platform = new CommandPlatform("chat", "Chat", true, true, true);

        IllegalStateException missingRenderer = assertThrows(IllegalStateException.class,
            () -> SimpleCommandAdapter.<NativeSource, NativeInput, RenderedResult>builder(framework, platform).build());
        IllegalStateException missingSourceMapper = assertThrows(IllegalStateException.class,
            () -> SimpleCommandAdapter.<NativeSource, NativeInput, RenderedResult>builder(framework, platform)
                .renderer(result -> new RenderedResult(0, result.reply()))
                .build());
        IllegalStateException missingInputMapper = assertThrows(IllegalStateException.class,
            () -> SimpleCommandAdapter.<NativeSource, NativeInput, RenderedResult>builder(framework, platform)
                .renderer(result -> new RenderedResult(0, result.reply()))
                .sourceMapper(source -> new CommandSource() {
                })
                .build());

        assertEquals("renderer must be configured", missingRenderer.getMessage());
        assertEquals("sourceMapper must be configured", missingSourceMapper.getMessage());
        assertEquals("inputMapper must be configured", missingInputMapper.getMessage());
        assertThrows(NullPointerException.class, () -> SimpleCommandAdapter.builder(null, platform));
        assertThrows(NullPointerException.class, () -> SimpleCommandAdapter.builder(framework, null));
        assertThrows(NullPointerException.class, () ->
            SimpleCommandAdapter.builder(framework, platform).config(null));
        assertThrows(NullPointerException.class, () ->
            SimpleCommandAdapter.builder(framework, platform).renderer(null));
        assertThrows(NullPointerException.class, () ->
            SimpleCommandAdapter.builder(framework, platform).sourceMapper(null));
        assertThrows(NullPointerException.class, () ->
            SimpleCommandAdapter.builder(framework, platform).inputMapper(null));
    }

    @Test
    void adapterValueObjectsValidateNullsAndBlankText() {
        CommandPlatform platform = new CommandPlatform("chat", "Chat", true, false, true);
        AdapterConfig config = AdapterConfig.of("chat", "Chat", AdapterCapabilities.from(platform));
        CommandResult result = Results.success("ok");

        assertEquals(new AdapterCapabilities(true, false, true), AdapterCapabilities.from(platform));
        assertEquals("chat", config.adapterId());
        assertSame(result, AdapterRenderer.identity().render(result));
        assertThrows(NullPointerException.class, () -> AdapterCapabilities.from(null));
        assertThrows(NullPointerException.class, () -> AdapterConfig.of(null, "Chat",
            AdapterCapabilities.from(platform)));
        assertThrows(NullPointerException.class, () -> AdapterConfig.of("chat", null,
            AdapterCapabilities.from(platform)));
        assertThrows(NullPointerException.class, () -> AdapterConfig.of("chat", "Chat", null));
        assertThrows(IllegalArgumentException.class, () -> AdapterConfig.of(" ", "Chat",
            AdapterCapabilities.from(platform)));
        assertThrows(IllegalArgumentException.class, () -> AdapterConfig.of("chat", "",
            AdapterCapabilities.from(platform)));
        assertThrows(NullPointerException.class, () -> AdapterRenderer.identity().render(null));
    }

    private static final class ContractAdapter implements CommandAdapter<NativeSource, NativeInput, RenderedResult> {
        private final AdapterRuntime runtime;
        private final AdapterConfig config;
        private final AdapterRenderer<RenderedResult> renderer = result -> new RenderedResult(
            result.status() == CommandResult.Status.SUCCESS ? 1 : 0,
            result.reply()
        );

        private ContractAdapter(CommandFramework framework) {
            CommandPlatform platform = new CommandPlatform("contract", "Contract", true, true, false);
            this.runtime = new AdapterRuntime(framework, platform);
            this.config = new AdapterConfig(
                "contract",
                "Contract Test",
                AdapterCapabilities.from(platform)
            );
        }

        @Override
        public AdapterRuntime runtime() {
            return runtime;
        }

        @Override
        public AdapterConfig config() {
            return config;
        }

        @Override
        public AdapterRenderer<RenderedResult> renderer() {
            return renderer;
        }

        @Override
        public CommandSource mapSource(NativeSource source) {
            return new CommandSource() {
                @Override
                public Optional<String> id() {
                    return Optional.of(source.id());
                }

                @Override
                public Optional<String> name() {
                    return Optional.of(source.name());
                }
            };
        }

        @Override
        public CommandInput mapInput(NativeSource source, NativeInput input) {
            return new CommandInput(
                mapSource(source),
                input.rawInput(),
                input.rawInput().substring(1),
                input.cursor(),
                "!",
                runtime.platform()
            );
        }
    }

    private record NativeSource(String id, String name) {
    }

    private record NativeInput(String rawInput, int cursor) {
    }

    private record RenderedResult(int nativeCode, Optional<String> message) {
    }
}
