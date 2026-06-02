package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandResult;
import dev.riege.buildmycommand.api.CommandSource;

import java.util.Objects;

public interface CommandAdapter<S, I, R> {
    AdapterRuntime runtime();

    AdapterConfig config();

    AdapterRenderer<R> renderer();

    CommandSource mapSource(S nativeSource);

    CommandInput mapInput(S nativeSource, I nativeInput);

    default AdapterCapabilities capabilities() {
        return config().capabilities();
    }

    default AdapterRegistrationLabels registrationLabels() {
        return runtime().registrationLabels();
    }

    default CommandResult dispatch(S nativeSource, I nativeInput) {
        return runtime().dispatch(mapInput(nativeSource, nativeInput));
    }

    default R render(CommandResult result) {
        Objects.requireNonNull(result, "result");
        return renderer().render(result);
    }

    default R execute(S nativeSource, I nativeInput) {
        return render(dispatch(nativeSource, nativeInput));
    }
}
