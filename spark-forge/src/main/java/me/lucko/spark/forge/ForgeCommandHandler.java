package me.lucko.spark.forge;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

@SuppressWarnings("NullableProblems")
public abstract class ForgeCommandHandler extends CommandHandler<ICommandSender> implements ICommand {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("spark-forge-async-worker").build()
    );

    @SuppressWarnings("deprecation")
    protected ITextComponent colorize(String message) {
        TextComponent component = ComponentSerializers.LEGACY.deserialize(message, '&');
        return ITextComponent.Serializer.jsonToComponent(ComponentSerializers.JSON.serialize(component));
    }

    protected abstract void broadcast(ITextComponent msg);

    @Override
    protected void sendMessage(ICommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    @Override
    protected void sendMessage(String message) {
        ITextComponent msg = colorize(message);
        broadcast(msg);
    }

    @Override
    protected void sendLink(String url) {
        TextComponentString msg = new TextComponentString(url);
        Style style = msg.getStyle();
        style.setColor(TextFormatting.GRAY);
        style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        msg.setStyle(style);

        broadcast(msg);
    }

    @Override
    protected void runAsync(Runnable r) {
        worker.execute(r);
    }

    @Override
    protected ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

    // implement ICommand

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getLabel();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!checkPermission(server, sender)) {
            TextComponentString msg = new TextComponentString("You do not have permission to use this command.");
            Style style = msg.getStyle();
            style.setColor(TextFormatting.GRAY);
            msg.setStyle(style);

            sender.sendMessage(msg);
            return;
        }

        handleCommand(sender, args);
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "spark.profiler");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getLabel().compareTo(o.getName());
    }
}
