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

package me.lucko.spark.standalone.remote;

import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.standalone.StandaloneCommandSender;
import me.lucko.spark.standalone.StandaloneSparkPlugin;
import net.kyori.adventure.text.Component;
import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.AbstractTerminal;

public abstract class AbstractRemoteInterface implements RemoteInterface {

    protected final StandaloneSparkPlugin spark;

    public AbstractRemoteInterface(StandaloneSparkPlugin spark) {
        this.spark = spark;
    }

    private static String stripSlashSpark(String command) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.startsWith("spark ")) {
            command = command.substring(6);
        }
        return command;
    }

    public void processSession(Terminal terminal, Runnable closer) {
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer((lineReader, parsedLine, list) -> {
                    String command = stripSlashSpark(parsedLine.line());
                    String[] args = command.split(" ", -1);
                    for (String suggestion : this.spark.suggest(args, StandaloneCommandSender.NO_OP)) {
                        list.add(new Candidate(suggestion));
                    }
                })
                .build();

        StandaloneCommandSender sender = new StandaloneCommandSender(reader::printAbove);

        this.spark.addSender(sender);
        ((AbstractTerminal) terminal).setOnClose(() -> this.spark.removeSender(sender));

        CommandResponseHandler resp = this.spark.createResponseHandler(sender);
        resp.replyPrefixed(Component.text("spark remote interface - " + this.spark.getVersion()));
        resp.replyPrefixed(Component.text("Use '/spark' commands as usual, or run 'exit' to exit."));

        while (true) {
            try {
                String line = reader.readLine("> ");
                if (line.trim().isEmpty()) {
                    continue;
                }

                String command = stripSlashSpark(line);
                if (command.equals("exit")) {
                    closer.run();
                    return;
                }

                this.spark.execute(command.split(" ", 0), sender);

            } catch (UserInterruptException e) {
                // ignore
            } catch (EndOfFileException e) {
                this.spark.removeSender(sender);
                return;
            }
        }
    }

}
