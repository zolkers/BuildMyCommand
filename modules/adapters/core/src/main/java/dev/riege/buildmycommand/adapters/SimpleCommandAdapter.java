/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.adapters;

import dev.riege.buildmycommand.api.CommandInput;
import dev.riege.buildmycommand.api.CommandPlatform;
import dev.riege.buildmycommand.api.CommandSource;
import dev.riege.buildmycommand.core.CommandFramework;

import java.util.Objects;

public final class SimpleCommandAdapter<S, I, R> implements CommandAdapter<S, I, R> {
    private final AdapterRuntime runtime;
    private final AdapterConfig config;
    private final AdapterRenderer<R> renderer;
    private final AdapterSourceMapper<S> sourceMapper;
    private final AdapterInputMapper<S, I> inputMapper;

    private SimpleCommandAdapter(
        AdapterRuntime runtime,
        AdapterConfig config,
        AdapterRenderer<R> renderer,
        AdapterSourceMapper<S> sourceMapper,
        AdapterInputMapper<S, I> inputMapper
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.config = Objects.requireNonNull(config, "config");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
        this.inputMapper = Objects.requireNonNull(inputMapper, "inputMapper");
    }

    public static <S, I, R> Builder<S, I, R> builder(CommandFramework framework, CommandPlatform platform) {
        return new Builder<>(framework, platform);
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
    public AdapterRenderer<R> renderer() {
        return renderer;
    }

    @Override
    public CommandSource mapSource(S nativeSource) {
        return sourceMapper.map(nativeSource);
    }

    @Override
    public CommandInput mapInput(S nativeSource, I nativeInput) {
        return inputMapper.map(nativeSource, nativeInput, runtime, sourceMapper);
    }

    public static final class Builder<S, I, R> {
        private final AdapterRuntime runtime;
        private AdapterConfig config;
        private AdapterRenderer<R> renderer;
        private AdapterSourceMapper<S> sourceMapper;
        private AdapterInputMapper<S, I> inputMapper;

        private Builder(CommandFramework framework, CommandPlatform platform) {
            Objects.requireNonNull(platform, "platform");
            this.runtime = new AdapterRuntime(framework, platform);
            this.config = AdapterConfig.of(platform.id(), platform.displayName(), AdapterCapabilities.from(platform));
        }

        public Builder<S, I, R> config(AdapterConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        public Builder<S, I, R> renderer(AdapterRenderer<R> renderer) {
            this.renderer = Objects.requireNonNull(renderer, "renderer");
            return this;
        }

        public Builder<S, I, R> sourceMapper(AdapterSourceMapper<S> sourceMapper) {
            this.sourceMapper = Objects.requireNonNull(sourceMapper, "sourceMapper");
            return this;
        }

        public Builder<S, I, R> inputMapper(AdapterInputMapper<S, I> inputMapper) {
            this.inputMapper = Objects.requireNonNull(inputMapper, "inputMapper");
            return this;
        }

        public SimpleCommandAdapter<S, I, R> build() {
            if (renderer == null) {
                throw new IllegalStateException("renderer must be configured");
            }
            if (sourceMapper == null) {
                throw new IllegalStateException("sourceMapper must be configured");
            }
            if (inputMapper == null) {
                throw new IllegalStateException("inputMapper must be configured");
            }
            return new SimpleCommandAdapter<>(runtime, config, renderer, sourceMapper, inputMapper);
        }
    }
}
