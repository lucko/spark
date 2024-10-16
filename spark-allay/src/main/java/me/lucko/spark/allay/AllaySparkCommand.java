package me.lucko.spark.allay;

import me.lucko.spark.common.SparkPlatform;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.SimpleCommand;
import org.allaymc.api.command.tree.CommandTree;

/**
 * @author IWareQ
 */
public class AllaySparkCommand extends SimpleCommand {

    private final SparkPlatform platform;

    public AllaySparkCommand(SparkPlatform platform) {
        super("spark", "spark");
        this.platform = platform;
        this.permissions.add("spark");
    }

    // only for game overloads
    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("help").root()
                .key("profiler")
                .key("info").up()
                .key("open").up()
                .key("start").enums("startFlags",
                        "--timeout",
                        "--thread",
                        "--only-ticks-over",
                        "--interval",
                        "--alloc"
                ).str("value").optional().up(3)
                .key("stop").up()
                .key("cancel").root()
                .key("tps").root()
                .key("ping").enums("pingFlags", "--player").optional().str("value").root()
                .key("healthreport").enums("healthreportFlags", "--memory", "--network").optional().str("value").optional().root()
                .key("tickmonitor").enums("tickmonitorFlags",
                        "--threshold",
                        "--threshold-tick",
                        "--without-gc"
                ).optional().str("value").optional().optional().root()
                .key("gc").root()
                .key("gcmonitor").root()
                .key("heapsummary").enums("heapsummaryFlags", "--save-to-file").optional().str("value").root()
                .key("heapdump").enums("heapdumpFlags", "--compress").str("value").root()
                .key("activity").enums("activityFlags", "--page").str("value");
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        this.platform.executeCommand(new AllayCommandSender(sender), args);
        return CommandResult.success(null);
    }
}
