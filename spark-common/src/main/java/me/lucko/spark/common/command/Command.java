/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.command;

import com.google.common.collect.ImmutableList;

import me.lucko.spark.common.SparkPlatform;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Command<S> {

    public static <S> Builder<S> builder() {
        return new Builder<>();
    }

    private final List<String> aliases;
    private final List<ArgumentInfo> arguments;
    private final Executor<S> executor;
    private final TabCompleter<S> tabCompleter;

    private Command(List<String> aliases, List<ArgumentInfo> arguments, Executor<S> executor, TabCompleter<S> tabCompleter) {
        this.aliases = aliases;
        this.arguments = arguments;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
    }

    public List<String> aliases() {
        return this.aliases;
    }

    public List<ArgumentInfo> arguments() {
        return this.arguments;
    }

    public Executor<S> executor() {
        return this.executor;
    }

    public TabCompleter<S> tabCompleter() {
        return this.tabCompleter;
    }

    public static final class Builder<S> {
        private final ImmutableList.Builder<String> aliases = ImmutableList.builder();
        private final ImmutableList.Builder<ArgumentInfo> arguments = ImmutableList.builder();
        private Executor<S> executor = null;
        private TabCompleter<S> tabCompleter = null;

        Builder() {

        }

        public Builder<S> aliases(String... aliases) {
            this.aliases.add(aliases);
            return this;
        }

        public Builder<S> argumentUsage(String argumentName, String parameterDescription) {
            this.arguments.add(new ArgumentInfo(argumentName, parameterDescription));
            return this;
        }

        public Builder<S> executor(Executor<S> executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder<S> tabCompleter(TabCompleter<S> tabCompleter) {
            this.tabCompleter = Objects.requireNonNull(tabCompleter, "tabCompleter");
            return this;
        }

        public Command<S> build() {
            List<String> aliases = this.aliases.build();
            if (aliases.isEmpty()) {
                throw new IllegalStateException("No aliases defined");
            }
            if (this.executor == null) {
                throw new IllegalStateException("No defined executor");
            }
            if (this.tabCompleter == null) {
                this.tabCompleter = TabCompleter.empty();
            }
            return new Command<>(aliases, this.arguments.build(), this.executor, this.tabCompleter);
        }
    }

    @FunctionalInterface
    public interface Executor<S> {
        void execute(SparkPlatform<S> platform, S sender, Arguments arguments);
    }

    @FunctionalInterface
    public interface TabCompleter<S> {
        static <S> TabCompleter<S> empty() {
            return (platform, sender, arguments) -> Collections.emptyList();
        }

        List<String> completions(SparkPlatform<S> platform, S sender, List<String> arguments);
    }

    public static final class ArgumentInfo {
        private final String argumentName;
        private final String parameterDescription;

        public ArgumentInfo(String argumentName, String parameterDescription) {
            this.argumentName = argumentName;
            this.parameterDescription = parameterDescription;
        }

        public String argumentName() {
            return this.argumentName;
        }

        public String parameterDescription() {
            return this.parameterDescription;
        }

        public boolean requiresParameter() {
            return this.parameterDescription != null;
        }
    }

}
