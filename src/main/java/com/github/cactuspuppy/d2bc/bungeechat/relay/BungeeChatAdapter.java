package com.github.cactuspuppy.d2bc.bungeechat.relay;

import com.github.cactuspuppy.d2bc.D2BC;
import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BungeeChatAdapter {
    private static final Pattern bungeeChatPattern = Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\]: ([A-Z]+) > ([A-Za-z]+) > ([A-Za-z0-9_~]{3,16})\\([a-z0-9-]+\\): (.+)");
    private static final Map<String, String> unicodeToShortcutMap = new HashMap<>();
    static {
        // From https://www.reddit.com/r/discordapp/comments/bt4anb/all_of_the_discord_emoji_shortcuts_that_ive_found/
        unicodeToShortcutMap.put("\uD83D\uDE42", ":)");
        unicodeToShortcutMap.put("\uD83D\uDE41", ":(");
        unicodeToShortcutMap.put("\uD83D\uDE04", ":D");
        unicodeToShortcutMap.put("\uD83D\uDE2E", ":O");
        unicodeToShortcutMap.put("\uD83D\uDE10", ":|");
        unicodeToShortcutMap.put("\uD83D\uDE2D", ";(");
        unicodeToShortcutMap.put("\uD83D\uDE22", ":'(");
        unicodeToShortcutMap.put("\uD83D\uDE20", ">:(");
        unicodeToShortcutMap.put("\uD83D\uDE1B", ":P");
        unicodeToShortcutMap.put("\uD83D\uDE05", ",:)");
        unicodeToShortcutMap.put("\uD83D\uDE12", ":$");
        unicodeToShortcutMap.put("\uD83D\uDE21", ":@");
        unicodeToShortcutMap.put("\u2764\uFE0F", "<3");
        unicodeToShortcutMap.put("\ud83d\ude09", ";)");
        unicodeToShortcutMap.put("\ud83d\ude02", ":')");
        unicodeToShortcutMap.put("\ud83d\ude13", ",:(");
    }

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

    private static String lastReadFilePath = "lastRead.dat";

    // Adapted from https://stackoverflow.com/a/40003648
    public BungeeChatAdapter(Path bungeeChatLogsPath) {
        File lastLineReadDisk = new File(D2BC.getPlugin().getDataFolder(), lastReadFilePath);
        if (Files.isReadable(lastLineReadDisk.toPath())) {
            try (Scanner scanner = new Scanner(lastLineReadDisk)) {
                lastLineRead = scanner.nextLong();
                D2BC.getPlugin().getLogger().info("Restored last line read: " + lastLineRead);
            } catch (FileNotFoundException e) {
                D2BC.getPlugin().getLogger().log(Level.SEVERE, "Faulty file check", e);
            } catch (Exception e) {
                D2BC.getPlugin().getLogger().log(Level.WARNING, "Exception while loading log read state," +
                        " defaulting to reading all of newest log file.", e);
                lastLineRead = -1;
            }
        } else {
            lastLineRead = -1;
        }
        logsPath = bungeeChatLogsPath;
        if (!Files.isDirectory(logsPath)) {
            D2BC.getPlugin().getLogger().warning("BungeeChat logs folder does not exist! " +
                    "Please ensure that you have the Logging module enabled in BungeeChat");
            latestLogPath = null;
        } else {
            // Get latest file (from https://stackoverflow.com/a/30892976)
            try {
                Optional<Path> lastFilePath = Files.list(logsPath)    // here we get the stream with full directory listing
                        .filter(f -> !Files.isDirectory(f))  // exclude subdirectories from listing
                        .max(Comparator.comparingLong(f -> f.toFile().lastModified()));  // finally get the last file using simple comparator by lastModified field
                if (!lastFilePath.isPresent()) {
                    D2BC.getPlugin().getLogger().info("No BungeeChat logs currently present");
                    latestLogPath = null;
                } else {
                    D2BC.getPlugin().getLogger().info("Loaded latest log file: " + lastFilePath.get().toString());
                    latestLogPath = lastFilePath.get();
                }
            } catch (IOException e) {
                D2BC.getPlugin().getLogger().log(Level.SEVERE, "Exception while finding latest log", e);
                return;
            }
        }
        // Schedule log check
        fileWatcher = ProxyServer.getInstance().getScheduler().schedule(D2BC.getPlugin(), processLogChanges,
                0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks for new chat log changes
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final Runnable processLogChanges = () -> {
        long currentLatestLineRead = lastLineRead;
        // Check for newer file
        LocalDateTime now = LocalDateTime.now();
        Path todayLog = Paths.get(ProxyServer.getInstance().getPluginsFolder().getAbsolutePath(),
                File.separator, "BungeeChat", File.separator, "logs", File.separator,
                String.format("%d-%02d-%02d-chat.log", now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
        if (!Files.isRegularFile(todayLog)) {
            D2BC.getPlugin().getLogger().fine("No chat file for today yet...");
            return;
        } else if (!todayLog.toAbsolutePath().equals(latestLogPath.toAbsolutePath())) {
            D2BC.getPlugin().getLogger().info("Rolling logs to new day...");
            D2BC.getPlugin().getLogger().info("Old log: " + latestLogPath.toAbsolutePath().toString());
            D2BC.getPlugin().getLogger().info("New log: " + todayLog.toAbsolutePath().toString());
            // Finish current log file
            relayNewMessages(latestLogPath);
            lastLineRead = -1;
            latestLogPath = todayLog;
        }
        relayNewMessages(latestLogPath);
        // If lastLineRead has changed, update on disk
        if (currentLatestLineRead != lastLineRead) {
            D2BC.getPlugin().getLogger().fine("Writing new lastLineRead to disk: " + lastLineRead);
            File lastRead = new File(D2BC.getPlugin().getDataFolder(), lastReadFilePath);
            try (FileWriter writer = new FileWriter(lastRead, false)) {
                writer.write(String.valueOf(lastLineRead));
            } catch (IOException e) {
                D2BC.getPlugin().getLogger().log(Level.SEVERE, "Exception while writing out log read state", e);
            }
        }
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
            D2BC.getPlugin().getLogger().fine("Could not find today's BungeeChat chat log");
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
        // Process and append message
        String original = ChatColor.stripColor(event.getMessage().getContentStripped());
        // Convert Discord shortcut emojis to ASCII emoticon form
        String str = undoDiscordEmojiShortcuts(original);
        // Convert remaining emoji to short-form
        str = EmojiUtils.shortCodify(str, true);
        // Remove all remaining non-supported Unicode
        str = str.replaceAll("[^\\u0000-\\uFFFF]", ""); // From https://stackoverflow.com/a/8519863
        D2BC.getPlugin().getLogger().fine(
                String.format("Relaying message from Channel <%d> by User [%s]: %s\nConverted message: %s",
                        event.getChannel().getIdLong(), event.getMember().getEffectiveName(),
                        unicodeToAsciiEscape(original), unicodeToAsciiEscape(str))
        );
        builder.append(str);
        builder.bold(false);
        // Create final message
        BaseComponent[] message = builder.create();

        ProxyServer.getInstance().getPlayers().parallelStream()
                .filter(this::filterPlayers).forEach(p -> p.sendMessage(ChatMessageType.CHAT, message));
        ProxyServer.getInstance().getConsole().sendMessage(message);
    }

    /**
     * Reverts certain emoji which Discord automatically shortcuts
     * @param discordMessage The message content from Discord to undo
     * @return The message content with all shortcut emoji reverted
     */
    @Contract("null -> null; !null -> !null")
    private String undoDiscordEmojiShortcuts(String discordMessage) {
        if (discordMessage == null) {
            return null;
        }
        for (Map.Entry<String, String> conversion : unicodeToShortcutMap.entrySet()) {
            discordMessage = discordMessage.replace(conversion.getKey(), conversion.getValue());
        }
        return discordMessage;
    }

    /**
     * Converts non-ASCII strings to Unicode escape sequences.
     * Code from https://stackoverflow.com/a/28176793
     * @param in Input string
     * @return Input string with all non-ASCII converted to Unicode escape sequences
     */
    private String unicodeToAsciiEscape(String in) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            if (ch <= 127) {
                out.append(ch);
            } else {
                out.append("\\u").append(String.format("%04x", (int)ch));
            }
        }
        return out.toString();
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
