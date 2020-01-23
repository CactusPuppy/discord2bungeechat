package com.github.cactuspuppy.d2bc.bungeechat.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import lombok.Getter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class BungeeChatAdapter {
    private static Pattern bungeeChatPattern = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\]: ([A-Z]+) > ([A-Za-z]+) > ([A-Za-z0-9_~]{3,16})\\([a-z0-9-]+\\): (.+)");

    /**
     * Path to BungeeChat logs folder
     */
    private Path logsPath;
    /**
     * Current log of interest
     */
    private Path latestLogPath;
    /**
     * Last processed line of the latest.log file
     */
    private long lastLineRead;
    /**
     * The repeating task
     */
    private ScheduledTask fileWatcher;

    private static BaseComponent[] discordPrefix = (new ComponentBuilder("[")).color(ChatColor.WHITE).bold(true)
            .append("Discord").color(ChatColor.AQUA).append("] ").color(ChatColor.WHITE).create();

    // Adapted from https://stackoverflow.com/a/40003648
    public BungeeChatAdapter(Path bungeeChatLogsPath) {
        //TODO: Load and save lastLineRead to disk
        lastLineRead = -1;
        logsPath = bungeeChatLogsPath;
        if (!Files.isDirectory(logsPath))
        // Get latest file (from
        try {
            Optional<Path> lastFilePath = Files.list(logsPath)    // here we get the stream with full directory listing
                    .filter(f -> !Files.isDirectory(f))  // exclude subdirectories from listing
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));  // finally get the last file using simple comparator by lastModified field
            if (!lastFilePath.isPresent()) {
                D2BC.getPlugin().getLogger().info("No BungeeChat logs currently present");
                latestLogPath = null;
            } else {
                latestLogPath = lastFilePath.get();
            }
        } catch (IOException e) {
            D2BC.getPlugin().getLogger().log(Level.SEVERE, "Exception while finding latest log", e);
            return;
        }
        // Schedule log check
        fileWatcher = ProxyServer.getInstance().getScheduler().schedule(D2BC.getPlugin(), processLogChanges,
                0, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks for new chat log changes
     */
    private Runnable processLogChanges = () -> {
        // Check for newer file
        LocalDateTime now = LocalDateTime.now();
        Path newest = Paths.get(ProxyServer.getInstance().getPluginsFolder().getAbsolutePath(),
                File.separator, "BungeeChat", File.separator, "logs", File.separator,
                String.format("%d-%02d-%02d-chat.log", now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
        if (!newest.equals(latestLogPath)) {
            //Finish current log file
            relayNewMessages(latestLogPath);
            lastLineRead = -1;
            latestLogPath = newest;
        }
        relayNewMessages(latestLogPath);
    };

    private void relayNewMessages(Path logFile) {
        if (logFile == null) {
            return;
        }
        try (Stream<String> lines = Files.lines(logFile).skip(lastLineRead + 1)) {
            lines.forEachOrdered(l -> {
                Matcher m = bungeeChatPattern.matcher(l);
                if (m.matches()) {
                    String type = m.group(1);
                    String server = m.group(2);
                    String user = m.group(3);
                    String message = TextComponent.toPlainText(new TextComponent(m.group(4)));
                    D2BC.getPlugin().getDiscordAdapter().relayBungeeChatMessage(type, server, user, message);
                }
                lastLineRead++;
            });
        } catch (NoSuchFileException e) {
            D2BC.getPlugin().getLogger().log(Level.WARNING, "Could not find today's BungeeChat chat log", e);
        } catch (IOException e) {
            D2BC.getPlugin().getLogger().log(Level.SEVERE, "Exception while parsing log file", e);
        }
    }

    /**
     * Takes in a MessageReceivedEvent from Discord and formats it for sending to BungeeChat
     * @param event MessageReceivedEvent to format and send
     */
    public void relayDiscordMessage(MessageReceivedEvent event) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(discordPrefix);
        builder.retain(ComponentBuilder.FormatRetention.NONE);
        // Append sender
        builder.append(String.format("%s » ",
                event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName()));
        builder.bold(true);
        // Append message
        builder.append(event.getMessage().getContentDisplay());
        // Create final message
        BaseComponent[] message = builder.create();

        ProxyServer.getInstance().getPlayers().parallelStream()
                .filter(this::filterPlayers).forEach(p -> p.sendMessage(ChatMessageType.CHAT, message));
        ProxyServer.getInstance().getConsole().sendMessage(message);
    }

    /**
     * Filter for sending Discord messages to players
     * @param player ProxiedPlayer in question
     * @return Whether said player should receive Discord messages
     */
    private boolean filterPlayers(ProxiedPlayer player) {
        //TODO: Filter out players who opt out of seeing messages
        return true;
    }
}
