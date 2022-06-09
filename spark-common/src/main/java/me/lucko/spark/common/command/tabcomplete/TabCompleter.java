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

package me.lucko.spark.common.command.tabcomplete;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for computing tab completion results
 */
public class TabCompleter {

    public static TabCompleter create() {
        return new TabCompleter();
    }

    public static List<String> completeForOpts(List<String> args, String... options) {
        List<String> opts = new ArrayList<>(Arrays.asList(options));
        opts.removeAll(args);

        return TabCompleter.create()
                .from(0, CompletionSupplier.startsWith(opts))
                .complete(args);
    }

    private final Map<Integer, CompletionSupplier> suppliers = new HashMap<>();
    private int from = Integer.MAX_VALUE;

    private TabCompleter() {

    }

    /**
     * Marks that the given completion supplier should be used to compute tab
     * completions at the given index.
     *
     * @param position the position
     * @param supplier the supplier
     * @return this
     */
    public TabCompleter at(int position, CompletionSupplier supplier) {
        Preconditions.checkState(position < this.from);
        this.suppliers.put(position, supplier);
        return this;
    }

    /**
     * Marks that the given completion supplier should be used to compute tab
     * completions at the given index and at all subsequent indexes infinitely.
     *
     * @param position the position
     * @param supplier the supplier
     * @return this
     */
    public TabCompleter from(int position, CompletionSupplier supplier) {
        Preconditions.checkState(this.from == Integer.MAX_VALUE);
        this.suppliers.put(position, supplier);
        this.from = position;
        return this;
    }

    public List<String> complete(List<String> args) {
        int lastIndex = 0;
        String partial;

        // nothing entered yet
        if (args.isEmpty() || (partial = args.get((lastIndex = args.size() - 1))).trim().isEmpty()) {
            return getCompletions(lastIndex, "");
        }

        // started typing something
        return getCompletions(lastIndex, partial);
    }

    private List<String> getCompletions(int position, String partial) {
        if (position >= this.from) {
            return this.suppliers.get(this.from).supplyCompletions(partial);
        }

        return this.suppliers.getOrDefault(position, CompletionSupplier.EMPTY).supplyCompletions(partial);
    }

}