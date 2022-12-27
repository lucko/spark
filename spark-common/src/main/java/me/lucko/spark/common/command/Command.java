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
import me.lucko.spark.common.command.sender.CommandSender;

import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class Command {

    public static Builder builder() {
        return new Builder();
    }

    private final List<String> aliases;
    private final List<ArgumentInfo> arguments;
    private final Executor executor;
    private final TabCompleter tabCompleter;
    private final boolean allowSubCommand;

    private Command(List<String> aliases, List<ArgumentInfo> arguments, Executor executor, TabCompleter tabCompleter, boolean allowSubCommand) {
        this.aliases = aliases;
        this.arguments = arguments;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
        this.allowSubCommand = allowSubCommand;
    }

    public List<String> aliases() {
        return this.aliases;
    }

    public List<ArgumentInfo> arguments() {
        return this.arguments;
    }

    public Executor executor() {
        return this.executor;
    }

    public TabCompleter tabCompleter() {
        return this.tabCompleter;
    }

    public String primaryAlias() {
        return this.aliases.get(0);
    }

    public boolean allowSubCommand() {
        return this.allowSubCommand;
    }

    public static final class Builder {
        private final ImmutableList.Builder<String> aliases = ImmutableList.builder();
        private final ImmutableList.Builder<ArgumentInfo> arguments = ImmutableList.builder();
        private Executor executor = null;
        private TabCompleter tabCompleter = null;
        private boolean allowSubCommand = false;

        Builder() {

        }

        public Builder aliases(String... aliases) {
            this.aliases.add(aliases);
            return this;
        }

        public Builder argumentUsage(String subCommandName, String argumentName, String parameterDescription) {
            this.arguments.add(new ArgumentInfo(subCommandName, argumentName, parameterDescription));
            return this;
        }

        public Builder argumentUsage(String argumentName, String parameterDescription) {
            this.arguments.add(new ArgumentInfo("", argumentName, parameterDescription));
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder tabCompleter(TabCompleter tabCompleter) {
            this.tabCompleter = Objects.requireNonNull(tabCompleter, "tabCompleter");
            return this;
        }

        public Builder allowSubCommand(boolean allowSubCommand) {
            this.allowSubCommand = allowSubCommand;
            return this;
        }

        public Command build() {
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
            return new Command(aliases, this.arguments.build(), this.executor, this.tabCompleter, this.allowSubCommand);
        }
    }

    @FunctionalInterface
    public interface Executor {
        void execute(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments);
    }

    @FunctionalInterface
    public interface TabCompleter {
        static <S> TabCompleter empty() {
            return (platform, sender, arguments) -> Collections.emptyList();
        }

        List<String> completions(SparkPlatform platform, CommandSender sender, List<String> arguments);
    }

    public static final class ArgumentInfo {
        private final String subCommandName;
        private final String argumentName;
        private final String parameterDescription;

        public ArgumentInfo(String subCommandName, String argumentName, String parameterDescription) {
            this.subCommandName = subCommandName;
            this.argumentName = argumentName;
            this.parameterDescription = parameterDescription;
        }

        public String subCommandName() {
            return this.subCommandName;
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

        public Component toComponent(String padding) {
            if (requiresParameter()) {
                 return text()
                        .content(padding)
                        .append(text("[", DARK_GRAY))
                        .append(text("--" + argumentName(), GRAY))
                        .append(space())
                        .append(text("<" + parameterDescription() + ">", DARK_GRAY))
                        .append(text("]", DARK_GRAY))
                        .build();
            } else {
                return text()
                        .content(padding)
                        .append(text("[", DARK_GRAY))
                        .append(text("--" + argumentName(), GRAY))
                        .append(text("]", DARK_GRAY))
                        .build();
            }
        }
    }

}
