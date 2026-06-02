package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.api.Results;
import dev.riege.buildmycommand.core.CommandFramework;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            java.util.List.of("ban"),
            java.util.List.of("ban", "block")
        ), adapter.registrationLabels());
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
