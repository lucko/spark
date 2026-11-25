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

package me.lucko.spark.minecraft.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Combines {@link Command} and {@link SuggestionProvider} into a single interface.
 *
 * @param <S> the type of the command source
 */
public interface CommandWithSuggestions<S> extends Command<S>, SuggestionProvider<S> {

    static <S> CommandWithSuggestions<S> forwardingTo(Supplier<CommandWithSuggestions<S>> supplier) {
        return new Forwarding<>(supplier);
    }

    /**
     * Registers this command to the given command dispatcher with the given aliases.
     *
     * @param dispatcher the command dispatcher
     * @param aliases the command aliases
     */
    default void registerToCommandDispatcher(CommandDispatcher<S> dispatcher, String... aliases) {
        if (aliases.length == 0) {
            return;
        }

        String mainName = aliases[0];
        LiteralArgumentBuilder<S> literal = LiteralArgumentBuilder.<S>literal(mainName)
                .executes(this)
                .then(RequiredArgumentBuilder.<S, String>argument("args", StringArgumentType.greedyString())
                        .suggests(this)
                        .executes(this)
                );

        LiteralCommandNode<S> node = dispatcher.register(literal);
        for (int i = 1; i < aliases.length; i++) {
            dispatcher.register(LiteralArgumentBuilder.<S>literal(aliases[i]).redirect(node));
        }
    }

    /**
     * An implementation of {@link CommandWithSuggestions} which forwards to another command.
     *
     * @param <S> the type of the command source
     */
    final class Forwarding<S> implements CommandWithSuggestions<S> {
        private final Supplier<CommandWithSuggestions<S>> delegateSupplier;

        public Forwarding(Supplier<CommandWithSuggestions<S>> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
        }

        private CommandWithSuggestions<S> delegate() {
            return Objects.requireNonNull(this.delegateSupplier.get(), "delegate supplier returned null");
        }

        @Override
        public int run(CommandContext<S> context) throws CommandSyntaxException {
            return delegate().run(context);
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            return delegate().getSuggestions(context, builder);
        }
    }

}
