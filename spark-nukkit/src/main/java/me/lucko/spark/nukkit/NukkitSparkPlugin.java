package me.lucko.spark.nukkit;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.AsyncTask;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;

import java.nio.file.Path;
import java.util.stream.Stream;

public class NukkitSparkPlugin extends PluginBase implements SparkPlugin {
    private SparkPlatform platform;

    @Override
    public void onEnable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    @Override
    public void onDisable() {
        this.platform.disable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.platform.executeCommand(new NukkitCommandSender(sender), args);
        return true;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<NukkitCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                getServer().getOnlinePlayers().values().stream().filter(player -> player.hasPermission(permission)),
                Stream.of(getServer().getConsoleSender())
        ).map(NukkitCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        getServer().getScheduler().scheduleAsyncTask(this, new AsyncTask() {
            @Override
            public void onRun() {
                task.run();
            }
        });
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new NukkitPlatformInfo(getServer());
    }
}
