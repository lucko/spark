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

package me.lucko.spark.common.command.modules;

import me.lucko.spark.common.ActivityLog;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.util.List;
import java.util.function.Consumer;

public class ActivityLogModule implements CommandModule {

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("activity", "activitylog", "log")
                .executor((platform, sender, resp, arguments) -> {
                    List<ActivityLog.Activity> log = platform.getActivityLog().getLog();
                    log.removeIf(ActivityLog.Activity::shouldExpire);

                    if (log.isEmpty()) {
                        resp.replyPrefixed(TextComponent.of("There are no entries present in the log."));
                        return;
                    }

                    resp.replyPrefixed(TextComponent.of("Showing recent spark activity...", TextColor.GOLD));

                    int count = 0;
                    for (ActivityLog.Activity activity : log) {
                        count++;

                        resp.replyPrefixed(TextComponent.builder("")
                                .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                .append(TextComponent.space())
                                .append(TextComponent.of("#" + count, TextColor.WHITE))
                                .append(TextComponent.of(" - ", TextColor.DARK_GRAY))
                                .append(TextComponent.of(activity.getType(), TextColor.YELLOW))
                                .append(TextComponent.of(" - ", TextColor.DARK_GRAY))
                                .append(TextComponent.of(formatDateDiff(activity.getTime()), TextColor.GRAY))
                                .build()
                        );
                        resp.replyPrefixed(TextComponent.builder("  ")
                                .append(TextComponent.of("Created by: ", TextColor.GRAY))
                                .append(TextComponent.of(activity.getUser().getName(), TextColor.WHITE))
                                .build()
                        );

                        TextComponent.Builder valueComponent = TextComponent.builder(activity.getDataValue(), TextColor.WHITE);
                        if (activity.getDataType().equals("url")) {
                            valueComponent.clickEvent(ClickEvent.openUrl(activity.getDataValue()));
                        }

                        resp.replyPrefixed(TextComponent.builder("  ")
                                .append(TextComponent.of(Character.toUpperCase(activity.getDataType().charAt(0)) + activity.getDataType().substring(1) + ": ", TextColor.GRAY))
                                .append(valueComponent)
                                .build()
                        );
                        resp.reply(TextComponent.space());
                    }
                })
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );
    }

    private static String formatDateDiff(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;

        if (seconds <= 0) {
            return "now";
        }

        long minute = seconds / 60;
        seconds = seconds % 60;
        long hour = minute / 60;
        minute = minute % 60;
        long day = hour / 24;
        hour = hour % 24;

        StringBuilder sb = new StringBuilder();
        if (day != 0) {
            sb.append(day).append("d ");
        }
        if (hour != 0) {
            sb.append(hour).append("h ");
        }
        if (minute != 0) {
            sb.append(minute).append("m ");
        }
        if (seconds != 0) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim() + " ago";
    }

}
