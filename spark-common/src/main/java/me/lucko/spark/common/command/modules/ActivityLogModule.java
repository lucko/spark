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

import me.lucko.spark.common.activitylog.ActivityLog.Activity;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.feature.pagination.Pagination;
import net.kyori.adventure.text.feature.pagination.Pagination.Renderer;
import net.kyori.adventure.text.feature.pagination.Pagination.Renderer.RowRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static me.lucko.spark.common.command.CommandResponseHandler.*;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public class ActivityLogModule implements CommandModule, RowRenderer<Activity> {

    private final Pagination.Builder pagination = Pagination.builder()
            .renderer(new Renderer() {
                @Override
                public Component renderEmpty() {
                    return applyPrefix(text("There are no entries present in the log."));
                }

                @Override
                public Component renderUnknownPage(int page, int pages) {
                    return applyPrefix(text("Unknown page selected. " + pages + " total pages."));
                }
            })
            .resultsPerPage(4);

    @Override
    public Collection<Component> renderRow(Activity activity, int index) {
        List<Component> reply = new ArrayList<>(5);
        reply.add(text()
                .append(text(">", DARK_GRAY, BOLD))
                .append(space())
                .append(text("#" + (index + 1), WHITE))
                .append(text(" - ", DARK_GRAY))
                .append(text(activity.getType(), YELLOW))
                .append(text(" - ", DARK_GRAY))
                .append(text(formatDateDiff(activity.getTime()), GRAY))
                .build()
        );
        reply.add(text()
                .content("  ")
                .append(text("Created by: ", GRAY))
                .append(text(activity.getUser().getName(), WHITE))
                .build()
        );

        TextComponent.Builder valueComponent = text().content(activity.getDataValue()).color(WHITE);
        if (activity.getDataType().equals("url")) {
            valueComponent.clickEvent(ClickEvent.openUrl(activity.getDataValue()));
        }

        reply.add(text()
                .content("  ")
                .append(text(Character.toUpperCase(activity.getDataType().charAt(0)) + activity.getDataType().substring(1) + ": ", GRAY))
                .append(valueComponent)
                .build()
        );
        reply.add(space());
        return reply;
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("activity", "activitylog", "log")
                .argumentUsage("page", "page no")
                .executor((platform, sender, resp, arguments) -> {
                    List<Activity> log = platform.getActivityLog().getLog();
                    log.removeIf(Activity::shouldExpire);

                    if (log.isEmpty()) {
                        resp.replyPrefixed(text("There are no entries present in the log."));
                        return;
                    }

                    int page = Math.max(1, arguments.intFlag("page"));

                    Pagination<Activity> activityPagination = this.pagination.build(
                            text("Recent spark activity", GOLD),
                            this,
                            value -> "/" + platform.getPlugin().getCommandName() + " activity --page " + value
                    );
                    activityPagination.render(log, page).forEach(resp::reply);
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--page"))
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
