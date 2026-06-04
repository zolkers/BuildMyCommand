/*
 * Copyright (c) 2026 Zolkers
 *
 * Licensed under the MIT License.
 * SPDX-License-Identifier: MIT
 */

package dev.riege.buildmycommand.api.help;

import dev.riege.buildmycommand.api.CommandContext;

import java.util.Objects;
import java.util.Optional;

public record HelpOptions(
    int page,
    int pageSize,
    boolean alphabetic,
    Optional<String> group,
    boolean showAliases,
    String commandPrefix
) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 8;

    public HelpOptions {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        group = Objects.requireNonNull(group, "group").map(String::trim).filter(value -> !value.isBlank());
        Objects.requireNonNull(commandPrefix, "commandPrefix");
    }

    public static HelpOptions defaults() {
        return builder().build();
    }

    public static HelpOptions from(CommandContext context) {
        Objects.requireNonNull(context, "context");
        return builder()
            .page(context.option("page", Integer.class).orElse(DEFAULT_PAGE))
            .pageSize(context.option("size", Integer.class).orElse(DEFAULT_PAGE_SIZE))
            .alphabetic(context.flag("alphabetic"))
            .group(context.option("group", String.class))
            .build();
    }

    public Builder toBuilder() {
        return builder()
            .page(page)
            .pageSize(pageSize)
            .alphabetic(alphabetic)
            .group(group)
            .showAliases(showAliases)
            .commandPrefix(commandPrefix);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int page = DEFAULT_PAGE;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private boolean alphabetic;
        private Optional<String> group = Optional.empty();
        private boolean showAliases = true;
        private String commandPrefix = "/";

        private Builder() {
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder alphabetic(boolean alphabetic) {
            this.alphabetic = alphabetic;
            return this;
        }

        public Builder group(String group) {
            this.group = Optional.ofNullable(group);
            return this;
        }

        public Builder group(Optional<String> group) {
            this.group = Objects.requireNonNull(group, "group");
            return this;
        }

        public Builder showAliases(boolean showAliases) {
            this.showAliases = showAliases;
            return this;
        }

        public Builder commandPrefix(String commandPrefix) {
            this.commandPrefix = Objects.requireNonNull(commandPrefix, "commandPrefix");
            return this;
        }

        public HelpOptions build() {
            return new HelpOptions(page, pageSize, alphabetic, group, showAliases, commandPrefix);
        }
    }
}
