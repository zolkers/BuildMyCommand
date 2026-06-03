package dev.riege.buildmycommand.adapters.minecraft.paper;

import java.util.Objects;

public final class PaperCommandRegistrationStrategy {
    private static final String HYBRID_MESSAGE =
        "Paper HYBRID registration is reserved for a future native Brigadier + command-map bridge.";

    private final PaperCommandRegistrationMode mode;

    private PaperCommandRegistrationStrategy(PaperCommandRegistrationMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public static PaperCommandRegistrationStrategy of(PaperCommandRegistrationMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (mode == PaperCommandRegistrationMode.HYBRID) {
            throw new UnsupportedOperationException(HYBRID_MESSAGE);
        }
        return new PaperCommandRegistrationStrategy(mode);
    }

    public PaperCommandRegistrationMode mode() {
        return mode;
    }

    public boolean usesBrigadierProjection() {
        return mode == PaperCommandRegistrationMode.BRIGADIER_PROJECTION;
    }

    public boolean usesNativeCommandMapFallback() {
        return mode == PaperCommandRegistrationMode.NATIVE_COMMAND;
    }
}
